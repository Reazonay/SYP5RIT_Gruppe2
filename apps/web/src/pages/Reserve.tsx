import { useState } from 'react';
import { api } from '../api';
import MenuList from '../components/MenuList';
import type { Menu, Run } from '../types';
const emptyPerson = () => ({ name: '', email: '', phone: '', method: 'cash' });
export default function Reserve({
  menu,
  run,
  done,
}: {
  menu: Menu[];
  run: Run;
  done: (g: string, o: string) => void;
}) {
  const [people, setPeople] = useState([emptyPerson()]),
    [start, setStart] = useState(''),
    [duration, setDuration] = useState(60);
  return (
    <>
      <h1>Platz reservieren</h1>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          void run(async () => {
            const r = await api<{ guestToken: string; organizerToken: string }>('reservations', {
              startsAt: new Date(start).toISOString(),
              duration,
              people,
            });
            done(r.guestToken, r.organizerToken);
          });
        }}
      >
        <div className="row">
          <label>
            Beginn
            <input
              type="datetime-local"
              required
              value={start}
              onChange={(e) => setStart(e.target.value)}
            />
          </label>
          <label>
            Dauer
            <select value={duration} onChange={(e) => setDuration(+e.target.value)}>
              {[30, 60, 90, 120, 180, 240].map((n) => (
                <option key={n} value={n}>
                  {n} Minuten
                </option>
              ))}
            </select>
          </label>
          <label>
            Personen
            <input
              type="number"
              min="1"
              max="10"
              value={people.length}
              onChange={(e) => {
                const n = Math.max(1, Math.min(10, +e.target.value));
                setPeople(Array.from({ length: n }, (_, i) => people[i] || emptyPerson()));
              }}
            />
          </label>
        </div>
        {people.map((p, i) => (
          <section key={i}>
            <h2>{i === 0 ? 'Ihre Kontaktdaten' : `Gast ${i + 1}`}</h2>
            <div className="row">
              {(['name', 'email', 'phone'] as const).map((k) => (
                <label key={k}>
                  {{ name: 'Name', email: 'E-Mail', phone: 'Telefon' }[k]}
                  <input
                    required
                    type={k === 'email' ? 'email' : k === 'phone' ? 'tel' : 'text'}
                    maxLength={k === 'email' ? 254 : 120}
                    value={p[k]}
                    onChange={(e) =>
                      setPeople(people.map((v, j) => (j === i ? { ...v, [k]: e.target.value } : v)))
                    }
                  />
                </label>
              ))}
              <label>
                Zahlungsart
                <select
                  value={p.method}
                  onChange={(e) =>
                    setPeople(
                      people.map((v, j) => (j === i ? { ...v, method: e.target.value } : v)),
                    )
                  }
                >
                  <option value="cash">Bar</option>
                  <option value="card">Kreditkarte</option>
                  <option value="paypal">PayPal</option>
                </select>
              </label>
            </div>
          </section>
        ))}
        <p>
          Jede Person erhält einen eigenen Reservierungslink per E-Mail. Check-in: 30 Minuten vor
          bis 15 Minuten nach Beginn.
        </p>
        <details>
          <summary>Storno- und Gebührenregeln</summary>
          <p>
            Ab 60 Minuten vorher kostenlos. Einzelstorno danach 10 €. Gruppen-Gesamtstorno danach 20
            €. Teilstorno danach 10 € je Person, ab 15 Minuten vorher 75 % der jeweiligen
            Vorbestellsumme. Nichterscheinen nach 15 Minuten: 10 € pro Person.
          </p>
        </details>
        <button type="submit" className="primary">
          Verbindlich reservieren
        </button>
      </form>
      <MenuList menu={menu} />
    </>
  );
}
