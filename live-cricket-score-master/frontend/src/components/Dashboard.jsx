import { useEffect, useState } from 'react';
import { api } from '../api';

export function TeamLine({ team, innings }) {
  const mine = innings.filter((i) => i.battingTeamId === team.id);
  return (
    <div className="team-line">
      <span className="dot" style={{ background: team.color }} />
      <b>{team.shortName}</b>
      <span className="team-name">{team.name}</span>
      <span className="team-score">
        {mine.map((i) => (
          <span key={i.number}>{i.runs}/{i.wickets} <small>({i.overs})</small></span>
        ))}
      </span>
    </div>
  );
}

function MatchCard({ m }) {
  async function startNow(e) {
    e.preventDefault();
    await api.start(m.id).catch(() => {});
  }
  return (
    <a className={`card match-card ${m.status.toLowerCase()}`} href={`#/match/${m.id}`}>
      <div className="mc-top">
        <span className={`badge ${m.status.toLowerCase()}`}>{m.status === 'LIVE' ? '● LIVE' : m.status === 'SCHEDULED' ? 'UPCOMING' : 'RESULT'}</span>
        <span className="muted small">{m.overs} overs · {m.venue}</span>
      </div>
      <TeamLine team={m.teamA} innings={m.innings} />
      <TeamLine team={m.teamB} innings={m.innings} />
      <p className="mc-status">{m.statusText}</p>
      <div className="mc-foot">
        {m.status === 'LIVE' && (
          <span className="muted small">
            CRR {m.crr ?? '-'}{m.rrr != null && ` · RRR ${m.rrr}`}{!m.autoPlay && ' · manual scoring'}
          </span>
        )}
        {m.status === 'SCHEDULED' && <button onClick={startNow}>Start now</button>}
      </div>
    </a>
  );
}

export default function Dashboard({ updates, connected }) {
  const [matches, setMatches] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    api.matches().then((list) => { setMatches(list); setError(''); }).catch((e) => setError(e.message));
  }, [connected]);

  const byId = new Map(matches.map((m) => [m.id, m]));
  Object.values(updates).forEach((u) => byId.set(u.id, u));
  const all = [...byId.values()];
  const live = all.filter((m) => m.status === 'LIVE');
  const upcoming = all.filter((m) => m.status === 'SCHEDULED');
  const results = all.filter((m) => m.status === 'COMPLETED').sort((a, b) => b.id - a.id);

  const section = (title, list, empty) => (
    <section>
      <h2>{title} <span className="count">{list.length}</span></h2>
      {list.length ? <div className="grid">{list.map((m) => <MatchCard key={m.id} m={m} />)}</div> : <p className="muted">{empty}</p>}
    </section>
  );

  return (
    <>
      {error && <div className="alert">Could not reach the score server: {error}. Is the Spring Boot backend running on port 8080?</div>}
      <div className="stats-row">
        <div className="stat"><b>{live.length}</b><span>Live now</span></div>
        <div className="stat"><b>{upcoming.length}</b><span>Upcoming</span></div>
        <div className="stat"><b>{results.length}</b><span>Completed</span></div>
      </div>
      {section('Live matches', live, 'No match is live right now.')}
      {section('Upcoming', upcoming, 'No upcoming fixtures.')}
      {section('Results', results, 'No completed matches yet.')}
    </>
  );
}
