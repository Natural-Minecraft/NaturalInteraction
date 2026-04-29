import { useState, useCallback, useMemo, useEffect, useRef } from 'react';
import {
  ReactFlow, Background, Controls, MiniMap,
  useNodesState, useEdgesState, addEdge,
  Panel,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { apiFetch } from '../api';
import PropertyPanel from '../components/PropertyPanel';
import StoryNode from '../components/StoryNode';
import { getNodeType, TYPE_STYLES, parseJumpTarget, buildFlowData } from '../utils/flowUtils';

const nodeTypes = { dialogueNode: StoryNode };

export default function Editor({ data: interactionData, onBack }) {
  const [interaction, setInteraction] = useState(() => JSON.parse(JSON.stringify(interactionData)));
  const [dirty, setDirty] = useState(false);
  const [selectedId, setSelectedId] = useState(null);
  const [toast, setToast] = useState(null);
  const [search, setSearch] = useState('');
  const [showSearch, setShowSearch] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [history, setHistory] = useState([]);
  const [historyIdx, setHistoryIdx] = useState(-1);
  const searchRef = useRef(null);

  const { flowNodes: initNodes, flowEdges: initEdges } = useMemo(() => buildFlowData(interaction), []);
  const [nodes, setNodes, onNodesChange] = useNodesState(initNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initEdges);

  function rebuildFlow(data) {
    const d = data || interaction;
    const { flowNodes, flowEdges } = buildFlowData(d);
    setNodes(flowNodes);
    setEdges(flowEdges);
  }

  // ── History (Undo/Redo) ──────────────────────────
  function pushHistory(state) {
    const snap = JSON.stringify(state);
    setHistory(prev => [...prev.slice(0, historyIdx + 1), snap].slice(-30));
    setHistoryIdx(prev => Math.min(prev + 1, 29));
  }

  function undo() {
    if (historyIdx <= 0) return;
    const newIdx = historyIdx - 1;
    const state = JSON.parse(history[newIdx]);
    setInteraction(state);
    setHistoryIdx(newIdx);
    setDirty(true);
    rebuildFlow(state);
    showToastMsg('↩ Undo', 'success');
  }

  function redo() {
    if (historyIdx >= history.length - 1) return;
    const newIdx = historyIdx + 1;
    const state = JSON.parse(history[newIdx]);
    setInteraction(state);
    setHistoryIdx(newIdx);
    setDirty(true);
    rebuildFlow(state);
    showToastMsg('↪ Redo', 'success');
  }

  // ── Keyboard shortcuts ──────────────────────────
  useEffect(() => {
    function handleKey(e) {
      if (e.ctrlKey && e.key === 'z') { e.preventDefault(); undo(); }
      if (e.ctrlKey && e.key === 'y') { e.preventDefault(); redo(); }
      if (e.ctrlKey && e.key === 's') { e.preventDefault(); saveData(); }
      if (e.ctrlKey && e.key === 'f') { e.preventDefault(); setShowSearch(s => !s); }
      if (e.key === 'Delete' && selectedId && document.activeElement?.tagName !== 'INPUT' && document.activeElement?.tagName !== 'TEXTAREA') {
        deleteNode(selectedId);
      }
    }
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  });

  useEffect(() => { if (showSearch && searchRef.current) searchRef.current.focus(); }, [showSearch]);

  // ── Init history ────────────────────────────────
  useEffect(() => { pushHistory(interaction); }, []);

  // ── Modify helpers ──────────────────────────────
  function mutate(fn) {
    const copy = JSON.parse(JSON.stringify(interaction));
    fn(copy);
    pushHistory(copy);
    setInteraction(copy);
    setDirty(true);
    rebuildFlow(copy);
  }

  const onConnect = useCallback((params) => {
    setEdges(eds => addEdge({ ...params, style: { stroke: '#4facfe' }, animated: true }, eds));
    // Also update interaction data
    const src = interaction.nodes[params.source];
    if (src) {
      if (params.sourceHandle?.startsWith('opt-')) {
        const idx = parseInt(params.sourceHandle.split('-')[1]);
        if (src.options?.[idx]) {
          mutate(d => { d.nodes[params.source].options[idx].targetNodeId = params.target; });
        }
      } else {
        mutate(d => { d.nodes[params.source].nextNodeId = params.target; });
      }
    }
  }, [interaction]);

  const onNodeClick = useCallback((_, node) => setSelectedId(node.id), []);
  const onPaneClick = useCallback(() => setSelectedId(null), []);

  function updateNode(nodeId, updatedNode) {
    mutate(d => { d.nodes[nodeId] = updatedNode; });
    setSelectedId(updatedNode.id);
  }

  function deleteNode(nodeId) {
    if (!confirm(`Delete node "${nodeId}"?`)) return;
    mutate(d => {
      delete d.nodes[nodeId];
      Object.values(d.nodes).forEach(n => {
        if (n.nextNodeId === nodeId) delete n.nextNodeId;
        if (n.options) n.options = n.options.map(o => o.targetNodeId === nodeId ? { ...o, targetNodeId: '' } : o);
      });
      if (d.rootNodeId === nodeId) d.rootNodeId = '';
    });
    setSelectedId(null);
    showToastMsg('Node deleted', 'error');
  }

  function setAsRoot(nodeId) {
    mutate(d => { d.rootNodeId = nodeId; });
    showToastMsg('Root node set!', 'success');
  }

  function addNode(type) {
    const id = 'node_' + Date.now();
    const node = {
      id, text: 'New node...', options: [], actions: [],
      durationSeconds: 5, skippable: true, giveReward: false,
      commandRewards: [], delayBeforeNext: 20, delayBeforeDialogueTicks: 0,
    };
    if (type === 'CHOICE') node.options = [{ text: 'Option 1', targetNodeId: '' }, { text: 'Option 2', targetNodeId: '' }];
    if (type === 'ACTION') node.actions = [{ type: 'COMMAND', value: '' }];
    if (type === 'REWARD') { node.giveReward = true; node.commandRewards = []; }
    mutate(d => { d.nodes[id] = node; });
    setSelectedId(id);
    showToastMsg('Node added', 'success');
  }

  function duplicateNode(nodeId) {
    const src = interaction.nodes[nodeId];
    if (!src) return;
    const id = nodeId + '_copy';
    mutate(d => { d.nodes[id] = { ...JSON.parse(JSON.stringify(src)), id }; });
    setSelectedId(id);
    showToastMsg('Node duplicated', 'success');
  }

  async function saveData() {
    try {
      nodes.forEach(n => {
        if (interaction.nodes[n.id]) {
          interaction.nodes[n.id]._x = n.position.x;
          interaction.nodes[n.id]._y = n.position.y;
        }
      });
      await apiFetch(`/api/interaction/${interaction.id}`, {
        method: 'POST', body: JSON.stringify(interaction),
      });
      setDirty(false);
      showToastMsg('Saved! 💾', 'success');
    } catch (e) { showToastMsg('Save failed: ' + e.message, 'error'); }
  }

  function exportJson() {
    const blob = new Blob([JSON.stringify(interaction, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = `${interaction.id}.json`; a.click();
    URL.revokeObjectURL(url);
    showToastMsg('Exported!', 'success');
  }

  // ── Validation ──────────────────────────────────
  const validation = useMemo(() => {
    const issues = [];
    const nodeIds = Object.keys(interaction.nodes);
    if (!interaction.rootNodeId) issues.push({ type: 'error', msg: 'No root node set' });
    else if (!interaction.nodes[interaction.rootNodeId]) issues.push({ type: 'error', msg: 'Root node missing' });
    // Broken refs
    nodeIds.forEach(id => {
      const n = interaction.nodes[id];
      if (n.nextNodeId && !interaction.nodes[n.nextNodeId]) issues.push({ type: 'warn', msg: `${id}: nextNodeId "${n.nextNodeId}" not found` });
      n.options?.forEach((o, i) => {
        if (o.targetNodeId && !interaction.nodes[o.targetNodeId]) issues.push({ type: 'warn', msg: `${id}: option[${i}] target "${o.targetNodeId}" not found` });
      });
    });
    // Orphans
    const reachable = new Set();
    const q = [interaction.rootNodeId];
    while (q.length) {
      const cur = q.shift();
      if (!cur || reachable.has(cur) || !interaction.nodes[cur]) continue;
      reachable.add(cur);
      const n = interaction.nodes[cur];
      if (n.nextNodeId) q.push(n.nextNodeId);
      n.options?.forEach(o => { if (o.targetNodeId) q.push(o.targetNodeId); });
      n.actions?.forEach(a => { const t = parseJumpTarget(a); if (t) q.push(t); });
    }
    nodeIds.filter(id => !reachable.has(id)).forEach(id => issues.push({ type: 'info', msg: `${id}: unreachable from root` }));
    return issues;
  }, [interaction]);

  // ── Search ──────────────────────────────────────
  function searchNodes() {
    if (!search.trim()) return;
    const q = search.toLowerCase();
    const found = Object.values(interaction.nodes).find(n =>
      n.id.toLowerCase().includes(q) || (n.text || '').toLowerCase().includes(q)
    );
    if (found) { setSelectedId(found.id); showToastMsg(`Found: ${found.id}`, 'success'); }
    else showToastMsg('No match', 'error');
  }

  function showToastMsg(msg, type) {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 2500);
  }

  const nodeCount = Object.keys(interaction.nodes).length;
  const selectedNode = selectedId ? interaction.nodes[selectedId] : null;

  return (
    <div className="editor-layout">
      {/* Toolbar */}
      <div className="editor-toolbar">
        <button className="btn btn-secondary btn-sm" onClick={() => { if (dirty && !confirm('Unsaved changes. Leave?')) return; onBack(); }}>← Kembali</button>
        <span className="editor-title">🌿 {interaction.id}</span>
        <span className="editor-npc">{interaction.npcDisplayName || ''}</span>
        <button className="btn-icon" style={{ marginLeft: 4 }} onClick={() => setShowSettings(true)} title="Pengaturan Interaksi">⚙️</button>
        <div className="toolbar-sep" />
        <button className="btn btn-secondary btn-sm" onClick={() => addNode('DIALOGUE')}>💬</button>
        <button className="btn btn-secondary btn-sm" onClick={() => addNode('CHOICE')}>🔀</button>
        <button className="btn btn-secondary btn-sm" onClick={() => addNode('ACTION')}>⚡</button>
        <button className="btn btn-secondary btn-sm" onClick={() => addNode('REWARD')}>⭐</button>
        <div className="toolbar-sep" />
        <button className="btn btn-secondary btn-sm" onClick={undo} title="Undo (Ctrl+Z)">↩</button>
        <button className="btn btn-secondary btn-sm" onClick={redo} title="Redo (Ctrl+Y)">↪</button>
        <button className="btn btn-secondary btn-sm" onClick={() => setShowSearch(s => !s)} title="Search (Ctrl+F)">🔍</button>
        <button className="btn btn-secondary btn-sm" onClick={exportJson} title="Export JSON">📥</button>
        <div style={{ flex: 1 }} />
        <div className="editor-stats">
          <span>{nodeCount} nodes</span>
          <span>{edges.length} connections</span>
          {validation.filter(v => v.type === 'error').length > 0 && <span className="stat-error">⚠ {validation.filter(v => v.type === 'error').length}</span>}
          {validation.filter(v => v.type === 'warn').length > 0 && <span className="stat-warn">⚡ {validation.filter(v => v.type === 'warn').length}</span>}
        </div>
        <div className="toolbar-sep" />
        {dirty && <span className="dirty-indicator">● Unsaved</span>}
        <button className="btn btn-primary btn-sm" onClick={saveData}>💾 Save</button>
      </div>

      {/* Search bar */}
      {showSearch && (
        <div className="editor-search-bar">
          <input ref={searchRef} className="prop-input" placeholder="Search nodes by ID or text..." value={search} onChange={e => setSearch(e.target.value)} onKeyDown={e => e.key === 'Enter' && searchNodes()} />
          <button className="btn btn-sm btn-secondary" onClick={searchNodes}>Find</button>
          <button className="btn btn-sm btn-secondary" onClick={() => setShowSearch(false)}>✕</button>
        </div>
      )}

      {/* Canvas + Panel */}
      <div className="editor-body">
        <div className="editor-canvas">
          <ReactFlow
            nodes={nodes} edges={edges}
            onNodesChange={onNodesChange} onEdgesChange={onEdgesChange}
            onConnect={onConnect} onNodeClick={onNodeClick} onPaneClick={onPaneClick}
            nodeTypes={nodeTypes} fitView fitViewOptions={{ padding: 0.2 }}
            proOptions={{ hideAttribution: true }}
            deleteKeyCode={null}
            style={{ background: '#0e1117' }}
          >
            <Background color="#1c2333" gap={30} size={1} />
            <Controls showInteractive={false} />
            <MiniMap nodeColor={n => { const t = getNodeType(n.data?.nodeData || {}); return TYPE_STYLES[t]?.border || '#4facfe'; }}
              style={{ background: '#161b22', border: '1px solid #30363d' }} />
            {/* Validation panel */}
            {validation.length > 0 && (
              <Panel position="bottom-left">
                <div className="validation-panel">
                  <div className="validation-header">⚠ {validation.length} issue(s)</div>
                  {validation.slice(0, 5).map((v, i) => (
                    <div key={i} className={`validation-item validation-${v.type}`}>{v.msg}</div>
                  ))}
                </div>
              </Panel>
            )}
          </ReactFlow>
        </div>

        <PropertyPanel
          node={selectedNode} allNodeIds={Object.keys(interaction.nodes)}
          rootNodeId={interaction.rootNodeId}
          onUpdate={(updated) => updateNode(selectedId, updated)}
          onDelete={() => deleteNode(selectedId)}
          onSetRoot={() => setAsRoot(selectedId)}
          onDuplicate={() => duplicateNode(selectedId)}
        />
      </div>

      {toast && <div className={`toast toast-${toast.type}`}>{toast.msg}</div>}

      {/* Settings Modal */}
      {showSettings && (
        <div className="modal-overlay" onClick={() => setShowSettings(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>⚙️ Pengaturan Interaksi</h3>
              <button className="btn-icon" onClick={() => setShowSettings(false)}>✕</button>
            </div>
            <div className="modal-body prop-body">
              <div className="prop-section">
                <label className="prop-label">Nama NPC / Judul</label>
                <input className="prop-input" value={interaction.npcDisplayName || ''} onChange={e => mutate(d => { d.npcDisplayName = e.target.value; })} />
              </div>
              <div className="prop-row-2">
                <div className="prop-section">
                  <label className="prop-label">Chapter</label>
                  <input className="prop-input" placeholder="Bab 1: Awal..." value={interaction.chapter || ''} onChange={e => mutate(d => { d.chapter = e.target.value; })} />
                </div>
                <div className="prop-section">
                  <label className="prop-label">Tipe Cerita</label>
                  <select className="prop-input" value={interaction.storyType || 'MAIN'} onChange={e => mutate(d => { d.storyType = e.target.value; })}>
                    <option value="MAIN">Utama (Main Story)</option>
                    <option value="SIDE">Sampingan (Side Quest)</option>
                    <option value="FREE">Bebas (Eksplorasi)</option>
                  </select>
                </div>
              </div>
              <div className="prop-row-2">
                <div className="prop-section">
                  <label className="prop-label">Cooldown (detik)</label>
                  <input className="prop-input" type="number" value={interaction.cooldownSeconds || 0} onChange={e => mutate(d => { d.cooldownSeconds = +e.target.value; })} />
                </div>
                <div className="prop-section" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 6, paddingTop: 14 }}>
                  <label className="prop-checkbox"><input type="checkbox" checked={!!interaction.mandatory} onChange={e => mutate(d => { d.mandatory = e.target.checked; })} /><span>Wajib (Mandatory)</span></label>
                  <label className="prop-checkbox"><input type="checkbox" checked={!!interaction.oneTimeReward} onChange={e => mutate(d => { d.oneTimeReward = e.target.checked; })} /><span>Hadiah 1x saja</span></label>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
