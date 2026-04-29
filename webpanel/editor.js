// ═══════════════════════════════════════════════════════════
//  NaturalInteraction — Visual Node Graph Editor
//  editor.js  |  dipanggil via initEditor() dari main.js
// ═══════════════════════════════════════════════════════════

// ─── Node type config ──────────────────────────────────────
const NODE_TYPES = {
  DIALOGUE:  { label: 'Dialogue',  color: '#4facfe', bg: '#0d1f3c', border: '#4facfe' },
  CHOICE:    { label: 'Choice',    color: '#3fb950', bg: '#0d2218', border: '#3fb950' },
  ACTION:    { label: 'Action',    color: '#f7971e', bg: '#2a1a08', border: '#f7971e' },
  CONDITION: { label: 'Condition', color: '#f85149', bg: '#2a0d0c', border: '#f85149' },
  REWARD:    { label: 'Reward',    color: '#ffd200', bg: '#2a2000', border: '#ffd200' },
};

const NODE_W       = 220;
const NODE_H_BASE  = 90;
const OPT_ROW_H    = 22;
const PORT_R       = 6;
const HEADER_H     = 24;

// ─── Minecraft color code map ──────────────────────────────
const MC_COLORS = {
  '0': '#000000', '1': '#0000AA', '2': '#00AA00', '3': '#00AAAA',
  '4': '#AA0000', '5': '#AA00AA', '6': '#FFAA00', '7': '#AAAAAA',
  '8': '#555555', '9': '#5555FF', 'a': '#55FF55', 'b': '#55FFFF',
  'c': '#FF5555', 'd': '#FF55FF', 'e': '#FFFF55', 'f': '#FFFFFF',
};

// ─── Parse Minecraft color codes → HTML spans ──────────────
export function parseMCText(raw = '') {
  let html = '';
  let color = '#e6edf3';
  let bold = false, italic = false, strike = false;
  let i = 0;
  while (i < raw.length) {
    // &#RRGGBB hex
    if (raw[i] === '&' && raw[i + 1] === '#' && i + 8 <= raw.length) {
      const hex = raw.substring(i + 1, i + 8);
      if (/^#[0-9A-Fa-f]{6}$/.test(hex)) { color = hex; i += 8; continue; }
    }
    // &x codes
    if (raw[i] === '&' && i + 1 < raw.length) {
      const c = raw[i + 1].toLowerCase();
      if (MC_COLORS[c]) { color = MC_COLORS[c]; i += 2; continue; }
      if (c === 'l') { bold   = true;  i += 2; continue; }
      if (c === 'o') { italic = true;  i += 2; continue; }
      if (c === 'm') { strike = true;  i += 2; continue; }
      if (c === 'r') { color = '#e6edf3'; bold = italic = strike = false; i += 2; continue; }
    }
    const ch = raw[i] === '<' ? '&lt;' : raw[i] === '>' ? '&gt;' : raw[i] === '&' ? '&amp;' : raw[i];
    let style = `color:${color};`;
    if (bold)   style += 'font-weight:700;';
    if (italic) style += 'font-style:italic;';
    if (strike) style += 'text-decoration:line-through;';
    html += `<span style="${style}">${ch}</span>`;
    i++;
  }
  return html;
}

// ─── Strip MC codes → plain text ──────────────────────────
function stripMC(text = '') {
  return text.replace(/&#[0-9A-Fa-f]{6}|&[0-9a-flmnorkrR]/g, '');
}

// ─── Determine node type from its data ────────────────────
function getNodeType(node) {
  if (node.giveReward || (node.commandRewards && node.commandRewards.length > 0)) return 'REWARD';
  if (node.actions && node.actions.some(a => a.type && a.type.startsWith('JUMP_IF')))  return 'CONDITION';
  if (node.actions && node.actions.length > 0) return 'ACTION';
  if (node.options && node.options.length > 1)  return 'CHOICE';
  return 'DIALOGUE';
}

// ─── Dynamic node height ───────────────────────────────────
function nodeHeight(node) {
  const optCount = (node.options || []).length;
  return NODE_H_BASE + Math.max(0, optCount - 1) * OPT_ROW_H;
}

// ─── Auto-layout (BFS tree) ────────────────────────────────
function autoLayout(nodes, rootId) {
  const visited  = new Set();
  const colCount = {};
  const queue    = [[rootId, 0]];

  while (queue.length) {
    const [id, col] = queue.shift();
    if (visited.has(id) || !nodes[id]) continue;
    visited.add(id);

    colCount[col] = colCount[col] || 0;
    nodes[id]._x  = col * (NODE_W + 100);
    nodes[id]._y  = colCount[col] * (NODE_H_BASE + 60);
    colCount[col]++;

    const n        = nodes[id];
    const children = [];
    (n.options || []).forEach(o => { if (o.targetNodeId) children.push(o.targetNodeId); });
    if (n.nextNodeId) children.push(n.nextNodeId);
    (n.actions || []).forEach(a => {
      if (a.targetNodeId) children.push(a.targetNodeId);
      // Parse legacy JUMP_IF_TAG format: "condition,targetNodeId"
      if (a.type && a.type.startsWith('JUMP_IF') && a.value && a.value.includes(',')) {
        const parts = a.value.split(',');
        const target = parts[parts.length - 1];
        if (target && nodes[target]) children.push(target);
      }
    });
    children.forEach(cid => { if (!visited.has(cid)) queue.push([cid, col + 1]); });
  }

  // Orphan nodes
  let orphanY = (Object.keys(colCount).length) * (NODE_H_BASE + 60) + 60;
  Object.keys(nodes).forEach(id => {
    if (!visited.has(id)) { nodes[id]._x = 0; nodes[id]._y = orphanY; orphanY += NODE_H_BASE + 60; }
  });
}

// ════════════════════════════════════════════════════════════
//  MAIN EXPORT
// ════════════════════════════════════════════════════════════
export function initEditor(interactionData, session, apiFetch) {
  // ── Deep clone & init state ──────────────────────────
  let data   = JSON.parse(JSON.stringify(interactionData));
  let nodes  = data.nodes || {};

  if (!Object.values(nodes).some(n => n._x !== undefined)) {
    autoLayout(nodes, data.rootNodeId);
  }

  let camera         = { x: 80, y: 80, zoom: 1 };
  let draggingNode   = null;
  let dragOffset     = { x: 0, y: 0 };
  let isPanning      = false;
  let panStart       = { x: 0, y: 0 };
  let camStart       = { x: 0, y: 0 };
  let selectedNodeId = null;
  let isDirty        = false;

  // ── Build DOM ────────────────────────────────────────
  const mount = document.getElementById('editor-mount');
  if (!mount) { console.error('[editor] #editor-mount not found'); return; }
  mount.innerHTML = '';
  mount.style.cssText = 'display:flex;flex-direction:column;height:100%;overflow:hidden;';

  // Toolbar
  const toolbar = document.createElement('div');
  toolbar.className = 'editor-toolbar';
  toolbar.innerHTML = `
    <button class="btn btn-secondary btn-sm" id="ed-btn-back">← Back</button>
    <span style="font-weight:700;font-size:13px;color:var(--accent)">🌿 ${data.id}</span>
    <div style="width:1px;height:20px;background:var(--border);margin:0 4px"></div>
    <button class="btn btn-secondary btn-sm" id="ed-btn-layout">⚡ Auto Layout</button>
    <button class="btn btn-secondary btn-sm" id="ed-btn-fit">🔍 Fit</button>
    <button class="btn btn-secondary btn-sm" id="ed-btn-add">➕ Add Node</button>
    <div style="flex:1"></div>
    <span id="ed-dirty" style="display:none;color:var(--warning);font-size:12px;margin-right:8px">● Unsaved</span>
    <button class="btn btn-primary btn-sm" id="ed-btn-save">💾 Save</button>
  `;
  mount.appendChild(toolbar);

  // Body
  const body = document.createElement('div');
  body.style.cssText = 'display:flex;flex:1;overflow:hidden;min-height:0;';
  mount.appendChild(body);

  // Canvas wrap
  const wrap = document.createElement('div');
  wrap.className = 'editor-canvas-wrap';
  body.appendChild(wrap);

  const canvas = document.createElement('canvas');
  wrap.appendChild(canvas);
  const ctx = canvas.getContext('2d');

  // Property panel
  const propPanel = document.createElement('div');
  propPanel.className = 'property-panel';
  propPanel.innerHTML = `
    <div class="prop-placeholder">
      <div style="font-size:40px;margin-bottom:12px">🖱️</div>
      <p style="color:var(--text-secondary);font-size:13px">Click a node to edit</p>
      <p style="color:var(--text-muted);font-size:11px;margin-top:6px">Right-click canvas for options</p>
    </div>`;
  body.appendChild(propPanel);

  // Context menu
  const ctxMenu = document.createElement('div');
  ctxMenu.className = 'editor-context-menu';
  ctxMenu.style.display = 'none';
  document.body.appendChild(ctxMenu);

  // ── Resize canvas ────────────────────────────────────
  const resizeCanvas = () => {
    canvas.width  = wrap.clientWidth;
    canvas.height = wrap.clientHeight;
    render();
  };
  const ro = new ResizeObserver(resizeCanvas);
  ro.observe(wrap);
  setTimeout(resizeCanvas, 0);

  // ── Coordinate helpers ───────────────────────────────
  const toWorld  = (cx, cy) => ({ x: cx * camera.zoom + camera.x, y: cy * camera.zoom + camera.y });
  const toCanvas = (wx, wy) => ({ x: (wx - camera.x) / camera.zoom, y: (wy - camera.y) / camera.zoom });

  function getNodeAt(mx, my) {
    const ids = Object.keys(nodes);
    for (let i = ids.length - 1; i >= 0; i--) {
      const n   = nodes[ids[i]];
      const pos = toWorld(n._x || 0, n._y || 0);
      const h   = nodeHeight(n) * camera.zoom;
      const w   = NODE_W * camera.zoom;
      if (mx >= pos.x && mx <= pos.x + w && my >= pos.y && my <= pos.y + h) return ids[i];
    }
    return null;
  }

  // ── Render ───────────────────────────────────────────
  function render() {
    const W = canvas.width, H = canvas.height;
    ctx.clearRect(0, 0, W, H);

    // Grid
    ctx.save();
    const gs = 40 * camera.zoom;
    const ox = ((camera.x % gs) + gs) % gs;
    const oy = ((camera.y % gs) + gs) % gs;
    ctx.strokeStyle = '#161b22';
    ctx.lineWidth   = 1;
    for (let x = ox; x < W; x += gs) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, H); ctx.stroke(); }
    for (let y = oy; y < H; y += gs) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke(); }
    ctx.restore();

    ctx.save();
    ctx.translate(camera.x, camera.y);
    ctx.scale(camera.zoom, camera.zoom);

    // Connections (drawn before nodes)
    drawAllConnections();

    // Nodes
    Object.values(nodes).forEach(drawNode);

    ctx.restore();
  }

  // ── Draw bezier connection ───────────────────────────
  function drawBezier(x1, y1, x2, y2, color, label = '') {
    const dx  = Math.abs(x2 - x1) * 0.55;
    const cpx1 = x1 + dx, cpx2 = x2 - dx;
    ctx.save();
    ctx.strokeStyle = color;
    ctx.lineWidth   = 1.8;
    ctx.shadowColor = color;
    ctx.shadowBlur  = 5;
    ctx.globalAlpha = 0.75;
    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.bezierCurveTo(cpx1, y1, cpx2, y2, x2, y2);
    ctx.stroke();

    // Arrowhead at target
    const angle = Math.atan2(y2 - (y1 * 0.1 + y2 * 0.9), x2 - (x1 * 0.1 + x2 * 0.9));
    ctx.fillStyle   = color;
    ctx.globalAlpha = 0.9;
    ctx.shadowBlur  = 0;
    ctx.beginPath();
    ctx.moveTo(x2, y2);
    ctx.lineTo(x2 - 9 * Math.cos(angle - 0.35), y2 - 9 * Math.sin(angle - 0.35));
    ctx.lineTo(x2 - 9 * Math.cos(angle + 0.35), y2 - 9 * Math.sin(angle + 0.35));
    ctx.closePath();
    ctx.fill();
    ctx.restore();
  }

  function drawAllConnections() {
    Object.values(nodes).forEach(node => {
      const nx  = node._x || 0;
      const ny  = node._y || 0;
      const nh  = nodeHeight(node);

      // options → each has its own port
      (node.options || []).forEach((opt, i) => {
        if (!opt.targetNodeId || !nodes[opt.targetNodeId]) return;
        const t  = nodes[opt.targetNodeId];
        const th = nodeHeight(t);
        const py = ny + HEADER_H + 20 + i * OPT_ROW_H + 8;
        drawBezier(nx + NODE_W, py, (t._x || 0), (t._y || 0) + th / 2, '#3fb950');
      });

      // nextNodeId
      if (node.nextNodeId && nodes[node.nextNodeId]) {
        const t  = nodes[node.nextNodeId];
        const th = nodeHeight(t);
        drawBezier(nx + NODE_W, ny + nh / 2, (t._x || 0), (t._y || 0) + th / 2, '#4facfe');
      }

      // JUMP_IF actions — support both a.targetNodeId and legacy "condition,nodeId" in value
      (node.actions || []).forEach(a => {
        if (!a.type || !a.type.startsWith('JUMP_IF')) return;
        let targetId = a.targetNodeId;
        // Legacy format: value = "condition,targetNodeId"
        if (!targetId && a.value && a.value.includes(',')) {
          const parts = a.value.split(',');
          targetId = parts[parts.length - 1];
        }
        if (targetId && nodes[targetId]) {
          const t  = nodes[targetId];
          const th = nodeHeight(t);
          drawBezier(nx + NODE_W, ny + nh / 2, (t._x || 0), (t._y || 0) + th / 2, '#f85149');
        }
      });
    });
  }

  // ── Draw single node ─────────────────────────────────
  function drawNode(node) {
    const x    = node._x || 0;
    const y    = node._y || 0;
    const h    = nodeHeight(node);
    const type = getNodeType(node);
    const cfg  = NODE_TYPES[type];
    const sel  = node.id === selectedNodeId;
    const root = node.id === data.rootNodeId;

    // Shadow / glow
    ctx.save();
    ctx.shadowColor = sel ? cfg.color : (root ? '#ffd200' : cfg.color + '55');
    ctx.shadowBlur  = sel ? 22 : 10;

    // Body
    roundRect(ctx, x, y, NODE_W, h, 10);
    ctx.fillStyle   = cfg.bg;
    ctx.fill();
    ctx.strokeStyle = sel ? cfg.color : (root ? '#ffd200' : cfg.color + '88');
    ctx.lineWidth   = sel ? 2.5 : 1.5;
    ctx.stroke();
    ctx.restore();

    // Header bar
    ctx.save();
    roundRect(ctx, x, y, NODE_W, HEADER_H, 10);
    ctx.fillStyle = cfg.color + '28';
    ctx.fill();
    ctx.restore();
    // Clip header bottom corners square
    ctx.save();
    ctx.fillStyle = cfg.color + '28';
    ctx.fillRect(x, y + HEADER_H - 8, NODE_W, 8);
    ctx.restore();

    // Type label
    ctx.fillStyle = cfg.color;
    ctx.font      = 'bold 10px Inter, sans-serif';
    ctx.fillText(cfg.label.toUpperCase(), x + 10, y + 16);

    // Root badge
    if (root) {
      ctx.fillStyle = '#ffd200';
      ctx.font      = 'bold 10px Inter, sans-serif';
      const rootW = ctx.measureText('★ ROOT').width;
      ctx.fillText('★ ROOT', x + NODE_W - rootW - 8, y + 16);
    }

    // Node ID
    ctx.fillStyle = '#8b949e';
    ctx.font      = '10px "JetBrains Mono", monospace';
    ctx.fillText(node.id.substring(0, 26), x + 10, y + HEADER_H + 14);

    // Text preview
    const plain = stripMC(node.text || '(no text)');
    ctx.fillStyle = '#c9d1d9';
    ctx.font      = '11px Inter, sans-serif';
    const preview = plain.length > 28 ? plain.substring(0, 28) + '…' : plain;
    ctx.fillText(preview, x + 10, y + HEADER_H + 28);

    // Options list
    if (node.options && node.options.length > 0) {
      node.options.forEach((opt, i) => {
        const py = y + HEADER_H + 38 + i * OPT_ROW_H;
        // Option row bg
        ctx.fillStyle = '#3fb95015';
        ctx.fillRect(x + 6, py - 12, NODE_W - 12, 18);

        ctx.fillStyle = '#3fb950';
        ctx.font      = '10px Inter, sans-serif';
        const optLabel = (opt.text || '—').substring(0, 20);
        ctx.fillText('→ ' + optLabel, x + 10, py);

        // Port dot on right edge
        ctx.beginPath();
        ctx.arc(x + NODE_W, py - 4, PORT_R, 0, Math.PI * 2);
        ctx.fillStyle   = '#3fb950';
        ctx.shadowColor = '#3fb950';
        ctx.shadowBlur  = 6;
        ctx.fill();
        ctx.shadowBlur  = 0;
      });
    } else {
      // Single right port
      ctx.beginPath();
      ctx.arc(x + NODE_W, y + h / 2, PORT_R, 0, Math.PI * 2);
      ctx.fillStyle   = cfg.color;
      ctx.shadowColor = cfg.color;
      ctx.shadowBlur  = 8;
      ctx.fill();
      ctx.shadowBlur  = 0;
    }

    // Left input port
    ctx.beginPath();
    ctx.arc(x, y + h / 2, PORT_R, 0, Math.PI * 2);
    ctx.fillStyle   = '#484f58';
    ctx.strokeStyle = '#8b949e';
    ctx.lineWidth   = 1.5;
    ctx.fill();
    ctx.stroke();

    // Reward/action badge
    if (node.giveReward || (node.commandRewards && node.commandRewards.length > 0)) {
      ctx.fillStyle = '#ffd200';
      ctx.font      = '10px Inter, sans-serif';
      ctx.fillText('⭐ reward', x + 10, y + h - 8);
    } else if (node.actions && node.actions.length > 0) {
      ctx.fillStyle = '#f7971e';
      ctx.font      = '10px Inter, sans-serif';
      ctx.fillText(`⚡ ${node.actions.length} action${node.actions.length > 1 ? 's' : ''}`, x + 10, y + h - 8);
    }
  }

  // ── roundRect helper ──────────────────────────────────
  function roundRect(ctx, x, y, w, h, r) {
    ctx.beginPath();
    if (ctx.roundRect) { ctx.roundRect(x, y, w, h, r); return; }
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.arcTo(x + w, y, x + w, y + r, r);
    ctx.lineTo(x + w, y + h - r);
    ctx.arcTo(x + w, y + h, x + w - r, y + h, r);
    ctx.lineTo(x + r, y + h);
    ctx.arcTo(x, y + h, x, y + h - r, r);
    ctx.lineTo(x, y + r);
    ctx.arcTo(x, y, x + r, y, r);
    ctx.closePath();
  }

  // ── Mouse events ──────────────────────────────────────
  canvas.addEventListener('mousedown', e => {
    hideCtxMenu();
    const r  = canvas.getBoundingClientRect();
    const mx = e.clientX - r.left, my = e.clientY - r.top;
    const id = getNodeAt(mx, my);

    if (e.button === 1 || (e.button === 0 && e.altKey)) {
      isPanning = true; panStart = { x: mx, y: my }; camStart = { ...camera };
      canvas.style.cursor = 'grabbing'; return;
    }
    if (e.button === 0) {
      if (id) {
        selectedNodeId = id;
        const wp = toWorld(nodes[id]._x || 0, nodes[id]._y || 0);
        draggingNode = id;
        dragOffset   = { x: mx - wp.x, y: my - wp.y };
        renderPropPanel(id);
      } else {
        selectedNodeId = null;
        isPanning      = true;
        panStart       = { x: mx, y: my };
        camStart       = { ...camera };
        propPanel.innerHTML = `<div class="prop-placeholder"><div style="font-size:40px;margin-bottom:12px">🖱️</div><p style="color:var(--text-secondary);font-size:13px">Click a node to edit</p></div>`;
      }
      render();
    }
  });

  canvas.addEventListener('mousemove', e => {
    const r  = canvas.getBoundingClientRect();
    const mx = e.clientX - r.left, my = e.clientY - r.top;
    if (draggingNode) {
      const cp   = toCanvas(mx - dragOffset.x, my - dragOffset.y);
      nodes[draggingNode]._x = cp.x;
      nodes[draggingNode]._y = cp.y;
      render();
    } else if (isPanning) {
      camera.x = camStart.x + (mx - panStart.x);
      camera.y = camStart.y + (my - panStart.y);
      render();
    }
    canvas.style.cursor = draggingNode ? 'grabbing' : (isPanning ? 'grabbing' : (getNodeAt(mx, my) ? 'pointer' : 'default'));
  });

  canvas.addEventListener('mouseup', () => { draggingNode = null; isPanning = false; canvas.style.cursor = 'default'; });

  canvas.addEventListener('wheel', e => {
    e.preventDefault();
    const r   = canvas.getBoundingClientRect();
    const mx  = e.clientX - r.left, my = e.clientY - r.top;
    const zf  = e.deltaY > 0 ? 0.9 : 1.1;
    const nz  = Math.min(Math.max(camera.zoom * zf, 0.15), 3);
    camera.x  = mx - (mx - camera.x) * (nz / camera.zoom);
    camera.y  = my - (my - camera.y) * (nz / camera.zoom);
    camera.zoom = nz;
    render();
  }, { passive: false });

  canvas.addEventListener('dblclick', e => {
    const r  = canvas.getBoundingClientRect();
    const id = getNodeAt(e.clientX - r.left, e.clientY - r.top);
    if (id) { selectedNodeId = id; renderPropPanel(id); render(); }
  });

  canvas.addEventListener('contextmenu', e => {
    e.preventDefault();
    const r  = canvas.getBoundingClientRect();
    const id = getNodeAt(e.clientX - r.left, e.clientY - r.top);
    showCtxMenu(e.clientX, e.clientY, id);
  });

  // ── Context menu ──────────────────────────────────────
  function showCtxMenu(cx, cy, nodeId) {
    const items = nodeId ? [
      { icon: '✏️', label: 'Edit Node',    fn: () => { selectedNodeId = nodeId; renderPropPanel(nodeId); render(); } },
      { icon: '⭐', label: 'Set as Root',  fn: () => { data.rootNodeId = nodeId; markDirty(); render(); } },
      { icon: '📋', label: 'Duplicate',    fn: () => duplicateNode(nodeId) },
      { icon: '🗑️', label: 'Delete Node',  fn: () => deleteNode(nodeId), danger: true },
    ] : [
      { icon: '💬', label: 'Add Dialogue Node',  fn: () => addNode(cx, cy, 'DIALOGUE') },
      { icon: '❓', label: 'Add Choice Node',    fn: () => addNode(cx, cy, 'CHOICE') },
      { icon: '⚡', label: 'Add Action Node',    fn: () => addNode(cx, cy, 'ACTION') },
      { icon: '⭐', label: 'Add Reward Node',    fn: () => addNode(cx, cy, 'REWARD') },
    ];

    ctxMenu.innerHTML = items.map((it, i) =>
      `<div class="ctx-item${it.danger ? ' danger' : ''}" data-i="${i}">${it.icon} ${it.label}</div>`
    ).join('');

    ctxMenu.style.cssText = `display:block;left:${cx}px;top:${cy}px;`;
    items.forEach((it, i) => ctxMenu.querySelector(`[data-i="${i}"]`).addEventListener('click', () => { it.fn(); hideCtxMenu(); }));
  }

  function hideCtxMenu() { ctxMenu.style.display = 'none'; }
  document.addEventListener('click', hideCtxMenu);

  // ── Node CRUD ─────────────────────────────────────────
  function addNode(screenX, screenY, type) {
    const r  = canvas.getBoundingClientRect();
    const cp = toCanvas(screenX - r.left, screenY - r.top);
    const id = 'node_' + Date.now();
    nodes[id] = {
      id, text: 'New node...', options: [], actions: [],
      durationSeconds: 5, delayBeforeDialogueTicks: 0, giveReward: false, _x: cp.x, _y: cp.y
    };
    if (type === 'CHOICE')  nodes[id].options = [{ text: 'Option 1', targetNodeId: '' }, { text: 'Option 2', targetNodeId: '' }];
    if (type === 'ACTION')  nodes[id].actions = [{ type: 'COMMAND', value: '' }];
    if (type === 'REWARD')  { nodes[id].giveReward = true; nodes[id].commandRewards = []; }
    selectedNodeId = id;
    markDirty(); render(); renderPropPanel(id);
  }

  function duplicateNode(id) {
    const src   = nodes[id];
    const newId = id + '_copy_' + Date.now().toString().slice(-4);
    nodes[newId] = JSON.parse(JSON.stringify(src));
    nodes[newId].id = newId;
    nodes[newId]._x = (src._x || 0) + 40;
    nodes[newId]._y = (src._y || 0) + 40;
    markDirty(); render();
  }

  function deleteNode(id) {
    if (!confirm(`Hapus node "${id}"?`)) return;
    delete nodes[id];
    Object.values(nodes).forEach(n => {
      if (n.nextNodeId === id) delete n.nextNodeId;
      if (n.options) n.options = n.options.map(o => o.targetNodeId === id ? { ...o, targetNodeId: '' } : o);
    });
    if (data.rootNodeId === id) data.rootNodeId = '';
    if (selectedNodeId === id) {
      selectedNodeId = null;
      propPanel.innerHTML = `<div class="prop-placeholder"><p style="color:var(--danger)">Node dihapus.</p></div>`;
    }
    markDirty(); render();
  }

  function markDirty() {
    isDirty = true; data.nodes = nodes;
    document.getElementById('ed-dirty').style.display = 'inline';
  }

  // ── Property Panel ────────────────────────────────────
  function renderPropPanel(nodeId) {
    const node = nodes[nodeId];
    if (!node) return;
    const type = getNodeType(node);
    const cfg  = NODE_TYPES[type];
    const nodeIds = Object.keys(nodes).sort();

    propPanel.innerHTML = `
      <div class="prop-header" style="border-left:3px solid ${cfg.color}">
        <span class="prop-type-badge" style="background:${cfg.color}22;color:${cfg.color}">${cfg.label}</span>
        <span class="prop-node-id">${node.id}</span>
      </div>

      <div class="prop-body">

        <div class="prop-section">
          <label class="prop-label">Node ID</label>
          <input class="prop-input" id="pi-id" type="text" value="${node.id}" />
        </div>

        <div class="prop-section">
          <label class="prop-label">Dialogue Text</label>
          <textarea class="prop-textarea" id="pi-text" rows="4">${escHtml(node.text || '')}</textarea>
          <div class="mc-preview" id="pi-mc-preview">${parseMCText(node.text || '')}</div>
        </div>

        <div class="prop-row-2">
          <div class="prop-section">
            <label class="prop-label">Duration (s)</label>
            <input class="prop-input" id="pi-dur" type="number" min="1" max="300" value="${node.durationSeconds || 5}" />
          </div>
          <div class="prop-section">
            <label class="prop-label">Delay (ticks)</label>
            <input class="prop-input" id="pi-delay" type="number" min="0" value="${node.delayBeforeDialogueTicks || 0}" />
          </div>
        </div>

        <div class="prop-section">
          <label class="prop-label">Next Node (fallthrough)</label>
          <select class="prop-select" id="pi-next">
            <option value="">— none (end) —</option>
            ${nodeIds.filter(nid => nid !== nodeId).map(nid => `<option value="${nid}" ${node.nextNodeId === nid ? 'selected' : ''}>${nid}</option>`).join('')}
          </select>
        </div>

        <div class="prop-row-2">
          <div class="prop-section">
            <label class="prop-checkbox">
              <input type="checkbox" id="pi-skip" ${node.skippable !== false ? 'checked' : ''} />
              <span>Skippable</span>
            </label>
          </div>
          <div class="prop-section">
            <label class="prop-checkbox">
              <input type="checkbox" id="pi-reward" ${node.giveReward ? 'checked' : ''} />
              <span>Give Reward</span>
            </label>
          </div>
        </div>

        <!-- Options -->
        <div class="prop-section">
          <div class="prop-section-header">
            <label class="prop-label">Options</label>
            <button class="btn-tiny" id="pi-add-opt">+ Add</button>
          </div>
          <div id="pi-options">
            ${(node.options || []).map((opt, i) => `
              <div class="prop-option-row" data-oi="${i}">
                <input class="prop-input opt-text" data-i="${i}" placeholder="Teks pilihan" value="${escHtml(opt.text || '')}" />
                <select class="prop-select opt-target" data-i="${i}">
                  <option value="">— pilih target —</option>
                  ${nodeIds.map(nid => `<option value="${nid}" ${opt.targetNodeId === nid ? 'selected' : ''}>${nid}</option>`).join('')}
                </select>
                <button class="btn-icon btn-del-opt" data-i="${i}" title="Hapus">✕</button>
              </div>
            `).join('') || '<div class="prop-empty">Tidak ada opsi (linear)</div>'}
          </div>
        </div>

        <!-- Actions -->
        <div class="prop-section">
          <div class="prop-section-header">
            <label class="prop-label">Actions</label>
            <button class="btn-tiny" id="pi-add-act">+ Add</button>
          </div>
          <div id="pi-actions">
            ${(node.actions || []).map((act, i) => `
              <div class="prop-action-row" data-ai="${i}">
                <select class="prop-select act-type" data-i="${i}">
                  ${['ADD_TAG','REMOVE_TAG','JUMP_IF_TAG','JUMP_IF_NOT_TAG','JUMP_IF_ITEM','JUMP_IF_FACT','JUMP_IF_NOT_FACT','SET_FACT','ADD_FACT','REMOVE_FACT','TAKE_ITEM','ITEM','COMMAND','SOUND','SCREENEFFECT','ZOOM','INVISIBLE','SUBTITLE','TITLE','PARTICLE','TELEPORT','NPC_WALK','NPC_SKIN'].map(t =>
                    `<option ${act.type === t ? 'selected' : ''}>${t}</option>`).join('')}
                </select>
                <input class="prop-input act-val" data-i="${i}" placeholder="value" value="${escHtml(act.value || '')}" />
                <button class="btn-icon btn-del-act" data-i="${i}" title="Hapus">✕</button>
              </div>
            `).join('') || '<div class="prop-empty">Tidak ada action</div>'}
          </div>
        </div>

        <!-- Command Rewards -->
        <div class="prop-section">
          <div class="prop-section-header">
            <label class="prop-label">Command Rewards</label>
            <button class="btn-tiny" id="pi-add-rew">+ Add</button>
          </div>
          <div id="pi-rewards">
            ${(node.commandRewards || []).map((cmd, i) => `
              <div class="prop-action-row">
                <input class="prop-input rew-cmd" data-i="${i}" placeholder="/command" value="${escHtml(cmd || '')}" />
                <button class="btn-icon btn-del-rew" data-i="${i}" title="Hapus">✕</button>
              </div>
            `).join('') || '<div class="prop-empty">Tidak ada command reward</div>'}
          </div>
        </div>

      </div><!-- /prop-body -->

      <div class="prop-footer">
        <button class="btn btn-primary" id="pi-apply">✓ Apply</button>
        <button class="btn btn-secondary" id="pi-root" title="Set as root node">⭐ Root</button>
        <button class="btn btn-danger-soft" id="pi-delete">🗑</button>
      </div>
    `;

    // Live MC preview
    propPanel.querySelector('#pi-text').addEventListener('input', e => {
      propPanel.querySelector('#pi-mc-preview').innerHTML = parseMCText(e.target.value);
    });

    // Add option/action/reward
    propPanel.querySelector('#pi-add-opt').addEventListener('click', () => {
      node.options = node.options || [];
      node.options.push({ text: 'New Option', targetNodeId: '' });
      markDirty(); render(); renderPropPanel(nodeId);
    });
    propPanel.querySelector('#pi-add-act').addEventListener('click', () => {
      node.actions = node.actions || [];
      node.actions.push({ type: 'COMMAND', value: '' });
      renderPropPanel(nodeId);
    });
    propPanel.querySelector('#pi-add-rew').addEventListener('click', () => {
      node.commandRewards = node.commandRewards || [];
      node.commandRewards.push('');
      renderPropPanel(nodeId);
    });

    // Delete buttons
    propPanel.querySelectorAll('.btn-del-opt').forEach(b => b.addEventListener('click', () => {
      node.options.splice(+b.dataset.i, 1); markDirty(); render(); renderPropPanel(nodeId);
    }));
    propPanel.querySelectorAll('.btn-del-act').forEach(b => b.addEventListener('click', () => {
      node.actions.splice(+b.dataset.i, 1); renderPropPanel(nodeId);
    }));
    propPanel.querySelectorAll('.btn-del-rew').forEach(b => b.addEventListener('click', () => {
      node.commandRewards.splice(+b.dataset.i, 1); renderPropPanel(nodeId);
    }));

    // Footer buttons
    propPanel.querySelector('#pi-apply').addEventListener('click',  () => applyChanges(nodeId));
    propPanel.querySelector('#pi-root').addEventListener('click',   () => { data.rootNodeId = nodeId; markDirty(); render(); toast('Root node set!', 'success'); });
    propPanel.querySelector('#pi-delete').addEventListener('click', () => deleteNode(nodeId));
  }

  function applyChanges(nodeId) {
    const n      = nodes[nodeId];
    const newId  = propPanel.querySelector('#pi-id').value.trim();

    // Rename node
    if (newId && newId !== nodeId) {
      Object.values(nodes).forEach(nd => {
        if (nd.nextNodeId === nodeId) nd.nextNodeId = newId;
        (nd.options || []).forEach(o => { if (o.targetNodeId === nodeId) o.targetNodeId = newId; });
        (nd.actions || []).forEach(a => { if (a.targetNodeId === nodeId) a.targetNodeId = newId; });
      });
      if (data.rootNodeId === nodeId) data.rootNodeId = newId;
      n.id = newId; nodes[newId] = n; delete nodes[nodeId];
      selectedNodeId = newId; nodeId = newId;
    }

    const nd         = nodes[nodeId];
    nd.text           = propPanel.querySelector('#pi-text').value;
    nd.durationSeconds = parseInt(propPanel.querySelector('#pi-dur').value)   || 5;
    nd.delayBeforeDialogueTicks = parseInt(propPanel.querySelector('#pi-delay').value) || 0;
    nd.giveReward     = propPanel.querySelector('#pi-reward').checked;
    nd.skippable      = propPanel.querySelector('#pi-skip').checked;
    nd.nextNodeId     = propPanel.querySelector('#pi-next').value || undefined;

    // Collect options
    propPanel.querySelectorAll('.opt-text').forEach((el, i) => { if (nd.options[i]) nd.options[i].text = el.value; });
    propPanel.querySelectorAll('.opt-target').forEach((el, i) => { if (nd.options[i]) nd.options[i].targetNodeId = el.value; });

    // Collect actions
    propPanel.querySelectorAll('.act-type').forEach((el, i) => { if (nd.actions[i]) nd.actions[i].type = el.value; });
    propPanel.querySelectorAll('.act-val').forEach((el, i)  => { if (nd.actions[i]) nd.actions[i].value = el.value; });
    propPanel.querySelectorAll('.act-target').forEach((el, i) => { if (nd.actions[i]) nd.actions[i].targetNodeId = el.value; });

    // Collect rewards
    const rewEls = propPanel.querySelectorAll('.rew-cmd');
    if (rewEls.length > 0) nd.commandRewards = Array.from(rewEls).map(e => e.value);

    markDirty(); render(); renderPropPanel(nodeId);
    toast('Changes applied ✓', 'success');
  }

  // ── Toolbar buttons ───────────────────────────────────
  document.getElementById('ed-btn-back').addEventListener('click', () => {
    if (isDirty && !confirm('Unsaved changes will be lost. Leave?')) return;
    ctxMenu.remove();
    if (window.__editorBack) window.__editorBack();
  });
  document.getElementById('ed-btn-layout').addEventListener('click', () => {
    autoLayout(nodes, data.rootNodeId); fitView(); markDirty();
  });
  document.getElementById('ed-btn-fit').addEventListener('click', fitView);
  document.getElementById('ed-btn-add').addEventListener('click', () => {
    addNode(canvas.width / 2, canvas.height / 2, 'DIALOGUE');
  });
  document.getElementById('ed-btn-save').addEventListener('click', saveData);

  function fitView() {
    const ns = Object.values(nodes);
    if (!ns.length) return;
    const pad  = 80;
    const minX = Math.min(...ns.map(n => n._x || 0)) - pad;
    const minY = Math.min(...ns.map(n => n._y || 0)) - pad;
    const maxX = Math.max(...ns.map(n => (n._x || 0) + NODE_W)) + pad;
    const maxY = Math.max(...ns.map(n => (n._y || 0) + nodeHeight(n))) + pad;
    camera.zoom = Math.min(canvas.width / (maxX - minX), canvas.height / (maxY - minY), 1.2);
    camera.x    = -minX * camera.zoom;
    camera.y    = -minY * camera.zoom;
    render();
  }

  async function saveData() {
    data.nodes = nodes;
    const payload = JSON.parse(JSON.stringify(data));
    try {
      await apiFetch(`/api/interaction/${data.id}`, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(payload),
      });
      isDirty = false;
      document.getElementById('ed-dirty').style.display = 'none';
      toast('Saved! 💾', 'success');
    } catch (err) {
      toast('Save failed: ' + err.message, 'error');
    }
  }

  // ── Toast ─────────────────────────────────────────────
  function toast(msg, type = 'info') {
    const el = document.createElement('div');
    el.className = `editor-toast toast-${type}`;
    el.textContent = msg;
    document.body.appendChild(el);
    requestAnimationFrame(() => el.classList.add('show'));
    setTimeout(() => { el.classList.remove('show'); setTimeout(() => el.remove(), 350); }, 2200);
  }

  // ── Keyboard shortcuts ────────────────────────────────
  document.addEventListener('keydown', e => {
    if ((e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); saveData(); }
    if (e.key === 'Delete' && selectedNodeId && document.activeElement === document.body) {
      deleteNode(selectedNodeId);
    }
    if (e.key === 'f' && document.activeElement === document.body) fitView();
  });

  // ── Utility ───────────────────────────────────────────
  function escHtml(str) {
    return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  // Init
  setTimeout(fitView, 120);
}
