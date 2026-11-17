import { ConflictException, ServiceUnavailableException } from '@nestjs/common';
import { randomUUID } from 'node:crypto';
import Stripe from 'stripe';
import { transaction } from './db.js';
import { lockGuest, lockParticipant, publicUrl, totals } from './service.js';
const stripe = () => {
  const key = process.env.STRIPE_SECRET_KEY;
  if (!key?.startsWith('sk_test_'))
    throw new ServiceUnavailableException('Stripe-Testzugang fehlt');
  return new Stripe(key, { timeout: 15000, maxNetworkRetries: 2 });
};
async function paypal(path: string, body?: unknown, key?: string) {
  const id = process.env.PAYPAL_CLIENT_ID,
    secret = process.env.PAYPAL_CLIENT_SECRET;
  if (!id || !secret) throw new ServiceUnavailableException('PayPal-Testzugang fehlt');
  const auth = await fetch('https://api-m.sandbox.paypal.com/v1/oauth2/token', {
    method: 'POST',
    headers: {
      Authorization: `Basic ${Buffer.from(`${id}:${secret}`).toString('base64')}`,
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: 'grant_type=client_credentials',
    signal: AbortSignal.timeout(15000),
  });
  if (!auth.ok) throw new ServiceUnavailableException('PayPal-Anmeldung fehlgeschlagen');
  const credentials = (await auth.json()) as any;
  const result = await fetch(`https://api-m.sandbox.paypal.com${path}`, {
    method: body ? 'POST' : 'GET',
    headers: {
      Authorization: `Bearer ${credentials.access_token}`,
      'Content-Type': 'application/json',
      ...(key ? { 'PayPal-Request-Id': key } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(15000),
  });
  if (!result.ok) throw new ServiceUnavailableException('PayPal-Anfrage fehlgeschlagen');
  return result.json() as Promise<any>;
}
export async function pay(
  access: string,
  method: 'cash' | 'card' | 'paypal',
  requestKey: string,
  amount: number,
  staff: boolean,
) {
  if (method === 'card') stripe();
  if (method === 'paypal' && (!process.env.PAYPAL_CLIENT_ID || !process.env.PAYPAL_CLIENT_SECRET))
    throw new ServiceUnavailableException('PayPal-Testzugang fehlt');
  const intent = await transaction(async (db) => {
    const p = await lockGuest(db, access);
    const existing = await db.query('SELECT * FROM payments WHERE request_key=$1', [requestKey]);
    if (existing.rowCount) {
      const old = existing.rows[0];
      if (old.participant_id !== p.id || old.amount !== amount || old.method !== method)
        throw new ConflictException('Zahlungsschlüssel bereits anderweitig verwendet');
      return old;
    }
    if (method === 'cash' && !staff)
      throw new ConflictException('Barzahlung muss Personal bestätigen');
    const pending = await db.query(
      "SELECT 1 FROM payments WHERE participant_id=$1 AND status='pending'",
      [p.id],
    );
    if (pending.rowCount) throw new ConflictException('Vorherige Zahlung bitte zuerst prüfen');
    const t = await totals(db, p.id);
    if (amount > t.due) throw new ConflictException('Betrag übersteigt offenen Saldo');
    const result = await db.query(
      'INSERT INTO payments(id,participant_id,request_key,method,amount,status) VALUES($1,$2,$3,$4,$5,$6) RETURNING *',
      [randomUUID(), p.id, requestKey, method, amount, method === 'cash' ? 'paid' : 'pending'],
    );
    return result.rows[0];
  });
  if (intent.status !== 'pending' || intent.provider_id) return intent;
  // The intent is committed before external I/O. Retries reuse the provider key.
  return transaction(async (db) => {
    const result = await db.query('SELECT * FROM payments WHERE id=$1 FOR UPDATE', [intent.id]);
    const current = result.rows[0];
    if (current.provider_id || current.status !== 'pending') return current;
    let providerId: string | null = null,
      url: string | null = null;
    if (method === 'card') {
      const session = await stripe().checkout.sessions.create(
        {
          mode: 'payment',
          payment_method_types: ['card'],
          line_items: [
            {
              price_data: {
                currency: 'eur',
                unit_amount: amount,
                product_data: { name: 'Restaurantrechnung' },
              },
              quantity: 1,
            },
          ],
          success_url: `${publicUrl}/#guest=${access}`,
          cancel_url: `${publicUrl}/#guest=${access}`,
        },
        { idempotencyKey: requestKey },
      );
      providerId = session.id;
      url = session.url;
    } else if (method === 'paypal') {
      const order = await paypal(
        '/v2/checkout/orders',
        {
          intent: 'CAPTURE',
          purchase_units: [{ amount: { currency_code: 'EUR', value: (amount / 100).toFixed(2) } }],
          payment_source: {
            paypal: {
              experience_context: {
                return_url: `${publicUrl}/#guest=${access}`,
                cancel_url: `${publicUrl}/#guest=${access}`,
                user_action: 'PAY_NOW',
              },
            },
          },
        },
        requestKey,
      );
      providerId = order.id;
      url = order.links.find((l: any) => ['approve', 'payer-action'].includes(l.rel))?.href;
      if (!url) throw new ServiceUnavailableException('PayPal-Zahlungslink fehlt');
    }
    return (
      await db.query('UPDATE payments SET provider_id=$2,checkout_url=$3 WHERE id=$1 RETURNING *', [
        current.id,
        providerId,
        url,
      ])
    ).rows[0];
  });
}
export async function reconcile(access: string | undefined, id: string) {
  return transaction(async (db) => {
    const participant = access
      ? undefined
      : (await db.query('SELECT participant_id FROM payments WHERE id=$1', [id])).rows[0];
    const guest = access
      ? await lockGuest(db, access)
      : participant
        ? await lockParticipant(db, participant.participant_id)
        : undefined;
    if (!guest) throw new ConflictException('Zahlung nicht gefunden');
    const result = await db.query(
      'SELECT * FROM payments WHERE id=$1 AND participant_id=$2 FOR UPDATE',
      [id, guest.id],
    );
    if (!result.rowCount) throw new ConflictException('Zahlung nicht gefunden');
    const p = result.rows[0];
    if (p.status !== 'pending') return { status: p.status };
    if (!p.provider_id) throw new ConflictException('Zahlungslink bitte zuerst wiederherstellen');
    let status = 'pending';
    if (p.method === 'card') {
      const session = await stripe().checkout.sessions.retrieve(p.provider_id);
      if (
        session.payment_status === 'paid' &&
        session.amount_total === p.amount &&
        session.currency === 'eur'
      )
        status = 'paid';
      else if (session.status === 'expired') status = 'failed';
    } else if (p.method === 'paypal') {
      let order = await paypal(`/v2/checkout/orders/${p.provider_id}`);
      if (order.status === 'APPROVED')
        order = await paypal(`/v2/checkout/orders/${p.provider_id}/capture`, {}, p.id);
      const captures = order.purchase_units?.flatMap((u: any) => u.payments?.captures || []) || [];
      if (
        captures.some(
          (c: any) =>
            c.status === 'COMPLETED' &&
            c.amount.currency_code === 'EUR' &&
            Math.round(Number(c.amount.value) * 100) === p.amount,
        )
      )
        status = 'paid';
      else if (order.status === 'VOIDED') status = 'failed';
    }
    await db.query('UPDATE payments SET status=$2 WHERE id=$1', [p.id, status]);
    return { status };
  });
}
export async function refund(access: string, id: string, staff: boolean) {
  // Persist the refund identity before contacting a payment provider.
  const intent = await transaction(async (db) => {
    const guest = await lockGuest(db, access);
    const result = await db.query(
      'SELECT * FROM payments WHERE id=$1 AND participant_id=$2 FOR UPDATE',
      [id, guest.id],
    );
    if (!result.rowCount) throw new ConflictException('Zahlung nicht gefunden');
    const p = result.rows[0],
      t = await totals(db, guest.id);
    if (p.status !== 'paid' || t.due >= 0) throw new ConflictException('Keine Erstattung offen');
    if (p.method === 'cash' && !staff)
      throw new ConflictException('Barauszahlung durch Personal erforderlich');
    const pending = await db.query(
      "SELECT r.* FROM refunds r JOIN payments p ON p.id=r.payment_id WHERE p.participant_id=$1 AND r.status='pending' FOR UPDATE OF r",
      [guest.id],
    );
    if (pending.rowCount) {
      if (pending.rows[0].payment_id !== id)
        throw new ConflictException('Bitte vorherige Erstattung abschließen');
      return pending.rows[0];
    }
    return (
      await db.query('INSERT INTO refunds(id,payment_id,amount) VALUES($1,$2,$3) RETURNING *', [
        randomUUID(),
        p.id,
        Math.min(-t.due, p.amount - p.refunded),
      ])
    ).rows[0];
  });
  return transaction(async (db) => {
    await lockGuest(db, access);
    const r = (await db.query('SELECT * FROM refunds WHERE id=$1 FOR UPDATE', [intent.id])).rows[0];
    if (r.status === 'done') return { refunded: r.amount };
    const p = (await db.query('SELECT * FROM payments WHERE id=$1 FOR UPDATE', [id])).rows[0];
    let providerId = null;
    if (p.method === 'card') {
      const session = await stripe().checkout.sessions.retrieve(p.provider_id);
      const response = await stripe().refunds.create(
        { payment_intent: session.payment_intent as string, amount: r.amount },
        { idempotencyKey: r.id },
      );
      if (response.status !== 'succeeded')
        throw new ServiceUnavailableException('Erstattung noch nicht abgeschlossen');
      providerId = response.id;
    } else if (p.method === 'paypal') {
      const order = await paypal(`/v2/checkout/orders/${p.provider_id}`);
      const capture = order.purchase_units[0].payments.captures[0];
      const response = await paypal(
        `/v2/payments/captures/${capture.id}/refund`,
        { amount: { currency_code: 'EUR', value: (r.amount / 100).toFixed(2) } },
        r.id,
      );
      if (response.status !== 'COMPLETED')
        throw new ServiceUnavailableException('Erstattung noch nicht abgeschlossen');
      providerId = response.id;
    }
    await db.query("UPDATE refunds SET status='done',provider_id=$2 WHERE id=$1", [
      r.id,
      providerId,
    ]);
    await db.query('UPDATE payments SET refunded=refunded+$2 WHERE id=$1', [p.id, r.amount]);
    return { refunded: r.amount };
  });
}
