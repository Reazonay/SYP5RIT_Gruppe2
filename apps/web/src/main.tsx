import { useCallback, useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { api } from './api';
import Group from './pages/Group';
import Reserve from './pages/Reserve';
import Staff from './pages/Staff';
import Visit from './pages/Visit';
import './style.css';
import type { Menu } from './types';
function App() {
  const [tab, setTab] = useState('reserve'),
    [menu, setMenu] = useState<Menu[]>([]),
    [error, setError] = useState(''),
    [notice, setNotice] = useState(''),
    [busy, setBusy] = useState(false),
    [guest, setGuest] = useState(''),
    [group, setGroup] = useState('');
  const run = useCallback(async (fn: () => Promise<void>) => {
    setBusy(true);
    setError('');
    setNotice('');
    try {
      await fn();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }, []);
  useEffect(() => {
    void api<Menu[]>('menu')
      .then(setMenu)
      .catch((e) => setError(e.message));
    const apply = () => {
      const p = new URLSearchParams(location.hash.slice(1));
      if (p.get('guest')) {
        setGuest(p.get('guest')!);
        setTab('visit');
      }
      if (p.get('group')) {
        setGroup(p.get('group')!);
        setTab('group');
      }
    };
    apply();
    window.addEventListener('hashchange', apply);
    return () => window.removeEventListener('hashchange', apply);
  }, []);
  return (
    <>
      <header>
        <a href="/" className="brand">
          Restaurant
        </a>
        <nav aria-label="Hauptnavigation">
          {[
            ['reserve', 'Reservieren'],
            ['visit', 'Mein Besuch'],
            ['group', 'Gruppe'],
            ['staff', 'Personal'],
          ].map(([id, label]) => (
            <button
              aria-current={tab === id ? 'page' : undefined}
              onClick={() => setTab(id)}
              key={id}
            >
              {label}
            </button>
          ))}
        </nav>
      </header>
      <main>
        <div role="alert">{error && <p className="error">{error}</p>}</div>
        <div role="status">
          {notice && <p className="notice">{notice}</p>}
          {busy && <p>Wird verarbeitet …</p>}
        </div>
        <fieldset disabled={busy} className="app-fieldset">
          {tab === 'reserve' && (
            <Reserve
              menu={menu}
              run={run}
              done={(g: string, o: string) => {
                setGuest(g);
                setGroup(o);
                location.hash = `guest=${g}`;
                setTab('visit');
              }}
            />
          )}
          {tab === 'visit' && (
            <Visit menu={menu} token={guest} setToken={setGuest} run={run} notify={setNotice} />
          )}
          {tab === 'group' && <Group token={group} setToken={setGroup} run={run} />}
          {tab === 'staff' && (
            <Staff
              menu={menu}
              refreshMenu={() => api<Menu[]>('menu').then(setMenu)}
              run={run}
              notify={setNotice}
            />
          )}
        </fieldset>
      </main>
      <footer>Reservierungen · Zeiten in Ihrer lokalen Zeitzone · Zahlungen im Testbetrieb</footer>
    </>
  );
}
createRoot(document.getElementById('root')!).render(<App />);
