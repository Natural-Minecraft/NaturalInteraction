import { useState, useMemo } from 'react';
import { apiFetch } from '../api';

export default function Dashboard({ session, interactions, onOpenEditor, onRefresh }) {
  const [search, setSearch] = useState('');
  const [activePage, setActivePage] = useState('interactions');
  const [activeTab, setActiveTab] = useState('ALL');

  const filtered = useMemo(() => {
    return interactions
      .filter(i => {
        if (activeTab !== 'ALL' && i.storyType !== activeTab && !(activeTab === 'MAIN' && !i.storyType)) return false;
        const q = search.toLowerCase();
        return !q || (i.id || '').toLowerCase().includes(q) || (i.npcDisplayName || '').toLowerCase().includes(q) || (i.chapter || '').toLowerCase().includes(q);
      })
      .sort((a, b) => (a.id || '').localeCompare(b.id || ''));
  }, [interactions, search]);

  const grouped = useMemo(() => {
    const groups = {};
    filtered.forEach(i => {
      let cat = i.chapter ? i.chapter.trim() : '';
      if (i.isPrologue) cat = '🌟 Prologue';
      else if (!cat) {
        if (i.storyType === 'SIDE') cat = '📜 Side Quests';
        else if (i.storyType === 'FREE') cat = '🧭 Eksplorasi Bebas';
        else cat = '📁 Tanpa Kategori (Main)';
      } else {
        cat = '📖 ' + cat;
      }
      if (!groups[cat]) groups[cat] = [];
      groups[cat].push(i);
    });
    // Sort keys logically
    return Object.keys(groups).sort((a, b) => {
      if (a.includes('Prologue')) return -1;
      if (b.includes('Prologue')) return 1;
      return a.localeCompare(b);
    }).map(k => ({ title: k, items: groups[k] }));
  }, [filtered]);

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
            <div className="page-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
              <div>
                <h3>All Interactions</h3>
                <p className="text-muted">Manage dialogue trees, branching stories, and NPC interactions</p>
              </div>
              <div className="dashboard-tabs" style={{ display: 'flex', gap: 8 }}>
                <button className={`btn btn-sm ${activeTab === 'ALL' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setActiveTab('ALL')}>Semua</button>
                <button className={`btn btn-sm ${activeTab === 'MAIN' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setActiveTab('MAIN')}>Main Story</button>
                <button className={`btn btn-sm ${activeTab === 'SIDE' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setActiveTab('SIDE')}>Side Quest</button>
                <button className={`btn btn-sm ${activeTab === 'FREE' ? 'btn-primary' : 'btn-secondary'}`} onClick={() => setActiveTab('FREE')}>Bebas</button>
              </div>
            </div>
            <div className="dashboard-groups">
              {filtered.length === 0 ? (
                <div className="card card-empty">
                  <div style={{ fontSize: 48, marginBottom: 12 }}>📝</div>
                  <p>Tidak ada interaksi ditemukan</p>
                </div>
              ) : grouped.map(g => (
                <div key={g.title} className="dashboard-group">
                  <h4 className="group-title">{g.title} <span className="group-count">{g.items.length}</span></h4>
                  <div className="card-grid">
                    {g.items.map(i => <InteractionCard key={i.id} data={i} onClick={() => onOpenEditor(i.id)} />)}
                  </div>
                </div>
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
  const typeMap = { MAIN: 'Utama', SIDE: 'Side Quest', FREE: 'Bebas' };
  return (
    <div className="card" onClick={onClick}>
      <div className="card-title">{data.npcDisplayName || data.id}</div>
      <div className="card-subtitle">{data.id}</div>
      <div className="card-badges">
        {data.storyType && <span className="badge badge-chapter">{typeMap[data.storyType] || data.storyType}</span>}
        {data.mandatory && <span className="badge badge-mandatory">⚠ Wajib</span>}
        {data.oneTimeReward && <span className="badge badge-complete">⭐ 1x Reward</span>}
      </div>
      <div className="card-meta">
        <span>📄 {data.nodeCount || Object.keys(data.nodes || {}).length || 0} node</span>
        <span>⏱ {data.cooldownSeconds || 0}s cooldown</span>
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
