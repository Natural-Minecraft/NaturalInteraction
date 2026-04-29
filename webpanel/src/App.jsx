import { useState, useEffect } from 'react';
import Landing from './pages/Landing';
import Dashboard from './pages/Dashboard';
import Editor from './pages/Editor';
import { initApi, verifyToken, setSession, apiFetch, getApiUrl } from './api';

export default function App() {
  const [page, setPage] = useState('landing'); // landing | dashboard | editor
  const [session, setSessionState] = useState(null);
  const [interactions, setInteractions] = useState([]);
  const [editorData, setEditorData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  // Init: check token in URL
  useEffect(() => {
    (async () => {
      await initApi();
      const path = window.location.pathname.replace(/^\//, '').replace(/\/$/, '');
      if (path && /^[A-Za-z]{6}$/.test(path)) {
        try {
          const data = await verifyToken(path);
          const apiUrl = getApiUrl();
          setSession(apiUrl, data.token);
          setSessionState({ playerName: data.playerName, token: data.token });
          window.history.replaceState({}, '', '/');
          setPage('dashboard');
        } catch (e) {
          setError(e.message || 'Token invalid or expired.');
        }
      } else {
        setError('Tidak ada token di URL. Ketik /ni connect di Minecraft.');
      }
      setLoading(false);
    })();
  }, []);

  // Load interactions
  useEffect(() => {
    if (page === 'dashboard' && session) loadInteractions();
  }, [page, session]);

  async function loadInteractions() {
    try {
      const resp = await apiFetch('/api/interactions');
      const data = await resp.json();
      setInteractions(data);
    } catch (e) { console.error('[API]', e); }
  }

  async function openEditor(id) {
    try {
      const resp = await apiFetch(`/api/interaction/${id}`);
      if (!resp.ok) { alert('Failed to load: ' + id); return; }
      const data = await resp.json();
      setEditorData(data);
      setPage('editor');
    } catch (e) { alert('Error: ' + e.message); }
  }

  if (page === 'landing' || !session) {
    return <Landing loading={loading} error={error} />;
  }
  if (page === 'editor' && editorData) {
    return <Editor data={editorData} onBack={() => { setPage('dashboard'); loadInteractions(); }} />;
  }
  return (
    <Dashboard
      session={session}
      interactions={interactions}
      onOpenEditor={openEditor}
      onRefresh={loadInteractions}
    />
  );
}
