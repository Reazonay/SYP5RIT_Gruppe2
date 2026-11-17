import { test, after } from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { tick } from '../src/worker.js';
import { pool, transaction, hash } from '../src/db.js';
import {
  reserve,
  view,
  order,
  cancel,
  checkIn,
  finish,
  group,
  shift,
  cancelGroup,
} from '../src/service.js';
import { pay, reconcile, refund } from '../src/payments.js';
const enabled = process.env.RUN_DATABASE_TESTS === '1';
after(async () => pool.end());
test(
  'database acceptance suite (dedicated empty test database required)',
  { skip: !enabled },
  async (t) => {
    const label = `Test ${randomUUID()}`;
    await pool.query('INSERT INTO seats(label,table_name) VALUES($1,$2)', [label, label]);
    const item = (
      await pool.query(
        "INSERT INTO menu(name,category,price) VALUES('Testgericht','food',3000) RETURNING id",
      )
    ).rows[0].id;
    const person = {
      name: 'Testgast',
      email: 'guest@example.test',
      phone: '123456',
      method: 'cash',
    };
    const startsAt = new Date(Date.now() + 10 * 60_000).toISOString();
    let booking: any;
    await t.test('concurrent booking attempts cannot claim the same seat', async () => {
      const results = await Promise.allSettled(
        Array.from({ length: 4 }, () => reserve({ startsAt, duration: 60, people: [person] })),
      );
      assert.equal(results.filter((r) => r.status === 'fulfilled').length, 1);
      booking = (results.find((r) => r.status === 'fulfilled') as PromiseFulfilledResult<any>)
        .value;
    });
    await t.test('unknown tokens cannot read reservations', async () => {
      await assert.rejects(() => view('a'.repeat(64)));
    });
    await t.test('preorder snapshots the price; unavailable item rejected', async () => {
      await order(booking.guestToken, { menuId: item, quantity: 1 });
      await pool.query('UPDATE menu SET price=5000,available=false WHERE id=$1', [item]);
      assert.equal((await view(booking.guestToken)).subtotal, 3000);
      await assert.rejects(() => order(booking.guestToken, { menuId: item, quantity: 1 }));
    });
    await t.test('cash payment is idempotent; guest cannot record cash', async () => {
      const key = randomUUID();
      await assert.rejects(() => pay(booking.guestToken, 'cash', randomUUID(), 2000, false));
      const a = await pay(booking.guestToken, 'cash', key, 2000, true);
      const b = await pay(booking.guestToken, 'cash', key, 2000, true);
      assert.equal(a.id, b.id);
      assert.equal((await view(booking.guestToken)).due, 1000);
      await assert.rejects(() => pay(booking.guestToken, 'cash', key, 1000, true));
    });
    await t.test('check-in once; unpaid guest cannot close; staff can', async () => {
      await checkIn(booking.guestToken);
      await assert.rejects(() => checkIn(booking.guestToken));
      await assert.rejects(() => cancel(booking.guestToken));
      await assert.rejects(() => finish(booking.guestToken));
      await finish(booking.guestToken, true);
      const v = await view(booking.guestToken);
      assert.equal(v.status, 'ended');
      assert.equal(v.usable, false);
      assert.equal(v.due, 1000);
      await assert.rejects(() => order(booking.guestToken, { menuId: item, quantity: 1 }));
    });
    await t.test('database exclusion constraint independently rejects overlap', async () => {
      await assert.rejects(
        () =>
          transaction(async (db) => {
            const p = (
              await db.query('SELECT * FROM participants WHERE booking_id=$1', [booking.id])
            ).rows[0];
            const allocation = (
              await db.query('SELECT * FROM allocations WHERE participant_id=$1', [p.id])
            ).rows[0];
            const id = randomUUID();
            await db.query(
              'INSERT INTO participants(id,booking_id,token_hash,name,email,phone,method) VALUES($1,$2,$3,$4,$5,$6,$7)',
              [id, booking.id, hash(randomUUID()), 'Other', 'other@example.test', '123', 'cash'],
            );
            await db.query('INSERT INTO allocations VALUES($1,$2,$3,true)', [
              id,
              allocation.seat_id,
              allocation.period,
            ]);
          }),
        (e: any) => e.code === '23P01',
      );
    });
    await t.test(
      'group cancellation releases seats, cancels orders, and preserves refund balance',
      async () => {
        const table = `Group ${randomUUID()}`;
        for (let i = 0; i < 2; i++)
          await pool.query('INSERT INTO seats(label,table_name) VALUES($1,$2)', [
            `${table} ${i}`,
            table,
          ]);
        const g = await reserve({
          startsAt,
          duration: 60,
          people: [person, { ...person, name: 'Second' }],
        });
        await pool.query('UPDATE menu SET available=true WHERE id=$1', [item]);
        await order(g.guestToken, { menuId: item, quantity: 1 });
        await pay(g.guestToken, 'cash', randomUUID(), 5000, true);
        await cancelGroup(g.organizerToken);
        assert.equal(
          (await group(g.organizerToken)).people.every(
            (p: { status: string }) => p.status === 'cancelled',
          ),
          true,
        );
        const v = await view(g.guestToken);
        assert.equal(v.subtotal, 0);
        assert.equal(v.fee, 2000);
        assert.equal(v.due, -3000);
        await refund(g.guestToken, v.payments[0].id, true);
        assert.equal((await view(g.guestToken)).due, 0);
        await assert.rejects(() => refund(g.guestToken, v.payments[0].id, true));
        await assert.rejects(() => checkIn(g.guestToken));
      },
    );
    await t.test('failed shift leaves original reservation and allocation intact', async () => {
      const future = await reserve({
        startsAt: new Date(Date.now() + 180 * 60_000).toISOString(),
        duration: 60,
        people: [person],
      });
      const old = await view(future.guestToken);
      for (let i = 0; i < 4; i++) {
        try {
          await reserve({ startsAt, duration: 60, people: [person] });
        } catch {
          break;
        }
      }
      await assert.rejects(() => shift(future.organizerToken, startsAt));
      assert.equal(+new Date((await view(future.guestToken)).starts_at), +new Date(old.starts_at));
    });

    await t.test(
      'sandbox payment verifies capture and refunds once after cancellation',
      async () => {
        const b = await reserve({
          startsAt: new Date(Date.now() + 500 * 60000).toISOString(),
          duration: 60,
          people: [person],
        });
        await order(b.guestToken, { menuId: item, quantity: 1 });
        const original = globalThis.fetch;
        const requests: { url: string; key: string | null }[] = [];
        process.env.PAYPAL_CLIENT_ID = 'test-client';
        process.env.PAYPAL_CLIENT_SECRET = 'test-secret';
        globalThis.fetch = async (input: any, init?: RequestInit) => {
          const url = String(input);
          requests.push({ url, key: new Headers(init?.headers).get('PayPal-Request-Id') });
          let body: any = {};
          if (url.endsWith('/token')) body = { access_token: 'test-access' };
          else if (url.endsWith('/v2/checkout/orders'))
            body = {
              id: 'TEST-ORDER',
              links: [
                {
                  rel: 'payer-action',
                  href: 'https://www.sandbox.paypal.com/checkoutnow?token=TEST-ORDER',
                },
              ],
            };
          else if (url.endsWith('/capture'))
            body = {
              status: 'COMPLETED',
              purchase_units: [
                {
                  payments: {
                    captures: [
                      {
                        id: 'TEST-CAPTURE',
                        status: 'COMPLETED',
                        amount: { currency_code: 'EUR', value: '50.00' },
                      },
                    ],
                  },
                },
              ],
            };
          else if (url.endsWith('/refund')) body = { id: 'TEST-REFUND', status: 'COMPLETED' };
          else
            body = {
              status: 'APPROVED',
              purchase_units: [{ payments: { captures: [{ id: 'TEST-CAPTURE' }] } }],
            };
          return new Response(JSON.stringify(body), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          });
        };
        try {
          const key = randomUUID();
          const a = await pay(b.guestToken, 'paypal', key, 5000, false);
          const repeated = await pay(b.guestToken, 'paypal', key, 5000, false);
          assert.equal(a.id, repeated.id);
          assert.equal(requests.filter((r) => r.url.endsWith('/v2/checkout/orders')).length, 1);
          await reconcile(b.guestToken, a.id);
          await reconcile(b.guestToken, a.id);
          assert.equal((await view(b.guestToken)).paid, 5000);
          await cancel(b.guestToken);
          await refund(b.guestToken, a.id, false);
          await assert.rejects(() => refund(b.guestToken, a.id, false));
          assert.equal((await view(b.guestToken)).due, 0);
          assert.equal(requests.filter((r) => r.url.endsWith('/refund')).length, 1);
        } finally {
          globalThis.fetch = original;
          delete process.env.PAYPAL_CLIENT_ID;
          delete process.env.PAYPAL_CLIENT_SECRET;
        }
      },
    );
    await t.test(
      'no-show job charges once, cancels preorder, and releases allocation',
      async () => {
        const b = await reserve({
          startsAt: new Date(Date.now() + 1000 * 60000).toISOString(),
          duration: 60,
          people: [person],
        });
        await order(b.guestToken, { menuId: item, quantity: 1 });
        await pool.query(
          "UPDATE bookings SET starts_at=now()-interval '16 minutes',ends_at=now()+interval '44 minutes' WHERE id=$1",
          [b.id],
        );
        await tick();
        await tick();
        const v = await view(b.guestToken);
        assert.equal(v.status, 'no_show');
        assert.equal(v.fee, 1000);
        assert.equal(v.subtotal, 0);
        assert.equal(v.usable, false);
        const active = await pool.query('SELECT active FROM allocations WHERE participant_id=$1', [
          v.id,
        ]);
        assert.equal(active.rows[0].active, false);
      },
    );
  },
);
