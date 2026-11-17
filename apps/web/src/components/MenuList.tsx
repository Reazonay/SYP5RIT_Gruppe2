import { useState } from 'react';
import { euro } from '../api';
import type { Menu } from '../types';
export default function MenuList({
  menu,
  order,
}: {
  menu: Menu[];
  order?: (id: number, q: number) => void;
}) {
  return (
    <section>
      <h2>Speisekarte</h2>
      {['food', 'drink'].map((category) => (
        <div key={category}>
          <h3>{category === 'food' ? 'Speisen' : 'Getränke'}</h3>
          <ul className="menu">
            {menu
              .filter((m) => m.category === category)
              .map((m) => (
                <li key={m.id}>
                  <div>
                    <strong>{m.name}</strong>
                    {!m.available && <span> · Nicht verfügbar</span>}
                  </div>
                  <span>{euro(m.price)}</span>
                  {order && <OrderButton disabled={!m.available} onOrder={(q) => order(m.id, q)} />}
                </li>
              ))}
          </ul>
        </div>
      ))}
    </section>
  );
}
function OrderButton({ disabled, onOrder }: { disabled: boolean; onOrder: (q: number) => void }) {
  const [q, setQ] = useState(1);
  return (
    <div className="inline">
      <input
        aria-label="Menge"
        className="quantity"
        type="number"
        min="1"
        max="99"
        value={q}
        onChange={(e) => setQ(+e.target.value)}
      />
      <button disabled={disabled || q < 1 || q > 99} onClick={() => onOrder(q)}>
        Bestellen
      </button>
    </div>
  );
}
