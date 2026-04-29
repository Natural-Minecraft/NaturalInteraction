let apiUrl = '';
let token = '';

export async function initApi() {
  try {
    const resp = await fetch('/config.json');
    if (resp.ok) { const cfg = await resp.json(); if (cfg.apiUrl) apiUrl = cfg.apiUrl; }
  } catch (e) { /* ignore */ }
  const stored = sessionStorage.getItem('ni_apiUrl');
  if (stored && !apiUrl) apiUrl = stored;
  const storedToken = sessionStorage.getItem('ni_token');
  if (storedToken) token = storedToken;
}

export function setSession(url, t) {
  apiUrl = url;
  token = t;
  sessionStorage.setItem('ni_apiUrl', url);
  sessionStorage.setItem('ni_token', t);
}

export function getApiUrl() { return apiUrl; }
export function getToken() { return token; }

export async function verifyToken(t) {
  if (!apiUrl) await initApi();
  const resp = await fetch(`${apiUrl}/api/session/verify?token=${t}`);
  if (!resp.ok) throw new Error('Token invalid');
  const data = await resp.json();
  if (!data.valid) throw new Error('Token expired');
  return data;
}

export async function apiFetch(path, options = {}) {
  const url = apiUrl + path;
  const headers = {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };
  const resp = await fetch(url, { ...options, headers });
  if (resp.status === 401) throw new Error('Session expired');
  return resp;
}
