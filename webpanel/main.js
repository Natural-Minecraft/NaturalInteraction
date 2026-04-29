/**
 * NaturalInteraction Web Panel — Main Application
 * Token-based auth (LuckPerms style), REST API client, interaction editor.
 */

// ─── State ──────────────────────────────────────────────────────────────────

let session = null;  // { token, apiUrl, playerName, ... }
let currentPage = 'interactions';
let interactionsData = [];

// ─── Init — Token Router ────────────────────────────────────────────────────

(async function init() {
  // Extract token from URL path: /AbCdEf
  const path = window.location.pathname.replace(/^\//, '').replace(/\/$/, '');

  if (path && /^[A-Za-z]{6}$/.test(path)) {
    // We have a token — try to verify
    await verifyToken(path);
  } else {
    // No token in URL — show error
    showLandingError('Tidak ada token di URL. Ketik /ni connect di Minecraft.');
  }
})();

async function verifyToken(token) {
  showLandingStatus('Connecting to server...');

  // Try to find the API URL — we need the plugin's api-url
  // The token verify endpoint returns the apiUrl
  // But we need to know WHERE to call verify first!
  // Solution: try apiUrl from sessionStorage first, then try the public-url origin

  let apiUrl = sessionStorage.getItem('ni_apiUrl');

  // If no stored apiUrl, we need the user to have configured it
  // Try the origin + common ports, or check for a config.json
  if (!apiUrl) {
    apiUrl = await discoverApiUrl();
    if (!apiUrl) {
      showLandingError('Tidak bisa menemukan API server. Pastikan api-url dikonfigurasi di config.yml plugin.');
      return;
    }
  }

  try {
    const resp = await fetch(`${apiUrl}/api/session/verify?token=${token}`, {
      method: 'GET',
      headers: { 'Accept': 'application/json' },
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      showLandingError(err.error || 'Token invalid or expired.');
      return;
    }

    const data = await resp.json();
    if (!data.valid) {
      showLandingError('Token invalid or expired.');
      return;
    }

    // Save session
    session = {
      token: data.token,
      apiUrl: data.apiUrl || apiUrl,
      playerName: data.playerName,
      playerUUID: data.playerUUID,
      expiresIn: data.expiresIn,
    };

    sessionStorage.setItem('ni_apiUrl', session.apiUrl);
    sessionStorage.setItem('ni_token', session.token);

    // Clean URL (remove token from visible path)
    window.history.replaceState({}, '', '/');

    // Show main app
    showMainApp();
  } catch (e) {
    console.error('[Auth] Verify failed:', e);
    showLandingError('Gagal terhubung ke server. Pastikan port API terbuka dan CORS aktif.');
  }
}

async function discoverApiUrl() {
  // Try loading from a config.json file (deployed alongside the frontend)
  try {
    const resp = await fetch('/config.json');
    if (resp.ok) {
      const cfg = await resp.json();
      if (cfg.apiUrl) return cfg.apiUrl;
    }
  } catch (e) { /* ignore */ }

  // Try sessionStorage
  const stored = sessionStorage.getItem('ni_apiUrl');
  if (stored) return stored;

  return null;
}

// ─── Landing UI ─────────────────────────────────────────────────────────────

function showLandingStatus(msg) {
  document.getElementById('landing-status').style.display = 'flex';
  document.getElementById('landing-status').querySelector('span').textContent = msg;
  document.getElementById('landing-error').style.display = 'none';
}

function showLandingError(msg) {
  document.getElementById('landing-status').style.display = 'none';
  document.getElementById('landing-error').style.display = 'block';
  document.getElementById('landing-error-text').textContent = msg;
}

// ─── Main App ───────────────────────────────────────────────────────────────

async function showMainApp() {
  document.getElementById('landing').style.display = 'none';
  document.getElementById('main-app').style.display = 'flex';

  // Update session info in sidebar
  document.getElementById('session-info').textContent = session.playerName;

  // Load data
  await loadInteractions();
}

// ─── API Client ─────────────────────────────────────────────────────────────

async function apiFetch(path, options = {}) {
  const url = session.apiUrl + path;
  const headers = {
    'Authorization': `Bearer ${session.token}`,
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  const resp = await fetch(url, { ...options, headers });

  if (resp.status === 401) {
    // Token expired — show landing with error
    document.getElementById('landing').style.display = 'flex';
    document.getElementById('main-app').style.display = 'none';
    showLandingError('Session expired. Ketik /ni connect lagi di Minecraft.');
    throw new Error('Unauthorized');
  }

  return resp;
}

// ─── Load Interactions ──────────────────────────────────────────────────────

async function loadInteractions() {
  try {
    const resp = await apiFetch('/api/interactions');
    interactionsData = await resp.json();
    renderInteractions();
  } catch (e) {
    console.error('[API] Failed to load interactions:', e);
  }
}

// ─── Navigation ─────────────────────────────────────────────────────────────

document.querySelectorAll('.nav-item').forEach(btn => {
  btn.addEventListener('click', () => switchPage(btn.dataset.page));
});

function switchPage(page) {
  currentPage = page;
  document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));
  const nav = document.querySelector(`[data-page="${page}"]`);
  if (nav) nav.classList.add('active');

  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  const pg = document.getElementById(`page-${page}`);
  if (pg) pg.classList.add('active');

  const titles = {
    interactions: 'Interactions',
    chapters: 'Chapters',
    facts: 'Facts Manager',
    editor: 'Node Editor',
  };
  document.getElementById('page-title').textContent = titles[page] || page;
  document.getElementById('breadcrumb').textContent = `Dashboard / ${titles[page] || page}`;
}

// ─── Render: Interactions ───────────────────────────────────────────────────

function renderInteractions() {
  const grid = document.getElementById('interactions-grid');
  if (!grid) return;

  if (interactionsData.length === 0) {
    grid.innerHTML = `
      <div class="card card-empty">
        <div class="card-empty-icon">📝</div>
        <p>No interactions loaded</p>
        <p class="text-muted">Create a new interaction or check plugin files</p>
      </div>`;
    return;
  }

  // Sort: prologue first, then alphabetical
  const sorted = [...interactionsData].sort((a, b) => {
    if (a.isPrologue) return -1;
    if (b.isPrologue) return 1;
    return (a.id || '').localeCompare(b.id || '');
  });

  grid.innerHTML = sorted.map(i => {
    const badges = [];
    if (i.isPrologue) badges.push(`<span class="card-badge badge-prologue">🌟 Prologue</span>`);
    if (i.chapter) badges.push(`<span class="card-badge badge-chapter">${esc(i.chapter)}</span>`);
    if (i.mandatory) badges.push(`<span class="card-badge badge-mandatory">Mandatory</span>`);
    if (i.oneTimeReward) badges.push(`<span class="card-badge badge-complete">One-time</span>`);

    return `
      <div class="card" data-id="${esc(i.id)}" onclick="window.__openEditor('${esc(i.id)}')">
        <div class="card-title">${esc(i.npcDisplayName || i.id)}</div>
        <div class="card-subtitle">${esc(i.id)}</div>
        <div style="margin-bottom:10px">${badges.join(' ')}</div>
        <div class="card-meta">
          <span>📄 ${i.nodeCount || 0} nodes</span>
          <span>⏱ ${i.cooldownSeconds || 0}s</span>
        </div>
      </div>`;
  }).join('');
}

// ─── Open Editor ────────────────────────────────────────────────────────────

window.__openEditor = async function(interactionId) {
  try {
    const resp = await apiFetch(`/api/interaction/${interactionId}`);
    if (!resp.ok) {
      alert('Failed to load interaction: ' + interactionId);
      return;
    }
    const data = await resp.json();

    // Switch to editor page
    switchPage('editor');
    document.getElementById('editor-interaction-name').textContent = data.npcDisplayName || data.id || interactionId;
    document.getElementById('editor-chapter-badge').textContent = data.chapter || '';

    // Import and initialize the editor
    const { initEditor } = await import('/editor.js');
    initEditor(data, session, apiFetch);
  } catch (e) {
    console.error('[Editor] Failed to open:', e);
    alert('Error opening editor: ' + e.message);
  }
};

// ─── Search ─────────────────────────────────────────────────────────────────

document.getElementById('search-input')?.addEventListener('input', e => {
  const q = e.target.value.toLowerCase();
  document.querySelectorAll('.card[data-id]').forEach(card => {
    const id = card.dataset.id?.toLowerCase() || '';
    const title = card.querySelector('.card-title')?.textContent?.toLowerCase() || '';
    card.style.display = (id.includes(q) || title.includes(q)) ? '' : 'none';
  });
});

// ─── New Interaction ────────────────────────────────────────────────────────

document.getElementById('btn-new')?.addEventListener('click', async () => {
  const id = prompt('Enter interaction ID (lowercase, underscore):');
  if (!id || !id.trim()) return;

  try {
    const resp = await apiFetch('/api/interaction-new', {
      method: 'POST',
      body: JSON.stringify({ id: id.trim() }),
    });

    if (!resp.ok) {
      const err = await resp.json().catch(() => ({}));
      alert(err.error || 'Failed to create interaction');
      return;
    }

    await loadInteractions();
    window.__openEditor(id.trim());
  } catch (e) {
    alert('Error: ' + e.message);
  }
});

// ─── Editor Back Button ─────────────────────────────────────────────────────

document.getElementById('editor-back')?.addEventListener('click', () => {
  switchPage('interactions');
  loadInteractions(); // Refresh list
});

// ─── Facts ──────────────────────────────────────────────────────────────────

document.getElementById('btn-load-facts')?.addEventListener('click', async () => {
  const uuid = document.getElementById('facts-uuid-input')?.value?.trim();
  if (!uuid) { alert('Enter a player UUID'); return; }

  try {
    const resp = await apiFetch(`/api/facts/${uuid}`);
    const data = await resp.json();
    const result = document.getElementById('facts-result');

    if (!data.facts || Object.keys(data.facts).length === 0) {
      result.innerHTML = `<p class="text-muted">No facts found for this player.</p>`;
      return;
    }

    const entries = Object.entries(data.facts).sort((a, b) => a[0].localeCompare(b[0]));
    result.innerHTML = `
      <h4 style="margin-bottom:12px">Facts (${entries.length})</h4>
      <table class="facts-table">
        <thead><tr><th>Key</th><th>Value</th></tr></thead>
        <tbody>
          ${entries.map(([k, v]) =>
            `<tr><td class="fact-key">${esc(k)}</td><td class="fact-value">${esc(v)}</td></tr>`
          ).join('')}
        </tbody>
      </table>`;
  } catch (e) {
    document.getElementById('facts-result').innerHTML =
      `<p style="color:var(--danger)">Error: ${esc(e.message)}</p>`;
  }
});

// ─── Helpers ────────────────────────────────────────────────────────────────

function esc(str) {
  const d = document.createElement('div');
  d.textContent = str || '';
  return d.innerHTML;
}
