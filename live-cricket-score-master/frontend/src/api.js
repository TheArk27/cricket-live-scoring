async function request(path, options = {}) {
  const res = await fetch(`/api${path}`, { headers: { 'Content-Type': 'application/json' }, ...options });
  const body = await res.json().catch(() => null);
  if (!res.ok) throw new Error(body?.error || `Request failed (${res.status})`);
  return body;
}

const post = (path, data) => request(path, { method: 'POST', body: data ? JSON.stringify(data) : undefined });

export const api = {
  matches: () => request('/matches'),
  match: (id) => request(`/matches/${id}`),
  create: (data) => post('/matches', data),
  start: (id) => post(`/matches/${id}/start`),
  ball: (id, data) => post(`/matches/${id}/balls`, data),
  autoplay: (id, enabled) => request(`/matches/${id}/autoplay?enabled=${enabled}`, { method: 'PUT' }),
  teams: () => request('/teams'),
  stats: (sort) => request(`/players/stats?sort=${sort}&limit=25`),
};
