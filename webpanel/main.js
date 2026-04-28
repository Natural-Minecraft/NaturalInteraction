/**
 * NaturalInteraction Web Panel — Main Application
 * Handles navigation, WebSocket connection, and page rendering.
 */

// ─── Configuration ────────────────────────────────────────────────────────────

const API_BASE = 'http://furina.nexuscloud.id:25809';
const WS_BASE = 'ws://furina.nexuscloud.id:25809/ws';

// ─── State ──────────────────────────────────────────────────────────────────

let ws = null;
let currentPage = 'interactions';
let interactionsData = [];
let isConnected = false;
let authToken = localStorage.getItem('ni_auth_token') || '';

// ─── Magic Link Auth ────────────────────────────────────────────────────────

const hash = window.location.hash;
if (hash && hash.length > 1) {
  const possibleToken = hash.substring(1);
  if (possibleToken.length === 6) { // Our short tokens are 6 chars
    authToken = possibleToken;
    localStorage.setItem('ni_auth_token', authToken);
    // Remove hash from URL without reloading
    window.history.replaceState(null, '', window.location.pathname + window.location.search);
    console.log('[Auth] Saved magic link token.');
  }
}

// ─── Navigation ─────────────────────────────────────────────────────────────

document.querySelectorAll('.nav-item').forEach((btn) => {
  btn.addEventListener('click', () => {
    const page = btn.dataset.page;
    switchPage(page);
  });
});

function switchPage(page) {
  currentPage = page;

  // Update nav active state
  document.querySelectorAll('.nav-item').forEach((b) => b.classList.remove('active'));
  const activeNav = document.querySelector(`[data-page="${page}"]`);
  if (activeNav) activeNav.classList.add('active');

  // Show/hide pages
  document.querySelectorAll('.page').forEach((p) => p.classList.remove('active'));
  const activePage = document.getElementById(`page-${page}`);
  if (activePage) activePage.classList.add('active');

  // Update topbar
  const titles = {
    interactions: 'Interactions',
    chapters: 'Chapters',
    facts: 'Facts Manager',
    manifests: 'Manifests',
    npcs: 'NPCs',
    cinematics: 'Cinematics',
    settings: 'Settings',
  };
  document.getElementById('page-title').textContent = titles[page] || page;
  document.getElementById('breadcrumb').textContent = `Dashboard / ${titles[page] || page}`;
}

// ─── WebSocket Connection ───────────────────────────────────────────────────

document.getElementById('btn-connect')?.addEventListener('click', connect);

function connect() {
  const addr = WS_BASE;
  
  if (!authToken) {
    console.warn('[WS] No auth token found. Run /ni connect in-game to get a link.');
    document.getElementById('page-title').textContent = '⚠️ Not Authenticated';
    document.getElementById('breadcrumb').innerHTML = 'Run <code style="color:#00f2fe">/ni connect</code> in Minecraft to login.';
    return;
  }

  if (ws) ws.close();

  try {
    ws = new WebSocket(addr + "?token=" + authToken);

    ws.onopen = () => {
      setConnectionStatus(true);
      // Send auth frame first
      ws.send(JSON.stringify({ type: 'auth', token: authToken }));
      
      // Then request data
      setTimeout(() => {
        sendMessage({ type: 'list_interactions' });
        sendMessage({ type: 'list_chapters' });
      }, 200);
      console.log('[WS] Connected to', addr);
    };

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);
        handleMessage(msg);
      } catch (e) {
        console.warn('[WS] Invalid message:', event.data);
      }
    };

    ws.onclose = () => {
      setConnectionStatus(false);
      console.log('[WS] Disconnected');
      // Auto-reconnect after 5s
      setTimeout(() => {
        if (!isConnected) connect();
      }, 5000);
    };

    ws.onerror = (err) => {
      console.error('[WS] Error:', err);
      setConnectionStatus(false);
    };
  } catch (e) {
    console.error('[WS] Connection failed:', e);
  }
}

function sendMessage(msg) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(msg));
  }
}

function setConnectionStatus(connected) {
  isConnected = connected;
  const el = document.getElementById('connection-status');
  const dot = el?.querySelector('.status-dot');
  const label = el?.querySelector('span:last-child');

  if (dot) {
    dot.classList.toggle('connected', connected);
    dot.classList.toggle('disconnected', !connected);
  }
  if (label) label.textContent = connected ? 'Connected' : 'Disconnected';
}

// ─── Message Handling ───────────────────────────────────────────────────────

function handleMessage(msg) {
  switch (msg.type) {
    case 'interactions_list':
      interactionsData = msg.data || [];
      renderInteractions();
      break;
    case 'chapters_tree':
      renderChapters(msg.data || {});
      break;
    case 'facts_list':
      renderFacts(msg.data || {});
      break;
    case 'interaction_saved':
      sendMessage({ type: 'list_interactions' });
      break;
    case 'error':
      console.error('[Server]', msg.message);
      break;
    default:
      console.log('[WS] Unknown message type:', msg.type);
  }
}

// ─── Render: Interactions ───────────────────────────────────────────────────

function renderInteractions() {
  const grid = document.getElementById('interactions-grid');
  if (!grid) return;

  if (interactionsData.length === 0) {
    grid.innerHTML = `
      <div class="card card-empty" id="card-empty">
        <div class="card-empty-icon">📝</div>
        <p>No interactions loaded</p>
        <p class="text-muted">Connect to the server or create a new interaction</p>
      </div>`;
    return;
  }

  grid.innerHTML = interactionsData
    .map((i) => {
      const nodeCount = Object.keys(i.nodes || {}).length;
      const chapter = i.chapter || 'Uncategorized';
      const badges = [];
      if (i.chapter) badges.push(`<span class="card-badge badge-chapter">${i.chapter}</span>`);
      if (i.mandatory) badges.push(`<span class="card-badge badge-mandatory">Mandatory</span>`);

      return `
        <div class="card" data-id="${i.id}">
          <div class="card-title">${escapeHtml(i.npcDisplayName || i.id)}</div>
          <div class="card-subtitle">${escapeHtml(i.id)}</div>
          <div style="margin-bottom: 10px;">${badges.join(' ')}</div>
          <div class="card-meta">
            <span>📄 ${nodeCount} nodes</span>
            <span>⏱ ${i.cooldownSeconds || 0}s cooldown</span>
            ${i.oneTimeReward ? '<span>🎁 One-time</span>' : ''}
          </div>
          <div style="margin-top: 12px; display: flex; gap: 8px;">
            <button class="btn btn-secondary btn-edit-json" data-id="${escapeHtml(i.id)}" style="flex:1; padding: 4px 8px; justify-content:center;">Edit JSON</button>
          </div>
        </div>`;
    })
    .join('');

  // Attach edit listeners
  document.querySelectorAll('.btn-edit-json').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      openEditor(btn.dataset.id);
    });
  });
}

// ─── JSON Editor ────────────────────────────────────────────────────────────

const editorModal = document.getElementById('editor-modal');
const editorTextarea = document.getElementById('editor-textarea');
const editorTitle = document.getElementById('editor-title');
const editorStatus = document.getElementById('editor-status');
let currentEditingId = null;

document.getElementById('btn-close-modal')?.addEventListener('click', closeEditor);
document.getElementById('btn-cancel-modal')?.addEventListener('click', closeEditor);
document.getElementById('btn-save-modal')?.addEventListener('click', saveEditor);

async function openEditor(id) {
  currentEditingId = id;
  editorTitle.textContent = \`Edit Interaction: \${id}\`;
  editorTextarea.value = 'Loading...';
  editorStatus.textContent = '';
  editorStatus.className = '';
  editorModal.classList.add('active');

  try {
    const res = await fetch(\`\${API_BASE}/api/interactions/file/\${id}\`, {
      headers: { 'Authorization': \`Bearer \${authToken}\` }
    });
    if (!res.ok) throw new Error(await res.text());
    const json = await res.json();
    // Assuming backend sends raw JSON string, if it sends object we stringify
    editorTextarea.value = typeof json === 'string' ? json : JSON.stringify(json, null, 2);
  } catch (err) {
    editorTextarea.value = '';
    editorStatus.textContent = 'Error loading file: ' + err.message;
    editorStatus.className = 'error';
  }
}

function closeEditor() {
  editorModal.classList.remove('active');
  currentEditingId = null;
}

async function saveEditor() {
  if (!currentEditingId) return;
  const content = editorTextarea.value;
  editorStatus.textContent = 'Saving...';
  editorStatus.className = '';

  try {
    // Validate JSON locally first
    JSON.parse(content);
  } catch (e) {
    editorStatus.textContent = 'Invalid JSON format!';
    editorStatus.className = 'error';
    return;
  }

  try {
    const res = await fetch(\`\${API_BASE}/api/interactions/file/\${currentEditingId}\`, {
      method: 'POST',
      headers: {
        'Authorization': \`Bearer \${authToken}\`,
        'Content-Type': 'application/json'
      },
      body: content
    });
    if (!res.ok) throw new Error(await res.text());
    
    editorStatus.textContent = 'Saved and reloaded successfully!';
    editorStatus.className = 'success';
    // Let the websocket updates refresh the list in the background
  } catch (err) {
    editorStatus.textContent = 'Error saving: ' + err.message;
    editorStatus.className = 'error';
  }
}

// ─── Render: Chapters ───────────────────────────────────────────────────────

function renderChapters(tree) {
  const container = document.getElementById('chapter-tree');
  if (!container) return;

  if (Object.keys(tree).length === 0) {
    container.innerHTML = '<div class="card card-empty"><p class="text-muted">No chapters found</p></div>';
    return;
  }

  container.innerHTML = buildTreeHtml(tree, 0);
}

function buildTreeHtml(node, depth) {
  let html = '';
  for (const [key, value] of Object.entries(node)) {
    const indent = depth * 16;
    if (typeof value === 'object' && !Array.isArray(value)) {
      html += `<div class="tree-node" style="margin-left:${indent}px">
                 <span class="tree-node-label">📁 ${escapeHtml(key)}</span>
               </div>`;
      html += buildTreeHtml(value, depth + 1);
    } else {
      html += `<div class="tree-node" style="margin-left:${indent}px">
                 <span class="tree-node-label">📄 ${escapeHtml(key)}</span>
               </div>`;
    }
  }
  return html;
}

// ─── Render: Facts ──────────────────────────────────────────────────────────

function renderFacts(data) {
  const panel = document.getElementById('facts-panel');
  if (!panel) return;

  const { playerName, facts } = data;
  const entries = Object.entries(facts || {});

  if (entries.length === 0) {
    panel.innerHTML = `<p class="text-muted">No facts for ${escapeHtml(playerName || 'player')}</p>`;
    return;
  }

  panel.innerHTML = `
    <h4 style="margin-bottom:12px">Facts for ${escapeHtml(playerName)}</h4>
    <table class="facts-table">
      <thead><tr><th>Key</th><th>Value</th></tr></thead>
      <tbody>
        ${entries.sort((a, b) => a[0].localeCompare(b[0]))
          .map(([k, v]) => `<tr><td class="fact-key">${escapeHtml(k)}</td><td class="fact-value">${escapeHtml(v)}</td></tr>`)
          .join('')}
      </tbody>
    </table>`;
}

// ─── Search ─────────────────────────────────────────────────────────────────

document.getElementById('search-input')?.addEventListener('input', (e) => {
  const query = e.target.value.toLowerCase();
  document.querySelectorAll('.card[data-id]').forEach((card) => {
    const id = card.dataset.id?.toLowerCase() || '';
    const title = card.querySelector('.card-title')?.textContent?.toLowerCase() || '';
    card.style.display = id.includes(query) || title.includes(query) ? '' : 'none';
  });
});

// ─── New Button ─────────────────────────────────────────────────────────────

document.getElementById('btn-new')?.addEventListener('click', () => {
  const id = prompt('Enter new interaction ID (e.g. main_quest_1):');
  if (id && id.trim()) {
    const defaultJson = {
      id: id.trim(),
      chapter: "uncategorized",
      npcDisplayName: "New NPC",
      rootNodeId: "start",
      nodes: {
        start: {
          type: "TEXT",
          lines: ["Hello there!"],
          nextNodeId: null
        }
      }
    };
    
    // Auto-open editor with boilerplate
    currentEditingId = id.trim();
    editorTitle.textContent = `New Interaction: ${currentEditingId}`;
    editorTextarea.value = JSON.stringify(defaultJson, null, 2);
    editorStatus.textContent = 'Unsaved new interaction';
    editorStatus.className = '';
    editorModal.classList.add('active');
  }
});

// ─── Helpers ────────────────────────────────────────────────────────────────

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str || '';
  return div.innerHTML;
}

// ─── Auto-Connect ───────────────────────────────────────────────────────────

// Try to connect automatically on page load
setTimeout(() => {
  if (!isConnected) connect();
}, 1000);
