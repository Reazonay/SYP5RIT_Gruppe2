import { useState } from 'react';
import { api, euro } from '../api';
import type { Menu, Run, Seat } from '../types';
export default function Management({
  menu,
  seats,
  run,
  refresh,
}: {
  menu: Menu[];
  seats: Seat[];
  run: Run;
  refresh: () => Promise<void>;
}) {
  const [editing, setEditing] = useState<Menu | null>(null);
  const [name, setName] = useState(''),
    [price, setPrice] = useState(''),
    [category, setCategory] = useState('food'),
    [label, setLabel] = useState(''),
    [tableName, setTableName] = useState('');
  return (
    <>
      <h2>Speisekarte verwalten</h2>
      <ul className="menu">
        {menu.map((m) => (
          <li key={m.id}>
            <span>
              {m.name} · {euro(m.price)}
            </span>
            <button
              onClick={() => {
                setEditing(m);
                setName(m.name);
                setPrice(String(m.price / 100));
                setCategory(m.category);
              }}
            >
              Bearbeiten
            </button>
            <button
              onClick={() =>
                void run(async () => {
                  await api(
                    `staff/menu/${m.id}`,
                    { ...m, available: !m.available },
                    undefined,
                    'PATCH',
                  );
                  await refresh();
                })
              }
            >
              {m.available ? 'Deaktivieren' : 'Aktivieren'}
            </button>
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          void run(async () => {
            await api(
              editing ? `staff/menu/${editing.id}` : 'staff/menu',
              {
                name,
                category,
                price: Math.round(Number(price) * 100),
                available: editing?.available ?? true,
              },
              undefined,
              editing ? 'PATCH' : 'POST',
            );
            setEditing(null);
            setName('');
            setPrice('');
            await refresh();
          });
        }}
      >
        <div className="row">
          <label>
            Name
            <input required value={name} onChange={(e) => setName(e.target.value)} />
          </label>
          <label>
            Preis in Euro
            <input
              required
              type="number"
              min="0"
              step=".01"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
            />
          </label>
          <label>
            Art
            <select value={category} onChange={(e) => setCategory(e.target.value)}>
              <option value="food">Speise</option>
              <option value="drink">Getränk</option>
            </select>
          </label>
        </div>
        <button>{editing ? 'Änderungen speichern' : 'Artikel hinzufügen'}</button>
        {editing && (
          <button
            type="button"
            onClick={() => {
              setEditing(null);
              setName('');
              setPrice('');
            }}
          >
            Abbrechen
          </button>
        )}
      </form>
      <h2>Plätze verwalten</h2>
      <ul>
        {seats.map((s) => (
          <li key={s.id}>
            {s.label} · {s.table_name}{' '}
            <button
              onClick={() =>
                void run(async () => {
                  await api(`staff/seats/${s.id}`, { active: !s.active }, undefined, 'PATCH');
                  await refresh();
                })
              }
            >
              {s.active ? 'Deaktivieren' : 'Aktivieren'}
            </button>
          </li>
        ))}
      </ul>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          void run(async () => {
            await api('staff/seats', { label, tableName });
            setLabel('');
            await refresh();
          });
        }}
      >
        <div className="row">
          <label>
            Platzbezeichnung
            <input required value={label} onChange={(e) => setLabel(e.target.value)} />
          </label>
          <label>
            Tisch
            <input required value={tableName} onChange={(e) => setTableName(e.target.value)} />
          </label>
        </div>
        <button>Platz hinzufügen</button>
      </form>
    </>
  );
}
