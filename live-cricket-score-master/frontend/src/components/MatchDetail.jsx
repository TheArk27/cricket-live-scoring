import { useEffect, useState } from 'react';
import { api } from '../api';
import { TeamLine } from './Dashboard.jsx';

const EXTRAS = [
  { v: 'NONE', t: 'Normal' }, { v: 'WIDE', t: 'Wide' }, { v: 'NO_BALL', t: 'No-ball' },
  { v: 'BYE', t: 'Bye' }, { v: 'LEG_BYE', t: 'Leg bye' },
];
const DISMISSALS = ['CAUGHT', 'BOWLED', 'LBW', 'RUN_OUT', 'STUMPED'];

function ballClass(sym) {
  if (sym === 'W') return 'ball w';
  if (sym === '4') return 'ball four';
  if (sym === '6') return 'ball six';
  if (/[a-z]/.test(sym)) return 'ball extra';
  if (sym === '0') return 'ball dot';
  return 'ball';
}

function Scorecard({ inn }) {
  const s = inn.score;
  return (
    <div className="scorecard">
      <div className="table-wrap">
        <table>
          <thead>
            <tr><th className="left">Batter</th><th className="left hide-sm"></th><th>R</th><th>B</th><th>4s</th><th>6s</th><th>SR</th></tr>
          </thead>
          <tbody>
            {inn.batting.map((b) => (
              <tr key={b.playerId} className={b.atCrease ? 'active-row' : ''}>
                <td className="left"><b>{b.name}{b.onStrike && ' *'}</b><div className="muted small show-sm">{b.out ? b.dismissal : 'not out'}</div></td>
                <td className="left muted small hide-sm">{b.out ? b.dismissal : b.atCrease ? 'batting' : 'not out'}</td>
                <td><b>{b.runs}</b></td><td>{b.balls}</td><td>{b.fours}</td><td>{b.sixes}</td><td>{b.strikeRate.toFixed(1)}</td>
              </tr>
            ))}
            <tr className="sub"><td className="left">Extras</td><td className="left muted small hide-sm">{inn.extrasText}</td><td>{inn.extras}</td><td colSpan="4" /></tr>
            <tr className="total"><td className="left">Total</td><td className="hide-sm" /><td colSpan="5" className="left">{s.runs}/{s.wickets} ({s.overs} ov)</td></tr>
          </tbody>
        </table>
      </div>
      {inn.partnership && <p className="small">Partnership: <b>{inn.partnership}</b></p>}
      {inn.yetToBat.length > 0 && <p className="small muted">Yet to bat: {inn.yetToBat.join(', ')}</p>}
      <div className="table-wrap">
        <table>
          <thead><tr><th className="left">Bowler</th><th>O</th><th>M</th><th>R</th><th>W</th><th>Econ</th></tr></thead>
          <tbody>
            {inn.bowling.map((w) => (
              <tr key={w.playerId} className={w.bowling ? 'active-row' : ''}>
                <td className="left"><b>{w.name}{w.bowling && ' •'}</b></td>
                <td>{w.overs}</td><td>{w.maidens}</td><td>{w.runs}</td><td><b>{w.wickets}</b></td><td>{w.economy.toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function ScorerPanel({ s, onDone }) {
  const [extra, setExtra] = useState('NONE');
  const [dismissal, setDismissal] = useState('CAUGHT');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function send(runs, wicket = false) {
    setBusy(true);
    setError('');
    try {
      await api.ball(s.id, { runs, extra, wicket, dismissal: wicket ? dismissal : null });
      setExtra('NONE');
      onDone();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card scorer">
      <div className="card-head">
        <h3>Scorer console</h3>
        <label className="switch">
          <input type="checkbox" checked={s.autoPlay} onChange={(e) => api.autoplay(s.id, e.target.checked).then(onDone)} />
          Auto simulation
        </label>
      </div>
      <p className="muted small">Record the next delivery. Pick an extra type first if needed.</p>
      <div className="seg">
        {EXTRAS.map((x) => (
          <button key={x.v} className={extra === x.v ? 'active' : ''} onClick={() => setExtra(x.v)}>{x.t}</button>
        ))}
      </div>
      <div className="runs">
        {[0, 1, 2, 3, 4, 6].map((r) => (
          <button key={r} disabled={busy} onClick={() => send(r)}>{r}</button>
        ))}
      </div>
      <div className="row">
        <select value={dismissal} onChange={(e) => setDismissal(e.target.value)}>
          {DISMISSALS.map((d) => <option key={d} value={d}>{d.replace('_', ' ').toLowerCase()}</option>)}
        </select>
        <button className="danger" disabled={busy} onClick={() => send(0, true)}>Wicket</button>
      </div>
      {error && <p className="error">{error}</p>}
    </div>
  );
}

export default function MatchDetail({ id, update }) {
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState('');
  const [tab, setTab] = useState(null);

  const load = () => api.match(id).then((d) => { setDetail(d); setError(''); }).catch((e) => setError(e.message));

  useEffect(() => { load(); }, [id, update?.updatedAt]);

  if (error && !detail) return <div className="alert">{error}</div>;
  if (!detail) return <p className="muted">Loading match...</p>;

  const { summary: s, innings, recentBalls, commentary } = detail;
  const shownNo = tab ?? s.currentInnings ?? 1;
  const shown = innings.find((i) => i.score.number === shownNo) || innings[innings.length - 1];
  const current = innings.find((i) => i.score.number === s.currentInnings);

  return (
    <div className="detail">
      <a href="#/" className="back">← All matches</a>
      <div className={`card hero ${s.status.toLowerCase()}`}>
        <div className="mc-top">
          <span className={`badge ${s.status.toLowerCase()}`}>{s.status === 'LIVE' ? '● LIVE' : s.status === 'SCHEDULED' ? 'UPCOMING' : 'RESULT'}</span>
          <span className="muted small">{s.overs} overs · {s.venue}</span>
        </div>
        <TeamLine team={s.teamA} innings={s.innings} />
        <TeamLine team={s.teamB} innings={s.innings} />
        <p className="hero-status">{s.statusText}</p>
        {s.status === 'LIVE' && (
          <div className="hero-meta">
            <span>CRR <b>{s.crr ?? '-'}</b></span>
            {s.rrr != null && <span>RRR <b>{s.rrr}</b></span>}
            {current?.score.target && <span>Target <b>{current.score.target}</b></span>}
            {current?.partnership && <span>Partnership <b>{current.partnership}</b></span>}
          </div>
        )}
        {s.toss && <p className="muted small">{s.toss}</p>}
        {recentBalls.length > 0 && s.status === 'LIVE' && (
          <div className="recent">
            <span className="muted small">Recent:</span>
            {recentBalls.map((b, i) => <span key={i} className={ballClass(b)}>{b}</span>)}
          </div>
        )}
        {s.status === 'SCHEDULED' && <button className="primary" onClick={() => api.start(s.id).then(load)}>Start match now</button>}
      </div>

      <div className="detail-grid">
        <div>
          {innings.length > 0 ? (
            <div className="card">
              <div className="tabs">
                {innings.map((i) => (
                  <button key={i.score.number} className={i.score.number === shown.score.number ? 'active' : ''} onClick={() => setTab(i.score.number)}>
                    {i.score.battingTeam} innings
                  </button>
                ))}
              </div>
              <Scorecard inn={shown} />
            </div>
          ) : (
            <p className="muted">Scorecard will appear once the match starts.</p>
          )}
        </div>
        <div>
          {s.status === 'LIVE' && <ScorerPanel s={s} onDone={load} />}
          <div className="card">
            <h3>Commentary</h3>
            {commentary.length === 0 && <p className="muted">No deliveries yet.</p>}
            <ul className="commentary">
              {commentary.map((c) => (
                <li key={c.id} className={c.wicket ? 'wkt' : ''}>
                  <span className={ballClass(c.symbol)}>{c.symbol}</span>
                  <div><b>{c.over}</b> {c.commentary}</div>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
