import { useEffect, useState } from 'react';
import { api, date, status } from '../api';
import TokenInput from '../components/TokenInput';
import type { GroupView, Run } from '../types';
export default function Group({
  token,
  setToken,
  run,
}: {
  token: string;
  setToken: (s: string) => void;
  run: Run;
}) {
  const [group, setGroup] = useState<GroupView | null>(null),
    [start, setStart] = useState('');
  const load = async () => setGroup(await api<GroupView>('group', undefined, token));
  useEffect(() => {
    setGroup(null);
    if (token.length === 64) void run(load);
  }, [token]);
  return (
    <>
      <h1>Gruppenverwaltung</h1>
      <TokenInput value={token} setValue={setToken} label="Verwaltungszugang" />
      {group && (
        <>
          <p>
            {date(group.starts_at)} – {date(group.ends_at)}
          </p>
          <ul>
            {group.people.map((p: { id: string; name: string; status: string }) => (
              <li key={p.id}>
                {p.name} · {status[p.status]}
              </li>
            ))}
          </ul>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              void run(async () => {
                await api('group/shift', { startsAt: new Date(start).toISOString() }, token);
                await load();
              });
            }}
          >
            <label>
              Neuer Beginn
              <input
                type="datetime-local"
                required
                value={start}
                onChange={(e) => setStart(e.target.value)}
              />
            </label>
            <button>Termin verschieben</button>
          </form>
          <button
            className="danger"
            onClick={() => {
              if (
                confirm(
                  'Gesamte Reservierung stornieren? Unter einer Stunde fällt die Gruppenpauschale von 20 € an.',
                )
              )
                void run(async () => {
                  await api('group/cancel', {}, token);
                  await load();
                });
            }}
          >
            Gesamte Reservierung stornieren
          </button>
        </>
      )}
    </>
  );
}
