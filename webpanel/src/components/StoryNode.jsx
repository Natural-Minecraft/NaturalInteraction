import { Handle, Position } from '@xyflow/react';
import { getNodeType, TYPE_STYLES } from '../utils/flowUtils';
import { stripMC } from '../utils/mcColors';

export default function StoryNode({ data, selected }) {
  const type = getNodeType(data.nodeData);
  const style = TYPE_STYLES[type];
  const node = data.nodeData;
  const isRoot = data.isRoot;
  const text = stripMC(node.text || '');
  const preview = text.length > 40 ? text.substring(0, 40) + '…' : (text || '(empty)');

  return (
    <div className={`flow-node ${selected ? 'selected' : ''}`} style={{
      background: `linear-gradient(145deg, ${style.bg} 0%, ${style.bg}ee 100%)`,
      borderColor: selected ? style.border : (isRoot ? '#ffd200' : style.border + '66'),
      borderWidth: selected ? 2 : 1,
      boxShadow: selected
        ? `0 0 24px ${style.border}44, inset 0 1px 0 ${style.border}22`
        : (isRoot ? '0 0 16px #ffd20022' : `0 2px 8px rgba(0,0,0,0.3)`),
    }}>
      <Handle type="target" position={Position.Left} className="flow-handle flow-handle-in" />

      {/* Header with gradient */}
      <div className="flow-node-header" style={{
        background: `linear-gradient(90deg, ${style.border}25 0%, transparent 100%)`,
        borderBottom: `1px solid ${style.border}33`,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ fontSize: 12 }}>{style.icon}</span>
          <span style={{ color: style.border, fontSize: 10, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px' }}>
            {style.label}
          </span>
        </div>
        <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
          {isRoot && <span className="flow-root-badge">★ ROOT</span>}
          {node.skippable === false && <span className="flow-lock-badge">🔒</span>}
        </div>
      </div>

      {/* Body */}
      <div className="flow-node-body">
        <div className="flow-node-id">{node.id}</div>
        <div className="flow-node-text">{preview}</div>

        {/* Duration indicator */}
        {node.durationSeconds > 0 && (
          <div className="flow-node-duration">⏱ {node.durationSeconds}s</div>
        )}

        {/* Options */}
        {node.options?.length > 0 && (
          <div className="flow-node-options">
            {node.options.map((opt, i) => (
              <div key={i} className="flow-node-opt">
                <span className="flow-opt-arrow">▸</span>
                <span className="flow-opt-text">{(opt.text || '—').substring(0, 25)}</span>
                <Handle type="source" position={Position.Right} id={`opt-${i}`}
                  className="flow-handle flow-handle-opt" style={{ top: 'auto', position: 'relative' }} />
              </div>
            ))}
          </div>
        )}

        {/* Action badges */}
        {node.actions?.length > 0 && (
          <div className="flow-node-actions">
            {node.actions.slice(0, 3).map((a, i) => (
              <span key={i} className="flow-action-pill" style={{
                borderColor: a.type?.startsWith('JUMP_IF') ? '#f85149' : '#f7971e',
                color: a.type?.startsWith('JUMP_IF') ? '#f85149' : '#f7971e',
              }}>{a.type?.replace('JUMP_IF_', 'IF ')}</span>
            ))}
            {node.actions.length > 3 && <span className="flow-action-more">+{node.actions.length - 3}</span>}
          </div>
        )}

        {/* Reward badge */}
        {node.giveReward && (
          <div className="flow-node-reward">
            <span>⭐</span>
            <span>{node.commandRewards?.length || 0} reward(s)</span>
          </div>
        )}
      </div>

      {/* Default output */}
      {(!node.options || node.options.length === 0) && (
        <Handle type="source" position={Position.Right} id="default" className="flow-handle flow-handle-out" />
      )}
    </div>
  );
}
