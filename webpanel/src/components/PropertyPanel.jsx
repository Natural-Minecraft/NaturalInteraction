import { useState } from 'react';
import { parseMCText } from '../utils/mcColors';

const ACTION_TYPES = [
  'ADD_TAG','REMOVE_TAG','JUMP_IF_TAG','JUMP_IF_NOT_TAG','JUMP_IF_ITEM',
  'JUMP_IF_FACT','JUMP_IF_NOT_FACT','SET_FACT','ADD_FACT','REMOVE_FACT',
  'TAKE_ITEM','ITEM','COMMAND','SOUND','SCREENEFFECT','ZOOM','INVISIBLE',
  'SUBTITLE','TITLE','PARTICLE','TELEPORT','NPC_WALK','NPC_SKIN',
];

export default function PropertyPanel({ node, allNodeIds, rootNodeId, onUpdate, onDelete, onSetRoot, onDuplicate }) {
  if (!node) {
    return (
      <div className="prop-panel">
        <div className="prop-placeholder">
          <div style={{ fontSize: 40, marginBottom: 12 }}>🖱️</div>
          <p>Click a node to edit</p>
          <p className="text-muted" style={{ marginTop: 6 }}>Drag to connect nodes</p>
        </div>
      </div>
    );
  }

  const [local, setLocal] = useState({ ...node });

  // Reset local state when selected node changes
  if (local.id !== node.id) {
    setLocal({ ...node });
  }

  function set(key, val) {
    setLocal(prev => ({ ...prev, [key]: val }));
  }

  function apply() {
    const updated = { ...local };
    // If id changed, rename
    if (updated.id !== node.id) {
      // Caller handles rename
    }
    onUpdate(updated);
  }

  function setOption(i, key, val) {
    const opts = [...(local.options || [])];
    opts[i] = { ...opts[i], [key]: val };
    setLocal(prev => ({ ...prev, options: opts }));
  }

  function addOption() {
    setLocal(prev => ({ ...prev, options: [...(prev.options || []), { text: 'New Option', targetNodeId: '' }] }));
  }

  function removeOption(i) {
    setLocal(prev => ({ ...prev, options: prev.options.filter((_, j) => j !== i) }));
  }

  function setAction(i, key, val) {
    const acts = [...(local.actions || [])];
    acts[i] = { ...acts[i], [key]: val };
    setLocal(prev => ({ ...prev, actions: acts }));
  }

  function addAction() {
    setLocal(prev => ({ ...prev, actions: [...(prev.actions || []), { type: 'COMMAND', value: '' }] }));
  }

  function removeAction(i) {
    setLocal(prev => ({ ...prev, actions: prev.actions.filter((_, j) => j !== i) }));
  }

  function setReward(i, val) {
    const rews = [...(local.commandRewards || [])];
    rews[i] = val;
    setLocal(prev => ({ ...prev, commandRewards: rews }));
  }

  function addReward() {
    setLocal(prev => ({ ...prev, commandRewards: [...(prev.commandRewards || []), ''] }));
  }

  function removeReward(i) {
    setLocal(prev => ({ ...prev, commandRewards: prev.commandRewards.filter((_, j) => j !== i) }));
  }

  const isRoot = node.id === rootNodeId;
  const otherNodes = allNodeIds.filter(id => id !== local.id);

  return (
    <div className="prop-panel">
      <div className="prop-header">
        <span className="prop-type-badge">{node.id}</span>
        {isRoot && <span className="flow-root-badge" style={{ marginLeft: 'auto' }}>★ ROOT</span>}
      </div>

      <div className="prop-body">
        {/* Node ID */}
        <div className="prop-section">
          <label className="prop-label">Node ID</label>
          <input className="prop-input" value={local.id} onChange={e => set('id', e.target.value)} />
        </div>

        {/* Text */}
        <div className="prop-section">
          <label className="prop-label">Dialogue Text</label>
          <textarea className="prop-textarea" rows={3} value={local.text || ''} onChange={e => set('text', e.target.value)} />
          <div className="mc-preview">{parseMCText(local.text || '')}</div>
        </div>

        {/* Duration + Delay */}
        <div className="prop-row-2">
          <div className="prop-section">
            <label className="prop-label">Duration (s)</label>
            <input className="prop-input" type="number" value={local.durationSeconds || 5} onChange={e => set('durationSeconds', +e.target.value)} />
          </div>
          <div className="prop-section">
            <label className="prop-label">Delay (ticks)</label>
            <input className="prop-input" type="number" value={local.delayBeforeDialogueTicks || 0} onChange={e => set('delayBeforeDialogueTicks', +e.target.value)} />
          </div>
        </div>

        {/* Next Node */}
        <div className="prop-section">
          <label className="prop-label">Next Node (fallthrough)</label>
          <select className="prop-input" value={local.nextNodeId || ''} onChange={e => set('nextNodeId', e.target.value || undefined)}>
            <option value="">— none (end) —</option>
            {otherNodes.map(id => <option key={id} value={id}>{id}</option>)}
          </select>
        </div>

        {/* Checkboxes */}
        <div className="prop-row-2">
          <label className="prop-checkbox">
            <input type="checkbox" checked={local.skippable !== false} onChange={e => set('skippable', e.target.checked)} />
            <span>Skippable</span>
          </label>
          <label className="prop-checkbox">
            <input type="checkbox" checked={!!local.giveReward} onChange={e => set('giveReward', e.target.checked)} />
            <span>Give Reward</span>
          </label>
        </div>

        {/* Options */}
        <div className="prop-section">
          <div className="prop-section-header">
            <label className="prop-label">Options</label>
            <button className="btn-tiny" onClick={addOption}>+ Add</button>
          </div>
          {(local.options || []).map((opt, i) => (
            <div key={i} className="prop-row-item">
              <input className="prop-input" placeholder="Text" value={opt.text || ''} onChange={e => setOption(i, 'text', e.target.value)} />
              <select className="prop-input" value={opt.targetNodeId || ''} onChange={e => setOption(i, 'targetNodeId', e.target.value)}>
                <option value="">— target —</option>
                {otherNodes.map(id => <option key={id} value={id}>{id}</option>)}
              </select>
              <button className="btn-icon" onClick={() => removeOption(i)}>✕</button>
            </div>
          ))}
          {(!local.options || local.options.length === 0) && <div className="prop-empty">No options (linear)</div>}
        </div>

        {/* Actions */}
        <div className="prop-section">
          <div className="prop-section-header">
            <label className="prop-label">Actions</label>
            <button className="btn-tiny" onClick={addAction}>+ Add</button>
          </div>
          {(local.actions || []).map((act, i) => (
            <div key={i} className="prop-row-item">
              <select className="prop-input" style={{ flex: '0 0 130px' }} value={act.type || ''} onChange={e => setAction(i, 'type', e.target.value)}>
                {ACTION_TYPES.map(t => <option key={t}>{t}</option>)}
              </select>
              <input className="prop-input" placeholder="value" value={act.value || ''} onChange={e => setAction(i, 'value', e.target.value)} />
              <button className="btn-icon" onClick={() => removeAction(i)}>✕</button>
            </div>
          ))}
          {(!local.actions || local.actions.length === 0) && <div className="prop-empty">No actions</div>}
        </div>

        {/* Command Rewards */}
        <div className="prop-section">
          <div className="prop-section-header">
            <label className="prop-label">Command Rewards</label>
            <button className="btn-tiny" onClick={addReward}>+ Add</button>
          </div>
          {(local.commandRewards || []).map((cmd, i) => (
            <div key={i} className="prop-row-item">
              <input className="prop-input" placeholder="/command" value={cmd} onChange={e => setReward(i, e.target.value)} />
              <button className="btn-icon" onClick={() => removeReward(i)}>✕</button>
            </div>
          ))}
          {(!local.commandRewards || local.commandRewards.length === 0) && <div className="prop-empty">No rewards</div>}
        </div>
      </div>

      <div className="prop-footer">
        <button className="btn btn-primary" style={{ flex: 1 }} onClick={apply}>✓ Apply</button>
        <button className="btn btn-secondary" onClick={onDuplicate} title="Duplicate node">📋</button>
        <button className="btn btn-secondary" onClick={onSetRoot} title="Set as root">⭐</button>
        <button className="btn btn-danger" onClick={onDelete} title="Delete (Del)">🗑</button>
      </div>
      <div className="prop-shortcuts">
        <span>Ctrl+S save</span><span>Ctrl+Z undo</span><span>Del delete</span>
      </div>
    </div>
  );
}
