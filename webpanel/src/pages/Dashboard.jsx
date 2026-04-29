import { useState } from 'react';
import { apiFetch } from '../api';

export default function Dashboard({ session, interactions, onOpenEditor, onRefresh }) {
  const [search, setSearch] = useState('');
  const [activePage, setActivePage] = useState('interactions');

  const filtered = interactions
    .filter(i => {
      const q = search.toLowerCase();
      return !q || (i.id || '').toLowerCase().includes(q) || (i.npcDisplayName || '').toLowerCase().includes(q);
    })
    .sort((a, b) => {
      if (a.isPrologue) return -1;
      if (b.isPrologue) return 1;
      return (a.id || '').localeCompare(b.id || '');
    });

  async function createNew() {
    const id = prompt('Enter interaction ID (lowercase, underscore):');
    if (!id?.trim()) return;
    try {
      const resp = await apiFetch('/api/interaction-new', {
        method: 'POST', body: JSON.stringify({ id: id.trim() }),
      });
      if (!resp.ok) { const e = await resp.json().catch(() => ({})); alert(e.error || 'Failed'); return; }
      onRefresh();
      onOpenEditor(id.trim());
    } catch (e) { alert(e.message); }
  }

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="sidebar-header">
          <div className="logo">
            <span className="logo-icon">✦</span>
            <h1>NaturalInteraction</h1>
          </div>
          <span className="version-badge">v2.0.0</span>
        </div>
        <nav className="sidebar-nav">
          <button className={`nav-item ${activePage === 'interactions' ? 'active' : ''}`} onClick={() => setActivePage('interactions')}>
            <span className="nav-icon">💬</span><span>Interactions</span>
          </button>
          <button className={`nav-item ${activePage === 'facts' ? 'active' : ''}`} onClick={() => setActivePage('facts')}>
            <span className="nav-icon">🧠</span><span>Facts</span>
          </button>
        </nav>
        <div className="sidebar-footer">
          <div className="connection-status">
            <span className="status-dot connected" />
            <span>{session.playerName}</span>
          </div>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div className="topbar-left">
            <h2 className="page-title">{activePage === 'interactions' ? 'Interactions' : 'Facts Manager'}</h2>
          </div>
          <div className="topbar-right">
            <input className="search-input" placeholder="Search..." value={search} onChange={e => setSearch(e.target.value)} />
            <button className="btn btn-primary" onClick={createNew}>+ New</button>
          </div>
        </header>

        {activePage === 'interactions' ? (
          <div className="page-body">
            <div className="page-header">
              <h3>All Interactions</h3>
              <p className="text-muted">Manage dialogue trees, branching stories, and NPC interactions</p>
            </div>
            <div className="card-grid">
              {filtered.length === 0 ? (
                <div className="card card-empty">
                  <div style={{ fontSize: 48, marginBottom: 12 }}>📝</div>
                  <p>No interactions found</p>
                </div>
              ) : filtered.map(i => (
                <InteractionCard key={i.id} data={i} onClick={() => onOpenEditor(i.id)} />
              ))}
            </div>
          </div>
        ) : (
          <FactsPage />
        )}
      </main>
    </div>
  );
}

function InteractionCard({ data, onClick }) {
  const chapter = (data.chapter || '').replace(/^\.+/, '').trim();
  return (
    <div className="card" onClick={onClick}>
      <div className="card-title">{data.npcDisplayName || data.id}</div>
      <div className="card-subtitle">{data.id}</div>
      <div className="card-badges">
        {data.isPrologue && <span className="badge badge-prologue">🌟 Prologue</span>}
        {chapter && <span className="badge badge-chapter">📖 {chapter}</span>}
        {data.mandatory && <span className="badge badge-mandatory">⚠ Mandatory</span>}
        {data.oneTimeReward && <span className="badge badge-complete">⭐ One-time</span>}
      </div>
      <div className="card-meta">
        <span>📄 {data.nodeCount || 0} nodes</span>
        <span>⏱ {data.cooldownSeconds || 0}s</span>
      </div>
    </div>
  );
}

function FactsPage() {
  const [uuid, setUuid] = useState('');
  const [facts, setFacts] = useState(null);

  async function loadFacts() {
    if (!uuid.trim()) return;
    try {
      const resp = await apiFetch(`/api/facts/${uuid.trim()}`);
      const data = await resp.json();
      setFacts(data.facts || {});
    } catch (e) { alert(e.message); }
  }

  return (
    <div className="page-body">
      <div className="page-header">
        <h3>Facts Manager</h3>
        <p className="text-muted">Browse player facts, tags, and completion state</p>
      </div>
      <div className="facts-search">
        <input className="prop-input" placeholder="Player UUID..." value={uuid} onChange={e => setUuid(e.target.value)} style={{ maxWidth: 400 }} />
        <button className="btn btn-primary" onClick={loadFacts}>Load</button>
      </div>
      {facts && (
        <table className="facts-table">
          <thead><tr><th>Key</th><th>Value</th></tr></thead>
          <tbody>
            {Object.entries(facts).sort((a, b) => a[0].localeCompare(b[0])).map(([k, v]) => (
              <tr key={k}><td className="fact-key">{k}</td><td>{String(v)}</td></tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
