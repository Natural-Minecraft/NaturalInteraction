import { useState, useCallback, useMemo, useEffect } from 'react';
import {
  ReactFlow, Background, Controls, MiniMap,
  useNodesState, useEdgesState, addEdge,
  Handle, Position, Panel,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { apiFetch } from '../api';
import PropertyPanel from '../components/PropertyPanel';
import { stripMC, parseMCText } from '../utils/mcColors';

// ── Node type config ──────────────────────────────
const TYPE_STYLES = {
  DIALOGUE:  { bg: '#0d1f3c', border: '#4facfe', label: 'Dialogue', icon: '💬' },
  CHOICE:    { bg: '#0d2218', border: '#3fb950', label: 'Choice',   icon: '🔀' },
  ACTION:    { bg: '#2a1a08', border: '#f7971e', label: 'Action',   icon: '⚡' },
  CONDITION: { bg: '#2a0d0c', border: '#f85149', label: 'Condition',icon: '🔴' },
  REWARD:    { bg: '#2a2000', border: '#ffd200', label: 'Reward',   icon: '⭐' },
};

function getNodeType(node) {
  if (node.giveReward || (node.commandRewards?.length > 0)) return 'REWARD';
  if (node.actions?.some(a => a.type?.startsWith('JUMP_IF'))) return 'CONDITION';
  if (node.actions?.length > 0) return 'ACTION';
  if (node.options?.length > 1) return 'CHOICE';
  return 'DIALOGUE';
}

// ── Custom Node Component ─────────────────────────
function DialogueNode({ data, selected }) {
  const type = getNodeType(data.nodeData);
  const style = TYPE_STYLES[type];
  const node = data.nodeData;
  const isRoot = data.isRoot;
  const text = stripMC(node.text || '');
  const preview = text.length > 35 ? text.substring(0, 35) + '…' : (text || '(empty)');

  return (
    <div className={`flow-node ${selected ? 'selected' : ''}`} style={{
      background: style.bg,
      borderColor: selected ? style.border : (isRoot ? '#ffd200' : style.border + '88'),
      borderWidth: selected ? 2 : 1,
      boxShadow: selected ? `0 0 20px ${style.border}44` : (isRoot ? '0 0 12px #ffd20033' : 'none'),
    }}>
      {/* Input handle */}
      <Handle type="target" position={Position.Left} className="flow-handle flow-handle-in" />

      {/* Header */}
      <div className="flow-node-header" style={{ background: style.border + '22' }}>
        <span style={{ color: style.border, fontSize: 10, fontWeight: 700, textTransform: 'uppercase' }}>
          {style.icon} {style.label}
        </span>
        {isRoot && <span className="flow-root-badge">★ ROOT</span>}
      </div>

      {/* Body */}
      <div className="flow-node-body">
        <div className="flow-node-id">{node.id}</div>
        <div className="flow-node-text">{preview}</div>

        {/* Options list */}
        {node.options?.length > 0 && (
          <div className="flow-node-options">
            {node.options.map((opt, i) => (
              <div key={i} className="flow-node-opt">
                <span>→ {(opt.text || '—').substring(0, 22)}</span>
                <Handle
                  type="source"
                  position={Position.Right}
                  id={`opt-${i}`}
                  className="flow-handle flow-handle-opt"
                  style={{ top: 'auto', position: 'relative' }}
                />
              </div>
            ))}
          </div>
        )}

        {/* Action/reward badges */}
        {node.giveReward && <div className="flow-node-badge" style={{ color: '#ffd200' }}>⭐ reward</div>}
        {!node.giveReward && node.actions?.length > 0 && (
          <div className="flow-node-badge" style={{ color: '#f7971e' }}>⚡ {node.actions.length} action(s)</div>
        )}
      </div>

      {/* Default output handle */}
      {(!node.options || node.options.length === 0) && (
        <Handle type="source" position={Position.Right} id="default" className="flow-handle flow-handle-out" />
      )}
    </div>
  );
}

const nodeTypes = { dialogueNode: DialogueNode };

// ── Parse JUMP_IF value → target node id ──────────
function parseJumpTarget(action) {
  if (action.targetNodeId) return action.targetNodeId;
  if (action.value?.includes(',')) {
    const parts = action.value.split(',');
    return parts[parts.length - 1];
  }
  return null;
}

// ── Convert interaction data → React Flow format ──
function buildFlowData(interactionData) {
  const nodesMap = interactionData.nodes || {};
  const rootId = interactionData.rootNodeId;
  const nodeIds = Object.keys(nodesMap);

  // Auto-layout (BFS tree)
  const visited = new Set();
  const colCount = {};
  const positions = {};
  const queue = [[rootId, 0]];
  const NODE_W = 260, GAP_X = 120, GAP_Y = 50, NODE_H = 120;

  while (queue.length) {
    const [id, col] = queue.shift();
    if (visited.has(id) || !nodesMap[id]) continue;
    visited.add(id);
    colCount[col] = colCount[col] || 0;
    positions[id] = { x: col * (NODE_W + GAP_X), y: colCount[col] * (NODE_H + GAP_Y) };
    colCount[col]++;

    const n = nodesMap[id];
    const children = [];
    n.options?.forEach(o => { if (o.targetNodeId) children.push(o.targetNodeId); });
    if (n.nextNodeId) children.push(n.nextNodeId);
    n.actions?.forEach(a => {
      const target = parseJumpTarget(a);
      if (target && nodesMap[target]) children.push(target);
    });
    children.forEach(cid => { if (!visited.has(cid)) queue.push([cid, col + 1]); });
  }

  // Orphans
  let orphanY = 0;
  nodeIds.forEach(id => {
    if (!visited.has(id)) {
      positions[id] = { x: -300, y: orphanY };
      orphanY += NODE_H + GAP_Y;
    }
  });

  // Build React Flow nodes
  const flowNodes = nodeIds.map(id => ({
    id,
    type: 'dialogueNode',
    position: positions[id] || { x: 0, y: 0 },
    data: { nodeData: nodesMap[id], isRoot: id === rootId },
  }));

  // Build edges
  const flowEdges = [];
  let edgeId = 0;
  nodeIds.forEach(id => {
    const n = nodesMap[id];

    // Options → individual edges
    n.options?.forEach((opt, i) => {
      if (opt.targetNodeId && nodesMap[opt.targetNodeId]) {
        flowEdges.push({
          id: `e-${edgeId++}`, source: id, target: opt.targetNodeId,
          sourceHandle: `opt-${i}`,
          style: { stroke: '#3fb950' }, animated: false,
          label: (opt.text || '').substring(0, 15),
          labelStyle: { fill: '#3fb950', fontSize: 9 },
        });
      }
    });

    // nextNodeId
    if (n.nextNodeId && nodesMap[n.nextNodeId]) {
      flowEdges.push({
        id: `e-${edgeId++}`, source: id, target: n.nextNodeId,
        sourceHandle: 'default',
        style: { stroke: '#4facfe' }, animated: true,
      });
    }

    // JUMP_IF actions
    n.actions?.forEach(a => {
      if (!a.type?.startsWith('JUMP_IF')) return;
      const target = parseJumpTarget(a);
      if (target && nodesMap[target]) {
        flowEdges.push({
          id: `e-${edgeId++}`, source: id, target,
          sourceHandle: 'default',
          style: { stroke: '#f85149', strokeDasharray: '5,5' },
          label: a.type, labelStyle: { fill: '#f85149', fontSize: 8 },
        });
      }
    });
  });

  return { flowNodes, flowEdges };
}

// ══════════════════════════════════════════════════
// EDITOR PAGE
// ══════════════════════════════════════════════════
export default function Editor({ data: interactionData, onBack }) {
  const [interaction, setInteraction] = useState(JSON.parse(JSON.stringify(interactionData)));
  const [dirty, setDirty] = useState(false);
  const [selectedId, setSelectedId] = useState(null);
  const [toast, setToast] = useState(null);

  const { flowNodes: initNodes, flowEdges: initEdges } = useMemo(
    () => buildFlowData(interaction), []
  );

  const [nodes, setNodes, onNodesChange] = useNodesState(initNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initEdges);

  // Rebuild flow when interaction data changes
  function rebuildFlow() {
    const { flowNodes, flowEdges } = buildFlowData(interaction);
    setNodes(flowNodes);
    setEdges(flowEdges);
  }

  const onConnect = useCallback((params) => {
    setEdges(eds => addEdge({ ...params, style: { stroke: '#4facfe' }, animated: true }, eds));
    setDirty(true);
  }, []);

  const onNodeClick = useCallback((_, node) => {
    setSelectedId(node.id);
  }, []);

  const onPaneClick = useCallback(() => {
    setSelectedId(null);
  }, []);

  // Update node in interaction data
  function updateNode(nodeId, updatedNode) {
    interaction.nodes[nodeId] = updatedNode;
    setInteraction({ ...interaction });
    setDirty(true);
    rebuildFlow();
    setSelectedId(updatedNode.id);
  }

  function deleteNode(nodeId) {
    if (!confirm(`Delete node "${nodeId}"?`)) return;
    delete interaction.nodes[nodeId];
    // Clean references
    Object.values(interaction.nodes).forEach(n => {
      if (n.nextNodeId === nodeId) delete n.nextNodeId;
      if (n.options) n.options = n.options.map(o => o.targetNodeId === nodeId ? { ...o, targetNodeId: '' } : o);
    });
    if (interaction.rootNodeId === nodeId) interaction.rootNodeId = '';
    setInteraction({ ...interaction });
    setSelectedId(null);
    setDirty(true);
    rebuildFlow();
    showToast('Node deleted', 'error');
  }

  function setAsRoot(nodeId) {
    interaction.rootNodeId = nodeId;
    setInteraction({ ...interaction });
    setDirty(true);
    rebuildFlow();
    showToast('Root node set!', 'success');
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
    interaction.nodes[id] = node;
    setInteraction({ ...interaction });
    setDirty(true);
    rebuildFlow();
    setSelectedId(id);
    showToast('Node added', 'success');
  }

  async function saveData() {
    try {
      // Sync node positions back
      nodes.forEach(n => {
        if (interaction.nodes[n.id]) {
          interaction.nodes[n.id]._x = n.position.x;
          interaction.nodes[n.id]._y = n.position.y;
        }
      });
      await apiFetch(`/api/interaction/${interaction.id}`, {
        method: 'POST',
        body: JSON.stringify(interaction),
      });
      setDirty(false);
      showToast('Saved! 💾', 'success');
    } catch (e) {
      showToast('Save failed: ' + e.message, 'error');
    }
  }

  function showToast(msg, type) {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 2500);
  }

  const selectedNode = selectedId ? interaction.nodes[selectedId] : null;

  return (
    <div className="editor-layout">
      {/* Toolbar */}
      <div className="editor-toolbar">
        <button className="btn btn-secondary btn-sm" onClick={() => {
          if (dirty && !confirm('Unsaved changes. Leave?')) return;
          onBack();
        }}>← Back</button>
        <span className="editor-title">🌿 {interaction.id}</span>
        <div className="toolbar-sep" />
        <button className="btn btn-secondary btn-sm" onClick={() => addNode('DIALOGUE')}>💬 Dialogue</button>
        <button className="btn btn-secondary btn-sm" onClick={() => addNode('CHOICE')}>🔀 Choice</button>
        <button className="btn btn-secondary btn-sm" onClick={() => addNode('ACTION')}>⚡ Action</button>
        <button className="btn btn-secondary btn-sm" onClick={() => addNode('REWARD')}>⭐ Reward</button>
        <div style={{ flex: 1 }} />
        {dirty && <span className="dirty-indicator">● Unsaved</span>}
        <button className="btn btn-primary btn-sm" onClick={saveData}>💾 Save</button>
      </div>

      {/* Canvas + Panel */}
      <div className="editor-body">
        <div className="editor-canvas">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeClick={onNodeClick}
            onPaneClick={onPaneClick}
            nodeTypes={nodeTypes}
            fitView
            fitViewOptions={{ padding: 0.2 }}
            proOptions={{ hideAttribution: true }}
            style={{ background: '#0e1117' }}
          >
            <Background color="#1c2333" gap={30} size={1} />
            <Controls showInteractive={false} />
            <MiniMap
              nodeColor={n => {
                const type = getNodeType(n.data?.nodeData || {});
                return TYPE_STYLES[type]?.border || '#4facfe';
              }}
              style={{ background: '#161b22', border: '1px solid #30363d' }}
            />
          </ReactFlow>
        </div>

        <PropertyPanel
          node={selectedNode}
          allNodeIds={Object.keys(interaction.nodes)}
          rootNodeId={interaction.rootNodeId}
          onUpdate={(updated) => updateNode(selectedId, updated)}
          onDelete={() => deleteNode(selectedId)}
          onSetRoot={() => setAsRoot(selectedId)}
        />
      </div>

      {/* Toast */}
      {toast && (
        <div className={`toast toast-${toast.type}`}>{toast.msg}</div>
      )}
    </div>
  );
}
