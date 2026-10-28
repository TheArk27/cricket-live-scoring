import { useEffect, useState } from 'react';
import Dashboard from './components/Dashboard.jsx';
import MatchDetail from './components/MatchDetail.jsx';
import PlayerStats from './components/PlayerStats.jsx';
import NewMatch from './components/NewMatch.jsx';

// Live feed: one Server-Sent Events connection shared by every page.
function useLiveFeed() {
  const [connected, setConnected] = useState(false);
  const [updates, setUpdates] = useState({});
  useEffect(() => {
    const es = new EventSource('/api/stream');
    es.onopen = () => setConnected(true);
    es.onerror = () => setConnected(false);
    es.addEventListener('match', (e) => {
      const s = JSON.parse(e.data);
      setUpdates((u) => ({ ...u, [s.id]: s }));
    });
    return () => es.close();
  }, []);
  return { connected, updates };
}

function useHashRoute() {
  const [hash, setHash] = useState(window.location.hash);
  useEffect(() => {
    const on = () => {
      setHash(window.location.hash);
      window.scrollTo(0, 0);
    };
    window.addEventListener('hashchange', on);
    return () => window.removeEventListener('hashchange', on);
  }, []);
  return hash.replace(/^#\/?/, '').split('/');
}

export default function App() {
  const { connected, updates } = useLiveFeed();
  const [page, param] = useHashRoute();

  let content;
  if (page === 'match' && param) content = <MatchDetail id={Number(param)} update={updates[param]} />;
  else if (page === 'players') content = <PlayerStats updates={updates} />;
  else if (page === 'new') content = <NewMatch />;
  else content = <Dashboard updates={updates} connected={connected} />;

  const nav = (href, label, active) => (
    <a href={href} className={active ? 'active' : ''}>{label}</a>
  );

  return (
    <div className="app">
      <header className="topbar">
        <a className="brand" href="#/">🏏 LiveCricket</a>
        <nav>
          {nav('#/', 'Matches', !page || page === 'match')}
          {nav('#/players', 'Player Stats', page === 'players')}
          {nav('#/new', 'New Match', page === 'new')}
        </nav>
        <span className={`conn ${connected ? 'on' : 'off'}`}>{connected ? 'Live feed connected' : 'Reconnecting...'}</span>
      </header>
      <main className="container">{content}</main>
    </div>
  );
}
