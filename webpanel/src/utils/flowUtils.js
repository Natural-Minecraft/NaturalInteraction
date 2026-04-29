// Flow utility functions — shared between Editor and components

export const TYPE_STYLES = {
  DIALOGUE:  { bg: '#0d1f3c', border: '#4facfe', label: 'Dialogue', icon: '💬' },
  CHOICE:    { bg: '#0d2218', border: '#3fb950', label: 'Choice',   icon: '🔀' },
  ACTION:    { bg: '#2a1a08', border: '#f7971e', label: 'Action',   icon: '⚡' },
  CONDITION: { bg: '#2a0d0c', border: '#f85149', label: 'Condition',icon: '🔴' },
  REWARD:    { bg: '#2a2000', border: '#ffd200', label: 'Reward',   icon: '⭐' },
};

export function getNodeType(node) {
  if (node.giveReward || (node.commandRewards?.length > 0)) return 'REWARD';
  if (node.actions?.some(a => a.type?.startsWith('JUMP_IF'))) return 'CONDITION';
  if (node.actions?.length > 0) return 'ACTION';
  if (node.options?.length > 1) return 'CHOICE';
  return 'DIALOGUE';
}

export function parseJumpTarget(action) {
  if (action.targetNodeId) return action.targetNodeId;
  if (action.value?.includes(',')) {
    const parts = action.value.split(',');
    return parts[parts.length - 1];
  }
  return null;
}

export function buildFlowData(interactionData) {
  const nodesMap = interactionData.nodes || {};
  const rootId = interactionData.rootNodeId;
  const nodeIds = Object.keys(nodesMap);

  // Use saved positions if available, otherwise BFS layout
  const positions = {};
  const hasSavedPositions = nodeIds.some(id => nodesMap[id]._x != null);

  if (hasSavedPositions) {
    nodeIds.forEach(id => {
      positions[id] = { x: nodesMap[id]._x || 0, y: nodesMap[id]._y || 0 };
    });
  } else {
    // BFS tree layout
    const visited = new Set();
    const colCount = {};
    const queue = [[rootId, 0]];
    const NODE_W = 280, GAP_X = 140, GAP_Y = 60, NODE_H = 130;

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
      n.actions?.forEach(a => { const t = parseJumpTarget(a); if (t && nodesMap[t]) children.push(t); });
      children.forEach(cid => { if (!visited.has(cid)) queue.push([cid, col + 1]); });
    }

    let orphanY = 0;
    nodeIds.forEach(id => {
      if (!visited.has(id)) { positions[id] = { x: -350, y: orphanY }; orphanY += 170; }
    });
  }

  const flowNodes = nodeIds.map(id => ({
    id, type: 'dialogueNode',
    position: positions[id] || { x: 0, y: 0 },
    data: { nodeData: nodesMap[id], isRoot: id === rootId },
  }));

  const flowEdges = [];
  let edgeId = 0;
  nodeIds.forEach(id => {
    const n = nodesMap[id];
    n.options?.forEach((opt, i) => {
      if (opt.targetNodeId && nodesMap[opt.targetNodeId]) {
        flowEdges.push({
          id: `e-${edgeId++}`, source: id, target: opt.targetNodeId,
          sourceHandle: `opt-${i}`, type: 'smoothstep',
          style: { stroke: '#3fb950', strokeWidth: 2 },
          label: (opt.text || '').substring(0, 18),
          labelStyle: { fill: '#3fb950', fontSize: 9, fontWeight: 600 },
          labelBgStyle: { fill: '#0e1117', fillOpacity: 0.8 },
          labelBgPadding: [4, 2], labelBgBorderRadius: 3,
        });
      }
    });
    if (n.nextNodeId && nodesMap[n.nextNodeId]) {
      flowEdges.push({
        id: `e-${edgeId++}`, source: id, target: n.nextNodeId,
        sourceHandle: 'default', type: 'smoothstep',
        style: { stroke: '#4facfe', strokeWidth: 2 }, animated: true,
      });
    }
    n.actions?.forEach(a => {
      if (!a.type?.startsWith('JUMP_IF')) return;
      const target = parseJumpTarget(a);
      if (target && nodesMap[target]) {
        flowEdges.push({
          id: `e-${edgeId++}`, source: id, target, sourceHandle: 'default',
          type: 'smoothstep',
          style: { stroke: '#f85149', strokeWidth: 2, strokeDasharray: '6,4' },
          label: a.type, labelStyle: { fill: '#f85149', fontSize: 8, fontWeight: 600 },
          labelBgStyle: { fill: '#0e1117', fillOpacity: 0.8 },
          labelBgPadding: [4, 2], labelBgBorderRadius: 3,
        });
      }
    });
  });

  return { flowNodes, flowEdges };
}
