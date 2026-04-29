import { useState, useRef, useCallback, useEffect } from 'react';
import { parseMCText } from '../utils/mcColors';

// ── Action categories ────────────────────────────
const FLOW_ACTIONS = ['JUMP_IF_TAG','JUMP_IF_NOT_TAG','JUMP_IF_ITEM','JUMP_IF_FACT','JUMP_IF_NOT_FACT'];
const TAG_ACTIONS = ['ADD_TAG','REMOVE_TAG'];
const FACT_ACTIONS = ['SET_FACT','ADD_FACT','REMOVE_FACT'];
const NPC_ACTIONS = ['NPC_WALK','NPC_SKIN'];
const EFFECT_ACTIONS = ['SOUND','SCREENEFFECT','ZOOM','INVISIBLE','SUBTITLE','TITLE','PARTICLE','TELEPORT','CINEMATIC'];
const COMMAND_ACTIONS = ['COMMAND','ITEM','TAKE_ITEM'];

const FLOW_LABELS = { JUMP_IF_TAG:'Punya tanda', JUMP_IF_NOT_TAG:'Tidak punya tanda', JUMP_IF_ITEM:'Punya item', JUMP_IF_FACT:'Catatan = true', JUMP_IF_NOT_FACT:'Catatan = false' };
const TAG_LABELS = { ADD_TAG:'＋ Beri tanda', REMOVE_TAG:'－ Hapus tanda' };
const NPC_LABELS = { NPC_WALK:'NPC berjalan ke', NPC_SKIN:'Ganti skin NPC' };
const EFFECT_LABELS = { SOUND:'🔊 Suara', SCREENEFFECT:'🖥️ Efek layar', ZOOM:'🔍 Zoom', INVISIBLE:'👻 Invisible', SUBTITLE:'📝 Subtitle', TITLE:'📢 Judul', PARTICLE:'✨ Partikel', TELEPORT:'🌀 Teleport', CINEMATIC:'🎥 Cinematic' };
const CMD_LABELS = { COMMAND:'⚡ Perintah', ITEM:'📦 Beri item', TAKE_ITEM:'🗑️ Ambil item' };

// Category definitions
const CATEGORIES = [
  { id: 'basic', icon: '📝', label: 'Dasar', desc: 'Teks, durasi, alur', color: '#4facfe' },
  { id: 'options', icon: '🔀', label: 'Pilihan', desc: 'Opsi pemain', color: '#3fb950' },
  { id: 'conditions', icon: '🔍', label: 'Syarat', desc: 'Cek kondisi', color: '#f85149' },
  { id: 'tags', icon: '🏷️', label: 'Setelah', desc: 'Beri/hapus tanda', color: '#f7971e' },
  { id: 'facts', icon: '📋', label: 'Catatan', desc: 'Progress pemain', color: '#58a6ff' },
  { id: 'npc', icon: '🎭', label: 'NPC', desc: 'Gerakan & skin', color: '#d2a8ff' },
  { id: 'effects', icon: '🎬', label: 'Efek', desc: 'Visual & audio', color: '#f778ba' },
  { id: 'commands', icon: '⚡', label: 'Perintah', desc: 'Command server', color: '#ffd200' },
  { id: 'rewards', icon: '🎁', label: 'Hadiah', desc: 'Reward pemain', color: '#3fb950' },
];

export default function PropertyPanel({ node, allNodeIds, rootNodeId, onUpdate, onDelete, onSetRoot, onDuplicate }) {
  const [panelWidth, setPanelWidth] = useState(320);
  const [dragging, setDragging] = useState(false);
  const panelRef = useRef(null);

  const onDragStart = useCallback((e) => { e.preventDefault(); setDragging(true); }, []);
  useEffect(() => {
    if (!dragging) return;
    const onMove = (e) => setPanelWidth(Math.max(280, Math.min(700, window.innerWidth - e.clientX)));
    const onUp = () => setDragging(false);
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => { window.removeEventListener('mousemove', onMove); window.removeEventListener('mouseup', onUp); };
  }, [dragging]);

  if (!node) {
    return (
      <div className="prop-panel" ref={panelRef} style={{ width: panelWidth }}>
        <div className="prop-resize-handle" onMouseDown={onDragStart}><div className="prop-resize-dots" /></div>
        <div className="prop-placeholder">
          <div style={{ fontSize: 48, marginBottom: 16 }}>🖱️</div>
          <p style={{ fontWeight: 600, marginBottom: 4 }}>Pilih node untuk diedit</p>
          <p className="text-muted">Klik node di canvas, lalu pilih kategori</p>
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
    panelWidth={panelWidth} onDragStart={onDragStart} panelRef={panelRef}
  />;
}

function PropertyEditor({ node, allNodeIds, rootNodeId, onUpdate, onDelete, onSetRoot, onDuplicate,
  panelWidth, onDragStart, panelRef }) {

  const [local, setLocal] = useState({ ...node });
  const [activeCategory, setActiveCategory] = useState(null); // null = grid view
  const [slideDir, setSlideDir] = useState('in'); // animation direction

  useEffect(() => { setLocal({ ...node }); setActiveCategory(null); }, [node.id]);

  const set = (k, v) => setLocal(prev => ({ ...prev, [k]: v }));
  const otherNodes = allNodeIds.filter(id => id !== local.id);
  const isRoot = node.id === rootNodeId;
  const actions = local.actions || [];

  // Count per category
  const counts = {
    options: (local.options || []).length,
    conditions: actions.filter(a => FLOW_ACTIONS.includes(a.type)).length,
    tags: actions.filter(a => TAG_ACTIONS.includes(a.type)).length,
    facts: actions.filter(a => FACT_ACTIONS.includes(a.type)).length,
    npc: actions.filter(a => NPC_ACTIONS.includes(a.type)).length,
    effects: actions.filter(a => EFFECT_ACTIONS.includes(a.type)).length,
    commands: actions.filter(a => COMMAND_ACTIONS.includes(a.type)).length,
    rewards: (local.commandRewards || []).length,
    basic: 0,
  };

  function openCategory(id) { setSlideDir('in'); setActiveCategory(id); }
  function backToGrid() { setSlideDir('out'); setTimeout(() => setActiveCategory(null), 0); }

  // ── Action CRUD ───────────────────────────
  function addAction(type) { setLocal(prev => ({ ...prev, actions: [...(prev.actions || []), { type, value: '' }] })); }
  function removeAction(idx) { setLocal(prev => ({ ...prev, actions: prev.actions.filter((_, i) => i !== idx) })); }
  function updateAction(idx, key, val) {
    setLocal(prev => { const a = [...prev.actions]; a[idx] = { ...a[idx], [key]: val }; return { ...prev, actions: a }; });
  }
  function getActionsOfType(types) {
    return (local.actions || []).map((a, i) => ({ ...a, _idx: i })).filter(a => types.includes(a.type));
  }

  // ── Options CRUD ──────────────────────────
  function setOption(i, key, val) {
    const o = [...(local.options || [])]; o[i] = { ...o[i], [key]: val };
    setLocal(prev => ({ ...prev, options: o }));
  }
  function addOption() { setLocal(prev => ({ ...prev, options: [...(prev.options || []), { text: 'Pilihan baru', targetNodeId: '' }] })); }
  function removeOption(i) { setLocal(prev => ({ ...prev, options: prev.options.filter((_, j) => j !== i) })); }

  // ── Rewards CRUD ──────────────────────────
  function setReward(i, val) {
    const r = [...(local.commandRewards || [])]; r[i] = val;
    setLocal(prev => ({ ...prev, commandRewards: r }));
  }
  function addReward() { setLocal(prev => ({ ...prev, commandRewards: [...(prev.commandRewards || []), ''] })); }
  function removeReward(i) { setLocal(prev => ({ ...prev, commandRewards: prev.commandRewards.filter((_, j) => j !== i) })); }

  function apply() { onUpdate({ ...local }); }

  // ═══ DETAIL RENDERERS ═══════════════════════
  function renderDetail() {
    switch (activeCategory) {
      case 'basic': return renderBasic();
      case 'options': return renderOptions();
      case 'conditions': return renderConditions();
      case 'tags': return renderTags();
      case 'facts': return renderFacts();
      case 'npc': return renderNPC();
      case 'effects': return renderEffects();
      case 'commands': return renderCommands();
      case 'rewards': return renderRewards();
      default: return null;
    }
  }

  function renderBasic() {
    return (<>
      <div className="prop-section">
        <label className="prop-label">ID Node</label>
        <input className="prop-input" value={local.id} onChange={e => set('id', e.target.value)} />
      </div>
      <div className="prop-section">
        <label className="prop-label">Teks Dialog</label>
        <textarea className="prop-textarea" rows={4} value={local.text || ''} onChange={e => set('text', e.target.value)}
          placeholder="Gunakan &e kuning, &b cyan, &c merah..." />
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
      <div className="prop-section">
        <label className="prop-label">Lanjut ke Node</label>
        <select className="prop-input" value={local.nextNodeId || ''} onChange={e => set('nextNodeId', e.target.value || undefined)}>
          <option value="">— selesai (berhenti) —</option>
          {otherNodes.map(id => <option key={id} value={id}>{id}</option>)}
        </select>
      </div>
      <div className="prop-row-2">
        <label className="prop-checkbox"><input type="checkbox" checked={local.skippable !== false} onChange={e => set('skippable', e.target.checked)} /><span>Bisa di-skip</span></label>
        <label className="prop-checkbox"><input type="checkbox" checked={!!local.giveReward} onChange={e => set('giveReward', e.target.checked)} /><span>Beri hadiah</span></label>
      </div>
    </>);
  }

  function renderOptions() {
    return (<>
      <div className="detail-hint">Opsi yang muncul untuk pemain setelah dialog</div>
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
      <button className="btn-tiny btn-add-full" onClick={addOption}>+ Tambah Pilihan</button>
    </>);
  }

  function renderConditions() {
    const items = getActionsOfType(FLOW_ACTIONS);
    return (<>
      <div className="detail-hint">Cek kondisi pemain sebelum lanjut ke node tertentu</div>
      {items.map(a => (
        <div key={a._idx} className="prop-condition-row">
          <div className="prop-condition-label">Jika pemain</div>
          <select className="prop-input" value={a.type} onChange={e => updateAction(a._idx, 'type', e.target.value)}>
            {FLOW_ACTIONS.map(t => <option key={t} value={t}>{FLOW_LABELS[t]}</option>)}
          </select>
          <input className="prop-input" placeholder="nama tanda/item..." value={a.value?.split(',')[0] || ''}
            onChange={e => { const t = a.value?.split(',')[1] || ''; updateAction(a._idx, 'value', t ? `${e.target.value},${t}` : e.target.value); }} />
          <div className="prop-condition-label">→ pergi ke</div>
          <select className="prop-input" value={a.targetNodeId || a.value?.split(',')[1] || ''}
            onChange={e => { const c = a.value?.split(',')[0] || ''; updateAction(a._idx, 'value', `${c},${e.target.value}`); updateAction(a._idx, 'targetNodeId', e.target.value); }}>
            <option value="">— node —</option>
            {otherNodes.map(id => <option key={id} value={id}>{id}</option>)}
          </select>
          <button className="btn-icon" onClick={() => removeAction(a._idx)}>✕</button>
        </div>
      ))}
      <button className="btn-tiny btn-add-full" onClick={() => addAction('JUMP_IF_TAG')}>+ Tambah Syarat</button>
    </>);
  }

  function renderTags() {
    const items = getActionsOfType(TAG_ACTIONS);
    return (<>
      <div className="detail-hint">Tanda yang diberikan/dihapus setelah dialog ini selesai</div>
      {items.map(a => (
        <div key={a._idx} className="prop-row-item">
          <select className="prop-input" style={{ flex: '0 0 120px' }} value={a.type} onChange={e => updateAction(a._idx, 'type', e.target.value)}>
            {TAG_ACTIONS.map(t => <option key={t} value={t}>{TAG_LABELS[t]}</option>)}
          </select>
          <input className="prop-input" placeholder="nama tanda..." value={a.value || ''} onChange={e => updateAction(a._idx, 'value', e.target.value)} />
          <button className="btn-icon" onClick={() => removeAction(a._idx)}>✕</button>
        </div>
      ))}
      <button className="btn-tiny btn-add-full" onClick={() => addAction('ADD_TAG')}>+ Tambah Tanda</button>
    </>);
  }

  function renderFacts() {
    const items = getActionsOfType(FACT_ACTIONS);
    return (<>
      <div className="detail-hint">Tandai progress pemain (misal: quest_done = true)</div>
      {items.map(a => {
        const parts = (a.value || '').split(','); const key = parts[0] || ''; const val = parts[1] || 'true';
        return (
          <div key={a._idx} className="prop-fact-row">
            <span className="prop-fact-label">Set catatan</span>
            <input className="prop-input" placeholder="nama catatan..." value={key} onChange={e => updateAction(a._idx, 'value', `${e.target.value},${val}`)} />
            <span className="prop-fact-label">=</span>
            <select className="prop-input" style={{ flex: '0 0 80px' }} value={val} onChange={e => updateAction(a._idx, 'value', `${key},${e.target.value}`)}>
              <option value="true">TRUE</option><option value="false">FALSE</option>
            </select>
            <button className="btn-icon" onClick={() => removeAction(a._idx)}>✕</button>
          </div>
        );
      })}
      <button className="btn-tiny btn-add-full" onClick={() => addAction('SET_FACT')}>+ Tambah Catatan</button>
    </>);
  }

  function renderNPC() {
    const items = getActionsOfType(NPC_ACTIONS);
    return (<>
      <div className="detail-hint">Atur gerakan dan penampilan NPC</div>
      {items.map(a => (
        <div key={a._idx} className="prop-row-item">
          <select className="prop-input" style={{ flex: '0 0 140px' }} value={a.type} onChange={e => updateAction(a._idx, 'type', e.target.value)}>
            {NPC_ACTIONS.map(t => <option key={t} value={t}>{NPC_LABELS[t]}</option>)}
          </select>
          <input className="prop-input" placeholder="koordinat / skin name..." value={a.value || ''} onChange={e => updateAction(a._idx, 'value', e.target.value)} />
          <button className="btn-icon" onClick={() => removeAction(a._idx)}>✕</button>
        </div>
      ))}
      <button className="btn-tiny btn-add-full" onClick={() => addAction('NPC_WALK')}>+ Tambah NPC Action</button>
    </>);
  }

  function renderEffects() {
    const items = getActionsOfType(EFFECT_ACTIONS);
    return (<>
      <div className="detail-hint">Efek visual dan audio yang diputar saat dialog</div>
      {items.map(a => (
        <div key={a._idx} className="prop-row-item">
          <select className="prop-input" style={{ flex: '0 0 140px' }} value={a.type} onChange={e => updateAction(a._idx, 'type', e.target.value)}>
            {EFFECT_ACTIONS.map(t => <option key={t} value={t}>{EFFECT_LABELS[t]}</option>)}
          </select>
          <input className="prop-input" placeholder="value..." value={a.value || ''} onChange={e => updateAction(a._idx, 'value', e.target.value)} />
          <button className="btn-icon" onClick={() => removeAction(a._idx)}>✕</button>
        </div>
      ))}
      <button className="btn-tiny btn-add-full" onClick={() => addAction('SOUND')}>+ Tambah Efek</button>
    </>);
  }

  function renderCommands() {
    const items = getActionsOfType(COMMAND_ACTIONS);
    return (<>
      <div className="detail-hint">Perintah server yang dijalankan saat dialog ini</div>
      {items.map(a => (
        <div key={a._idx} className="prop-row-item">
          <select className="prop-input" style={{ flex: '0 0 130px' }} value={a.type} onChange={e => updateAction(a._idx, 'type', e.target.value)}>
            {COMMAND_ACTIONS.map(t => <option key={t} value={t}>{CMD_LABELS[t]}</option>)}
          </select>
          <input className="prop-input" placeholder="/command atau item..." value={a.value || ''} onChange={e => updateAction(a._idx, 'value', e.target.value)} />
          <button className="btn-icon" onClick={() => removeAction(a._idx)}>✕</button>
        </div>
      ))}
      <button className="btn-tiny btn-add-full" onClick={() => addAction('COMMAND')}>+ Tambah Perintah</button>
    </>);
  }

  function renderRewards() {
    return (<>
      <div className="detail-hint">Perintah yang dijalankan sebagai hadiah (butuh "Beri hadiah" ✓)</div>
      {!local.giveReward && <div className="detail-warn">⚠ Aktifkan "Beri hadiah" di pengaturan Dasar terlebih dahulu</div>}
      {(local.commandRewards || []).map((cmd, i) => (
        <div key={i} className="prop-row-item">
          <input className="prop-input" placeholder="/give @p diamond 1" value={cmd} onChange={e => setReward(i, e.target.value)} />
          <button className="btn-icon" onClick={() => removeReward(i)}>✕</button>
        </div>
      ))}
      {(!local.commandRewards || local.commandRewards.length === 0) && <div className="prop-empty">Belum ada hadiah</div>}
      <button className="btn-tiny btn-add-full" onClick={addReward}>+ Tambah Hadiah</button>
    </>);
  }

  // ═══ RENDER ═════════════════════════════════════
  const activeCat = CATEGORIES.find(c => c.id === activeCategory);

  return (
    <div className="prop-panel" ref={panelRef} style={{ width: panelWidth }}>
      <div className="prop-resize-handle" onMouseDown={onDragStart}><div className="prop-resize-dots" /></div>

      {/* Header */}
      <div className="prop-header">
        {activeCategory ? (
          <>
            <button className="prop-back-btn" onClick={backToGrid}>←</button>
            <span style={{ fontSize: 16 }}>{activeCat?.icon}</span>
            <span className="prop-type-badge">{activeCat?.label}</span>
          </>
        ) : (
          <>
            <span className="prop-type-badge">{node.id}</span>
            {isRoot && <span className="flow-root-badge" style={{ marginLeft: 'auto' }}>★ ROOT</span>}
          </>
        )}
      </div>

      <div className="prop-body">
        {activeCategory ? (
          /* ─── DETAIL VIEW ─── */
          <div className={`prop-detail-view slide-${slideDir}`}>
            {renderDetail()}
          </div>
        ) : (
          /* ─── CARD GRID ─── */
          <div className="prop-card-grid">
            {CATEGORIES.map(cat => (
              <button key={cat.id} className="prop-card" onClick={() => openCategory(cat.id)}
                style={{ '--card-color': cat.color }}>
                <div className="prop-card-icon">{cat.icon}</div>
                <div className="prop-card-info">
                  <div className="prop-card-label">{cat.label}</div>
                  <div className="prop-card-desc">{cat.desc}</div>
                </div>
                {counts[cat.id] > 0 && <span className="prop-card-count">{counts[cat.id]}</span>}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="prop-footer">
        {activeCategory ? (
          <button className="btn btn-primary" style={{ flex: 1 }} onClick={apply}>✓ Terapkan</button>
        ) : (
          <>
            <button className="btn btn-primary" style={{ flex: 1 }} onClick={apply}>✓ Terapkan Semua</button>
            <button className="btn btn-secondary" onClick={onDuplicate} title="Duplikat">📋</button>
            <button className="btn btn-secondary" onClick={onSetRoot} title="Root">⭐</button>
            <button className="btn btn-danger" onClick={onDelete} title="Hapus">🗑</button>
          </>
        )}
      </div>
    </div>
  );
}
