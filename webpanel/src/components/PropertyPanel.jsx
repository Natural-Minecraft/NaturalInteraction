import { useState, useRef, useCallback, useEffect } from 'react';
import { parseMCText } from '../utils/mcColors';

// ── Action categories (internal → friendly) ────────
const FLOW_ACTIONS = ['JUMP_IF_TAG','JUMP_IF_NOT_TAG','JUMP_IF_ITEM','JUMP_IF_FACT','JUMP_IF_NOT_FACT'];
const TAG_ACTIONS = ['ADD_TAG','REMOVE_TAG'];
const FACT_ACTIONS = ['SET_FACT','ADD_FACT','REMOVE_FACT'];
const NPC_ACTIONS = ['NPC_WALK','NPC_SKIN'];
const EFFECT_ACTIONS = ['SOUND','SCREENEFFECT','ZOOM','INVISIBLE','SUBTITLE','TITLE','PARTICLE','TELEPORT'];
const COMMAND_ACTIONS = ['COMMAND','ITEM','TAKE_ITEM'];

const FLOW_LABELS = {
  JUMP_IF_TAG: 'Punya tanda', JUMP_IF_NOT_TAG: 'Tidak punya tanda',
  JUMP_IF_ITEM: 'Punya item', JUMP_IF_FACT: 'Catatan = true', JUMP_IF_NOT_FACT: 'Catatan = false',
};
const TAG_LABELS = { ADD_TAG: '＋ Beri tanda', REMOVE_TAG: '－ Hapus tanda' };
const FACT_LABELS = { SET_FACT: 'Set catatan', ADD_FACT: 'Tambah catatan', REMOVE_FACT: 'Hapus catatan' };
const NPC_LABELS = { NPC_WALK: 'NPC berjalan ke', NPC_SKIN: 'Ganti skin NPC' };
const EFFECT_LABELS = {
  SOUND: '🔊 Putar suara', SCREENEFFECT: '🖥️ Efek layar', ZOOM: '🔍 Zoom kamera',
  INVISIBLE: '👻 Invisible', SUBTITLE: '📝 Subtitle', TITLE: '📢 Judul besar',
  PARTICLE: '✨ Partikel', TELEPORT: '🌀 Teleport',
};
const CMD_LABELS = { COMMAND: '⚡ Jalankan perintah', ITEM: '📦 Beri item', TAKE_ITEM: '🗑️ Ambil item' };

function getActionLabel(type) {
  return FLOW_LABELS[type] || TAG_LABELS[type] || FACT_LABELS[type] ||
    NPC_LABELS[type] || EFFECT_LABELS[type] || CMD_LABELS[type] || type;
}

function getActionCategory(type) {
  if (FLOW_ACTIONS.includes(type)) return 'flow';
  if (TAG_ACTIONS.includes(type)) return 'tag';
  if (FACT_ACTIONS.includes(type)) return 'fact';
  if (NPC_ACTIONS.includes(type)) return 'npc';
  if (EFFECT_ACTIONS.includes(type)) return 'effect';
  return 'command';
}

export default function PropertyPanel({ node, allNodeIds, rootNodeId, onUpdate, onDelete, onSetRoot, onDuplicate }) {
  const [panelWidth, setPanelWidth] = useState(320);
  const [dragging, setDragging] = useState(false);
  const panelRef = useRef(null);

  // ── Resize drag ────────────────────────────────
  const onDragStart = useCallback((e) => {
    e.preventDefault();
    setDragging(true);
  }, []);

  useEffect(() => {
    if (!dragging) return;
    const onMove = (e) => {
      const newW = window.innerWidth - e.clientX;
      setPanelWidth(Math.max(280, Math.min(650, newW)));
    };
    const onUp = () => setDragging(false);
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => { window.removeEventListener('mousemove', onMove); window.removeEventListener('mouseup', onUp); };
  }, [dragging]);

  const isWide = panelWidth > 420;

  if (!node) {
    return (
      <div className="prop-panel" ref={panelRef} style={{ width: panelWidth }}>
        <div className="prop-resize-handle" onMouseDown={onDragStart} />
        <div className="prop-placeholder">
          <div style={{ fontSize: 48, marginBottom: 16 }}>🖱️</div>
          <p style={{ fontWeight: 600, marginBottom: 4 }}>Pilih node untuk diedit</p>
          <p className="text-muted">Klik node di canvas atau drag handle antar node untuk membuat koneksi</p>
          <div className="prop-hint-keys">
            <span>Ctrl+S simpan</span><span>Ctrl+Z undo</span><span>Del hapus</span>
          </div>
        </div>
      </div>
    );
  }

  return <PropertyEditor
    key={node.id} node={node} allNodeIds={allNodeIds} rootNodeId={rootNodeId}
    onUpdate={onUpdate} onDelete={onDelete} onSetRoot={onSetRoot} onDuplicate={onDuplicate}
    panelWidth={panelWidth} isWide={isWide} onDragStart={onDragStart} panelRef={panelRef}
  />;
}

function PropertyEditor({ node, allNodeIds, rootNodeId, onUpdate, onDelete, onSetRoot, onDuplicate,
  panelWidth, isWide, onDragStart, panelRef }) {

  const [local, setLocal] = useState({ ...node });
  const [expandedSections, setExpandedSections] = useState({
    basic: true, flow: true, tags: true, facts: false, npc: false, effects: false, commands: false, rewards: false,
  });

  // Sync when node changes
  useEffect(() => { setLocal({ ...node }); }, [node.id]);

  const set = (k, v) => setLocal(prev => ({ ...prev, [k]: v }));
  const otherNodes = allNodeIds.filter(id => id !== local.id);
  const isRoot = node.id === rootNodeId;

  // ── Group actions by category ──────────────────
  const actions = local.actions || [];
  const flowActions = actions.filter(a => FLOW_ACTIONS.includes(a.type));
  const tagActions = actions.filter(a => TAG_ACTIONS.includes(a.type));
  const factActions = actions.filter(a => FACT_ACTIONS.includes(a.type));
  const npcActions = actions.filter(a => NPC_ACTIONS.includes(a.type));
  const effectActions = actions.filter(a => EFFECT_ACTIONS.includes(a.type));
  const cmdActions = actions.filter(a => COMMAND_ACTIONS.includes(a.type));

  // ── Action CRUD (preserves order in flat array) ─
  function addAction(type) {
    setLocal(prev => ({ ...prev, actions: [...(prev.actions || []), { type, value: '' }] }));
  }
  function removeAction(idx) {
    setLocal(prev => ({ ...prev, actions: prev.actions.filter((_, i) => i !== idx) }));
  }
  function updateAction(idx, key, val) {
    setLocal(prev => {
      const acts = [...prev.actions]; acts[idx] = { ...acts[idx], [key]: val };
      return { ...prev, actions: acts };
    });
  }
  function getGlobalIdx(action) {
    return (local.actions || []).indexOf(action);
  }

  // ── Options CRUD ───────────────────────────────
  function setOption(i, key, val) {
    const opts = [...(local.options || [])]; opts[i] = { ...opts[i], [key]: val };
    setLocal(prev => ({ ...prev, options: opts }));
  }
  function addOption() { setLocal(prev => ({ ...prev, options: [...(prev.options || []), { text: 'Pilihan baru', targetNodeId: '' }] })); }
  function removeOption(i) { setLocal(prev => ({ ...prev, options: prev.options.filter((_, j) => j !== i) })); }

  // ── Rewards CRUD ───────────────────────────────
  function setReward(i, val) {
    const rews = [...(local.commandRewards || [])]; rews[i] = val;
    setLocal(prev => ({ ...prev, commandRewards: rews }));
  }
  function addReward() { setLocal(prev => ({ ...prev, commandRewards: [...(prev.commandRewards || []), ''] })); }
  function removeReward(i) { setLocal(prev => ({ ...prev, commandRewards: prev.commandRewards.filter((_, j) => j !== i) })); }

  function apply() { onUpdate({ ...local }); }
  const toggle = (s) => setExpandedSections(prev => ({ ...prev, [s]: !prev[s] }));

  return (
    <div className="prop-panel" ref={panelRef} style={{ width: panelWidth }}>
      <div className="prop-resize-handle" onMouseDown={onDragStart}>
        <div className="prop-resize-dots" />
      </div>

      {/* Header */}
      <div className="prop-header">
        <span className="prop-type-badge">{node.id}</span>
        {isRoot && <span className="flow-root-badge" style={{ marginLeft: 'auto' }}>★ ROOT</span>}
      </div>

      <div className="prop-body">
        {/* ═══ BASIC ═══ */}
        <Section title="📝 Dasar" open={expandedSections.basic} onToggle={() => toggle('basic')}>
          <div className={isWide ? 'prop-row-2' : ''}>
            <div className="prop-section">
              <label className="prop-label">ID Node</label>
              <input className="prop-input" value={local.id} onChange={e => set('id', e.target.value)} />
            </div>
            {isWide && (
              <div className="prop-section">
                <label className="prop-label">Lanjut ke Node</label>
                <select className="prop-input" value={local.nextNodeId || ''} onChange={e => set('nextNodeId', e.target.value || undefined)}>
                  <option value="">— selesai —</option>
                  {otherNodes.map(id => <option key={id} value={id}>{id}</option>)}
                </select>
              </div>
            )}
          </div>

          <div className="prop-section">
            <label className="prop-label">Teks Dialog</label>
            <textarea className="prop-textarea" rows={3} value={local.text || ''} onChange={e => set('text', e.target.value)}
              placeholder="Gunakan &e untuk kuning, &b untuk cyan..." />
            <div className="mc-preview">{parseMCText(local.text || '')}</div>
          </div>

          <div className="prop-row-2">
            <div className="prop-section">
              <label className="prop-label">Durasi (detik)</label>
              <input className="prop-input" type="number" value={local.durationSeconds || 5} onChange={e => set('durationSeconds', +e.target.value)} />
            </div>
            <div className="prop-section">
              <label className="prop-label">Delay (tick)</label>
              <input className="prop-input" type="number" value={local.delayBeforeDialogueTicks || 0} onChange={e => set('delayBeforeDialogueTicks', +e.target.value)} />
            </div>
          </div>

          {!isWide && (
            <div className="prop-section">
              <label className="prop-label">Lanjut ke Node</label>
              <select className="prop-input" value={local.nextNodeId || ''} onChange={e => set('nextNodeId', e.target.value || undefined)}>
                <option value="">— selesai —</option>
                {otherNodes.map(id => <option key={id} value={id}>{id}</option>)}
              </select>
            </div>
          )}

          <div className="prop-row-2">
            <label className="prop-checkbox"><input type="checkbox" checked={local.skippable !== false} onChange={e => set('skippable', e.target.checked)} /><span>Bisa di-skip</span></label>
            <label className="prop-checkbox"><input type="checkbox" checked={!!local.giveReward} onChange={e => set('giveReward', e.target.checked)} /><span>Beri hadiah</span></label>
          </div>
        </Section>

        {/* ═══ PILIHAN / OPTIONS ═══ */}
        <Section title={`🔀 Pilihan Pemain (${(local.options||[]).length})`} open={expandedSections.flow} onToggle={() => toggle('flow')}
          onAdd={addOption} addLabel="+ Pilihan">
          {(local.options || []).map((opt, i) => (
            <div key={i} className="prop-row-item">
              <input className="prop-input" placeholder="Teks pilihan..." value={opt.text || ''} onChange={e => setOption(i, 'text', e.target.value)} />
              <select className="prop-input" value={opt.targetNodeId || ''} onChange={e => setOption(i, 'targetNodeId', e.target.value)}>
                <option value="">— tujuan —</option>
                {otherNodes.map(id => <option key={id} value={id}>{id}</option>)}
              </select>
              <button className="btn-icon" onClick={() => removeOption(i)}>✕</button>
            </div>
          ))}
          {(!local.options || local.options.length === 0) && <div className="prop-empty">Tidak ada pilihan (dialog lanjut otomatis)</div>}
        </Section>

        {/* ═══ SEBELUM — Kondisi / JUMP_IF ═══ */}
        <Section title={`🔍 Syarat / Kondisi (${flowActions.length})`} open={flowActions.length > 0} onToggle={() => {}}
          onAdd={() => addAction('JUMP_IF_TAG')} addLabel="+ Syarat"
          hint="Cek kondisi sebelum lanjut ke node lain">
          {flowActions.map((a, i) => {
            const gi = getGlobalIdx(a);
            return (
              <div key={gi} className="prop-condition-row">
                <div className="prop-condition-label">Jika pemain</div>
                <select className="prop-input" value={a.type} onChange={e => updateAction(gi, 'type', e.target.value)}>
                  {FLOW_ACTIONS.map(t => <option key={t} value={t}>{FLOW_LABELS[t]}</option>)}
                </select>
                <input className="prop-input" placeholder="nama tanda/item..." value={a.value?.split(',')[0] || ''} onChange={e => {
                  const target = a.value?.split(',')[1] || '';
                  updateAction(gi, 'value', target ? `${e.target.value},${target}` : e.target.value);
                }} />
                <div className="prop-condition-label">→ pergi ke</div>
                <select className="prop-input" value={a.targetNodeId || a.value?.split(',')[1] || ''} onChange={e => {
                  const cond = a.value?.split(',')[0] || '';
                  updateAction(gi, 'value', `${cond},${e.target.value}`);
                  updateAction(gi, 'targetNodeId', e.target.value);
                }}>
                  <option value="">— node —</option>
                  {otherNodes.map(id => <option key={id} value={id}>{id}</option>)}
                </select>
                <button className="btn-icon" onClick={() => removeAction(gi)}>✕</button>
              </div>
            );
          })}
        </Section>

        {/* ═══ SESUDAH — Tags ═══ */}
        <Section title={`🏷️ Setelah Dialog Ini (${tagActions.length})`} open={tagActions.length > 0} onToggle={() => {}}
          onAdd={() => addAction('ADD_TAG')} addLabel="+ Tanda"
          hint="Tanda yang diberikan/dihapus setelah dialog ini">
          {tagActions.map((a, i) => {
            const gi = getGlobalIdx(a);
            return (
              <div key={gi} className="prop-row-item">
                <select className="prop-input" style={{ flex: '0 0 120px' }} value={a.type} onChange={e => updateAction(gi, 'type', e.target.value)}>
                  {TAG_ACTIONS.map(t => <option key={t} value={t}>{TAG_LABELS[t]}</option>)}
                </select>
                <input className="prop-input" placeholder="nama tanda..." value={a.value || ''} onChange={e => updateAction(gi, 'value', e.target.value)} />
                <button className="btn-icon" onClick={() => removeAction(gi)}>✕</button>
              </div>
            );
          })}
        </Section>

        {/* ═══ CATATAN / FACTS ═══ */}
        <Section title={`📋 Catatan Pemain (${factActions.length})`} open={factActions.length > 0} onToggle={() => {}}
          onAdd={() => addAction('SET_FACT')} addLabel="+ Catatan"
          hint="Tandai progress pemain (misal: quest_selesai = true)">
          {factActions.map((a, i) => {
            const gi = getGlobalIdx(a);
            const parts = (a.value || '').split(',');
            const factKey = parts[0] || '';
            const factVal = parts[1] || 'true';
            return (
              <div key={gi} className="prop-fact-row">
                <span className="prop-fact-label">Set catatan</span>
                <input className="prop-input" placeholder="nama catatan..." value={factKey} onChange={e => updateAction(gi, 'value', `${e.target.value},${factVal}`)} />
                <span className="prop-fact-label">=</span>
                <select className="prop-input" style={{ flex: '0 0 80px' }} value={factVal} onChange={e => updateAction(gi, 'value', `${factKey},${e.target.value}`)}>
                  <option value="true">TRUE</option>
                  <option value="false">FALSE</option>
                </select>
                <button className="btn-icon" onClick={() => removeAction(gi)}>✕</button>
              </div>
            );
          })}
        </Section>

        {/* ═══ NPC ═══ */}
        <Section title={`🎭 Pengaturan NPC (${npcActions.length})`} open={npcActions.length > 0} onToggle={() => {}}
          onAdd={() => addAction('NPC_WALK')} addLabel="+ NPC"
          hint="Gerakan dan penampilan NPC">
          {npcActions.map((a, i) => {
            const gi = getGlobalIdx(a);
            return (
              <div key={gi} className="prop-row-item">
                <select className="prop-input" style={{ flex: '0 0 140px' }} value={a.type} onChange={e => updateAction(gi, 'type', e.target.value)}>
                  {NPC_ACTIONS.map(t => <option key={t} value={t}>{NPC_LABELS[t]}</option>)}
                </select>
                <input className="prop-input" placeholder="koordinat/skin..." value={a.value || ''} onChange={e => updateAction(gi, 'value', e.target.value)} />
                <button className="btn-icon" onClick={() => removeAction(gi)}>✕</button>
              </div>
            );
          })}
        </Section>

        {/* ═══ EFEK ═══ */}
        <Section title={`🎬 Efek & Visual (${effectActions.length})`} open={effectActions.length > 0} onToggle={() => {}}
          onAdd={() => addAction('SOUND')} addLabel="+ Efek"
          hint="Suara, partikel, efek layar, teleport">
          {effectActions.map((a, i) => {
            const gi = getGlobalIdx(a);
            return (
              <div key={gi} className="prop-row-item">
                <select className="prop-input" style={{ flex: '0 0 140px' }} value={a.type} onChange={e => updateAction(gi, 'type', e.target.value)}>
                  {EFFECT_ACTIONS.map(t => <option key={t} value={t}>{EFFECT_LABELS[t]}</option>)}
                </select>
                <input className="prop-input" placeholder="value..." value={a.value || ''} onChange={e => updateAction(gi, 'value', e.target.value)} />
                <button className="btn-icon" onClick={() => removeAction(gi)}>✕</button>
              </div>
            );
          })}
        </Section>

        {/* ═══ PERINTAH ═══ */}
        <Section title={`⚡ Perintah Server (${cmdActions.length})`} open={cmdActions.length > 0} onToggle={() => {}}
          onAdd={() => addAction('COMMAND')} addLabel="+ Perintah"
          hint="Jalankan command, beri/ambil item">
          {cmdActions.map((a, i) => {
            const gi = getGlobalIdx(a);
            return (
              <div key={gi} className="prop-row-item">
                <select className="prop-input" style={{ flex: '0 0 140px' }} value={a.type} onChange={e => updateAction(gi, 'type', e.target.value)}>
                  {COMMAND_ACTIONS.map(t => <option key={t} value={t}>{CMD_LABELS[t]}</option>)}
                </select>
                <input className="prop-input" placeholder="/command atau item..." value={a.value || ''} onChange={e => updateAction(gi, 'value', e.target.value)} />
                <button className="btn-icon" onClick={() => removeAction(gi)}>✕</button>
              </div>
            );
          })}
        </Section>

        {/* ═══ HADIAH ═══ */}
        {local.giveReward && (
          <Section title={`🎁 Hadiah (${(local.commandRewards||[]).length})`} open={true} onToggle={() => {}}
            onAdd={addReward} addLabel="+ Hadiah"
            hint="Perintah yang dijalankan sebagai hadiah">
            {(local.commandRewards || []).map((cmd, i) => (
              <div key={i} className="prop-row-item">
                <input className="prop-input" placeholder="/give @p diamond 1" value={cmd} onChange={e => setReward(i, e.target.value)} />
                <button className="btn-icon" onClick={() => removeReward(i)}>✕</button>
              </div>
            ))}
            {(!local.commandRewards || local.commandRewards.length === 0) && <div className="prop-empty">Belum ada hadiah</div>}
          </Section>
        )}
      </div>

      {/* Footer */}
      <div className="prop-footer">
        <button className="btn btn-primary" style={{ flex: 1 }} onClick={apply}>✓ Terapkan</button>
        <button className="btn btn-secondary" onClick={onDuplicate} title="Duplikat node">📋</button>
        <button className="btn btn-secondary" onClick={onSetRoot} title="Jadikan root">⭐</button>
        <button className="btn btn-danger" onClick={onDelete} title="Hapus (Del)">🗑</button>
      </div>
    </div>
  );
}

// ── Collapsible Section Component ─────────────────
function Section({ title, open, onToggle, onAdd, addLabel, hint, children }) {
  const [isOpen, setIsOpen] = useState(open);
  return (
    <div className={`prop-section-collapsible ${isOpen ? 'open' : ''}`}>
      <div className="prop-section-trigger" onClick={() => setIsOpen(!isOpen)}>
        <span className="prop-section-arrow">{isOpen ? '▾' : '▸'}</span>
        <span className="prop-section-title">{title}</span>
        {onAdd && <button className="btn-tiny" onClick={e => { e.stopPropagation(); onAdd(); if (!isOpen) setIsOpen(true); }}>{addLabel || '+'}</button>}
      </div>
      {hint && isOpen && <div className="prop-section-hint">{hint}</div>}
      {isOpen && <div className="prop-section-content">{children}</div>}
    </div>
  );
}
