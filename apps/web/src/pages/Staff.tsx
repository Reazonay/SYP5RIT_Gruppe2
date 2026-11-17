import { useEffect, useState } from 'react';
import { api, date, status } from '../api';
import Management from '../components/Management';
import Scanner from '../components/Scanner';
import type { KitchenOrder, Menu, Run, Seat, StaffRole, VisitSummary } from '../types';
import Visit from './Visit';
export default function Staff({
  menu,
  refreshMenu,
  run,
  notify,
}: {
  menu: Menu[];
  refreshMenu: () => Promise<void>;
  run: Run;
  notify: (s: string) => void;
}) {
  const [role, setRole] = useState(''),
    [email, setEmail] = useState(''),
    [password, setPassword] = useState(''),
    [page, setPage] = useState('seats'),
    [seats, setSeats] = useState<Seat[]>([]),
    [visits, setVisits] = useState<VisitSummary[]>([]),
    [kitchen, setKitchen] = useState<KitchenOrder[]>([]),
    [at, setAt] = useState(() =>
      new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16),
    ),
    [scan, setScan] = useState('');
  const refresh = async () => {
    if (role === 'waiter' || role === 'manager') {
      setSeats(
        await api<Seat[]>(`staff/seats?at=${encodeURIComponent(new Date(at).toISOString())}`),
      );
      setVisits(await api<VisitSummary[]>('staff/visits'));
    }
    setKitchen(await api<KitchenOrder[]>('staff/kitchen'));
  };
  useEffect(() => {
    void api<{ role: StaffRole }>('staff/me')
      .then((v) => {
        setRole(v.role);
        if (v.role === 'cook') setPage('kitchen');
      })
      .catch(() => {});
  }, []);
  useEffect(() => {
    if (!role) return;
    void run(refresh);
    const timer = setInterval(() => {
      void refresh().catch(() => {});
    }, 15000);
    return () => clearInterval(timer);
  }, [role, at]);
  if (!role)
    return (
      <>
        <h1>Personal anmelden</h1>
        <form
          className="login"
          onSubmit={(e) => {
            e.preventDefault();
            void run(async () => {
              const v = await api<{ role: StaffRole }>('login', { email, password });
              setPassword('');
              setRole(v.role);
              setPage(v.role === 'cook' ? 'kitchen' : 'seats');
            });
          }}
        >
          <label>
            E-Mail
            <input
              type="email"
              required
              value={email}
              autoComplete="username"
              onChange={(e) => setEmail(e.target.value)}
            />
          </label>
          <label>
            Passwort
            <input
              type="password"
              required
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>
          <button className="primary">Anmelden</button>
        </form>
      </>
    );
  return (
    <>
      <div className="toolbar">
        <h1>Restaurantbetrieb</h1>
        <button
          onClick={() =>
            void run(async () => {
              await api('logout', {});
              setRole('');
            })
          }
        >
          Abmelden
        </button>
      </div>
      <nav className="subnav">
        {(role === 'cook'
          ? ['kitchen']
          : role === 'manager'
            ? ['seats', 'visit', 'kitchen', 'manage']
            : ['seats', 'visit', 'kitchen']
        ).map((p) => (
          <button key={p} aria-current={page === p ? 'page' : undefined} onClick={() => setPage(p)}>
            {
              (
                {
                  seats: 'Plätze',
                  visit: 'Check-in & Rechnung',
                  kitchen: 'Bestellungen',
                  manage: 'Verwaltung',
                } as Record<string, string>
              )[p]
            }
          </button>
        ))}
      </nav>
      {page === 'seats' && (
        <>
          <label>
            Zeitpunkt
            <input type="datetime-local" value={at} onChange={(e) => setAt(e.target.value)} />
          </label>
          <div className="seats">
            {seats.map((s) => (
              <div key={s.id} className={`seat ${s.status}`}>
                <strong>{s.label}</strong>
                <span>{s.table_name}</span>
                <span>{s.active ? status[s.status] : 'Deaktiviert'}</span>
              </div>
            ))}
          </div>
          <h2>Besuche</h2>
          <ul className="visits">
            {visits.map((p) => (
              <li key={p.id}>
                <strong>{p.name}</strong> · {date(p.starts_at)} · {p.label} · {status[p.status]}
                {p.status === 'checked_in' && (
                  <button
                    onClick={() => {
                      if (
                        confirm(
                          'Besuch beenden und Platz nach Reinigungspuffer freigeben? Offene Forderungen bleiben bestehen.',
                        )
                      )
                        void run(async () => {
                          await api(`staff/visits/${p.id}/finish`, {});
                          await refresh();
                        });
                    }}
                  >
                    Besuch beenden
                  </button>
                )}
                {p.overdue && <strong className="error"> · Seit über zwei Stunden aktiv</strong>}
              </li>
            ))}
          </ul>
        </>
      )}
      {page === 'visit' && (
        <>
          <Scanner onScan={setScan} />
          <Visit menu={menu} token={scan} setToken={setScan} run={run} notify={notify} staff />
        </>
      )}
      {page === 'kitchen' && (
        <>
          <h2>Offene Bestellungen</h2>
          <button onClick={() => void run(refresh)}>Aktualisieren</button>
          {kitchen.length === 0 && <p>Keine offenen Bestellungen.</p>}
          <ul className="kitchen">
            {kitchen.map((o) => (
              <li key={o.id}>
                <h3>
                  {o.quantity} × {o.name}
                </h3>
                <p>
                  {o.guest} · {o.label} · {date(o.starts_at)} · {status[o.status]}
                </p>
                {((role === 'waiter' && o.status === 'ready') ||
                  (role === 'cook' && o.status !== 'ready') ||
                  role === 'manager') && (
                  <button
                    onClick={() =>
                      void run(async () => {
                        await api(
                          `staff/orders/${o.id}`,
                          {
                            status: { open: 'preparing', preparing: 'ready', ready: 'served' }[
                              o.status as 'open' | 'preparing' | 'ready'
                            ],
                          },
                          undefined,
                          'PATCH',
                        );
                        await refresh();
                      })
                    }
                  >
                    {
                      (
                        {
                          open: 'Zubereitung beginnen',
                          preparing: 'Bereit melden',
                          ready: 'Serviert bestätigen',
                        } as Record<string, string>
                      )[o.status]
                    }
                  </button>
                )}
              </li>
            ))}
          </ul>
        </>
      )}
      {page === 'manage' && (
        <Management
          menu={menu}
          seats={seats}
          run={run}
          refresh={async () => {
            await refreshMenu();
            await refresh();
          }}
        />
      )}
    </>
  );
}
