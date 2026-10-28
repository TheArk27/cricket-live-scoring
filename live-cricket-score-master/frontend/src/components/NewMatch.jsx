import { useEffect, useState } from 'react';
import { api } from '../api';

export default function NewMatch() {
  const [teams, setTeams] = useState([]);
  const [form, setForm] = useState({ teamAId: '', teamBId: '', overs: 10, venue: '', startNow: true, autoPlay: true });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.teams().then((t) => {
      setTeams(t);
      if (t.length >= 2) setForm((f) => ({ ...f, teamAId: t[0].id, teamBId: t[1].id }));
    }).catch((e) => setError(e.message));
  }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  async function submit(e) {
    e.preventDefault();
    if (String(form.teamAId) === String(form.teamBId)) return setError('Pick two different teams.');
    setBusy(true);
    setError('');
    try {
      const m = await api.create({ ...form, teamAId: Number(form.teamAId), teamBId: Number(form.teamBId), overs: Number(form.overs) });
      window.location.hash = `#/match/${m.id}`;
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="card form" onSubmit={submit}>
      <h2>Create a match</h2>
      <div className="form-grid">
        <label>Team A
          <select value={form.teamAId} onChange={set('teamAId')}>{teams.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}</select>
        </label>
        <label>Team B
          <select value={form.teamBId} onChange={set('teamBId')}>{teams.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}</select>
        </label>
        <label>Overs per side
          <input type="number" min="1" max="50" value={form.overs} onChange={set('overs')} required />
        </label>
        <label>Venue
          <input value={form.venue} placeholder="random if empty" onChange={set('venue')} />
        </label>
      </div>
      <label className="check"><input type="checkbox" checked={form.startNow} onChange={set('startNow')} /> Start immediately (otherwise in 2 minutes)</label>
      <label className="check"><input type="checkbox" checked={form.autoPlay} onChange={set('autoPlay')} /> Auto simulation (untick to score every ball yourself)</label>
      {error && <p className="error">{error}</p>}
      <button className="primary" disabled={busy}>{busy ? 'Creating...' : 'Create match'}</button>
    </form>
  );
}
