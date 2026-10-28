import { useEffect, useState } from 'react';
import { api } from '../api';

const SORTS = [
  { v: 'runs', t: 'Most runs' }, { v: 'wickets', t: 'Most wickets' }, { v: 'strikeRate', t: 'Best strike rate' },
  { v: 'economy', t: 'Best economy' }, { v: 'sixes', t: 'Most sixes' },
];

export default function PlayerStats({ updates }) {
  const [sort, setSort] = useState('runs');
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const tick = Object.keys(updates).length;

  useEffect(() => {
    const load = () => api.stats(sort).then((r) => { setRows(r); setError(''); }).catch((e) => setError(e.message));
    load();
    const t = setInterval(load, 5000);
    return () => clearInterval(t);
  }, [sort, tick]);

  return (
    <>
      <div className="page-head">
        <h2>Player statistics</h2>
        <div className="seg">
          {SORTS.map((s) => (
            <button key={s.v} className={sort === s.v ? 'active' : ''} onClick={() => setSort(s.v)}>{s.t}</button>
          ))}
        </div>
      </div>
      <p className="muted small">Career totals across every match on the platform, updated live.</p>
      {error && <div className="alert">{error}</div>}
      <div className="card table-wrap">
        <table>
          <thead>
            <tr>
              <th>#</th><th className="left">Player</th><th>Team</th><th>M</th><th>Runs</th><th>HS</th><th>Avg</th><th>SR</th>
              <th>4s</th><th>6s</th><th>Wkts</th><th>Overs</th><th>Econ</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((p, i) => (
              <tr key={p.playerId}>
                <td>{i + 1}</td>
                <td className="left"><b>{p.name}</b><div className="muted small">{p.role.replace('_', ' ').toLowerCase()}</div></td>
                <td><span className="dot" style={{ background: p.teamColor }} /> {p.team}</td>
                <td>{p.matches}</td>
                <td className={sort === 'runs' ? 'hl' : ''}>{p.runs}</td>
                <td>{p.highest}</td>
                <td>{p.average.toFixed(1)}</td>
                <td className={sort === 'strikeRate' ? 'hl' : ''}>{p.strikeRate.toFixed(1)}</td>
                <td>{p.fours}</td>
                <td className={sort === 'sixes' ? 'hl' : ''}>{p.sixes}</td>
                <td className={sort === 'wickets' ? 'hl' : ''}>{p.wickets}</td>
                <td>{p.overs}</td>
                <td className={sort === 'economy' ? 'hl' : ''}>{p.economy.toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
