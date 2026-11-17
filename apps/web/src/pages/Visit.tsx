import { useEffect, useState } from 'react';
import { api, date, euro, status } from '../api';
import MenuList from '../components/MenuList';
import TokenInput from '../components/TokenInput';
import type { Guest, Menu, Order, Payment, Run } from '../types';
export default function Visit({
  menu,
  token,
  setToken,
  run,
  notify,
  staff = false,
}: {
  menu: Menu[];
  token: string;
  setToken: (s: string) => void;
  run: Run;
  notify: (s: string) => void;
  staff?: boolean;
}) {
  const [p, setP] = useState<Guest | null>(null),
    [qr, setQr] = useState(''),
    [amount, setAmount] = useState(''),
    [rating, setRating] = useState(5),
    [comment, setComment] = useState('');
  const load = async () => {
    const v = await api<Guest>(staff ? 'staff/reservation' : 'reservation', undefined, token);
    setP(v);
    setAmount((Math.max(0, v.due) / 100).toFixed(2));
    if (v.usable) setQr((await api<{ image: string }>('qr', undefined, token)).image);
    else setQr('');
  };
  useEffect(() => {
    setP(null);
    setQr('');
    if (token.length === 64) void run(load);
  }, [token, staff]);
  const action = (path: string, body: unknown = {}) =>
    void run(async () => {
      await api(path, body, token);
      await load();
      notify('Gespeichert.');
    });
  return (
    <>
      <h1>{staff ? 'Besuch bearbeiten' : 'Mein Besuch'}</h1>
      <TokenInput value={token} setValue={setToken} label="Reservierungszugang" />
      {p && (
        <>
          <section>
            <h2>{p.name}</h2>
            <p>
              {date(p.starts_at)} – {date(p.ends_at)} · {status[p.status]}
              {['checked_in', 'ended'].includes(p.status) ? ` · ${p.seat_label}` : ''}
            </p>
            {qr && <img className="qr" src={qr} alt="QR-Code für den Check-in" />}
            <div className="actions">
              <button onClick={() => void run(load)}>Aktualisieren</button>
              {p.status === 'reserved' && (
                <>
                  <button
                    onClick={() => {
                      if (confirm('Reservierung kostenpflichtig gemäß Gebührenregeln stornieren?'))
                        action('cancel');
                    }}
                  >
                    Stornieren
                  </button>
                  {staff && <button onClick={() => action('staff/check-in')}>Einchecken</button>}
                </>
              )}
              {p.status === 'checked_in' && (
                <button onClick={() => action(staff ? 'staff/finish' : 'finish')}>
                  Besuch beenden{staff ? ' (offene Forderung bleibt)' : ''}
                </button>
              )}
            </div>
          </section>
          <section>
            <h2>Rechnung</h2>
            <p>
              Bestellungen: {euro(p.subtotal)} · Gebühren: {euro(p.fee)} · Bezahlt: {euro(p.paid)}
            </p>
            <p>
              <strong>
                {p.due < 0 ? 'Guthaben' : 'Offen'}: {euro(Math.abs(p.due))}
              </strong>
            </p>
            {p.due > 0 && (
              <>
                <label>
                  Betrag in Euro
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                  />
                </label>
                <div className="actions">
                  {(staff ? ['cash'] : ['card', 'paypal']).map((method) => (
                    <button
                      key={method}
                      onClick={() =>
                        void run(async () => {
                          const requestKey = crypto.randomUUID();
                          const payment = await api<Payment>(
                            staff ? 'staff/payments' : 'payments',
                            { method, amount: Math.round(Number(amount) * 100), requestKey },
                            token,
                          );
                          if (payment.checkout_url) location.assign(payment.checkout_url);
                          else await load();
                        })
                      }
                    >
                      {method === 'cash'
                        ? 'Barzahlung erfassen'
                        : method === 'card'
                          ? 'Mit Kreditkarte zahlen'
                          : 'Mit PayPal zahlen'}
                    </button>
                  ))}
                </div>
              </>
            )}
            {p.payments.map((v: Payment) => (
              <div className="payment" key={v.id}>
                <span>
                  {v.method} · {euro(v.amount)} · {status[v.status]}
                  {v.refunded > 0 ? ` · Erstattet ${euro(v.refunded)}` : ''}
                </span>
                {v.status === 'pending' && (
                  <>
                    {v.checkout_url ? (
                      <a href={v.checkout_url}>Zahlung fortsetzen</a>
                    ) : (
                      <button
                        onClick={() =>
                          void run(async () => {
                            const payment = await api<Payment>(
                              'payments',
                              { method: v.method, amount: v.amount, requestKey: v.request_key },
                              token,
                            );
                            if (payment.checkout_url) location.assign(payment.checkout_url);
                            await load();
                          })
                        }
                      >
                        Zahlungslink wiederherstellen
                      </button>
                    )}
                    <button onClick={() => action(`payments/${v.id}/check`)}>
                      Zahlungsstatus prüfen
                    </button>
                  </>
                )}
                {p.due < 0 && v.status === 'paid' && v.refunded < v.amount && (
                  <button onClick={() => action(`${staff ? 'staff/' : ''}payments/${v.id}/refund`)}>
                    Erstattung {v.method === 'cash' ? 'auszahlen' : 'anfordern'}
                  </button>
                )}
              </div>
            ))}
          </section>
          <section>
            <h2>Bestellungen</h2>
            <ul>
              {p.orders.map((o: Order) => (
                <li key={o.id}>
                  {o.quantity} × {o.name} · {euro(o.quantity * o.unit_price)} · {status[o.status]}
                </li>
              ))}
            </ul>
          </section>
          {p.usable && (
            <MenuList
              menu={menu}
              order={(menuId, quantity) => action('orders', { menuId, quantity })}
            />
          )}{' '}
          {p.status === 'ended' && !staff && (
            <form
              onSubmit={(e) => {
                e.preventDefault();
                action('feedback', { rating, comment });
              }}
            >
              <h2>Feedback</h2>
              <label>
                Bewertung
                <select value={rating} onChange={(e) => setRating(+e.target.value)}>
                  {[1, 2, 3, 4, 5].map((n) => (
                    <option key={n} value={n}>
                      {n} von 5
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Kommentar
                <textarea
                  maxLength={2000}
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                />
              </label>
              <button>Feedback senden</button>
            </form>
          )}
        </>
      )}
    </>
  );
}
