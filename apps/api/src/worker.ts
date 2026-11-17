import nodemailer from 'nodemailer';
import QRCode from 'qrcode';
import { pool, transaction } from './db.js';
import { reconcile } from './payments.js';
import { cancelPerson, lockParticipant } from './service.js';
export async function tick() {
  const due = await pool.query(
    "SELECT p.id FROM participants p JOIN bookings b ON b.id=p.booking_id WHERE p.status='reserved' AND b.starts_at<=now()-interval '15 minutes'",
  );
  for (const row of due.rows)
    await transaction(async (db) => {
      const p = await lockParticipant(db, row.id);
      if (p.status === 'reserved' && +new Date(p.starts_at) <= Date.now() - 15 * 60000)
        await cancelPerson(db, p, 1000, 'no_show');
    });
  const pending = await pool.query(
    "SELECT id FROM payments WHERE status='pending' AND provider_id IS NOT NULL ORDER BY created_at LIMIT 20",
  );
  for (const payment of pending.rows) {
    try {
      await reconcile(undefined, payment.id);
    } catch (error: any) {
      console.error('Payment reconciliation deferred', payment.id, error.name);
    }
  }
  const transport = nodemailer.createTransport({
    host: process.env.SMTP_HOST || 'localhost',
    port: Number(process.env.SMTP_PORT || 1025),
    secure: Number(process.env.SMTP_PORT || 1025) === 465,
    requireTLS: process.env.NODE_ENV === 'production',
    ...(process.env.SMTP_USER
      ? { auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASSWORD } }
      : {}),
  });
  await transaction(async (db) => {
    const result = await db.query(
      'SELECT * FROM outbox WHERE sent_at IS NULL AND attempts<20 ORDER BY id LIMIT 10 FOR UPDATE SKIP LOCKED',
    );
    for (const m of result.rows) {
      try {
        const guestLink = m.body.match(/https?:\/\/[^\s]+\/#guest=[a-f0-9]{64}/)?.[0];
        await transport.sendMail({
          from: process.env.SMTP_FROM || 'restaurant@example.test',
          to: m.recipient,
          subject: m.subject,
          text: m.body,
          ...(guestLink
            ? {
                attachments: [
                  { filename: 'reservierung.png', content: await QRCode.toBuffer(guestLink) },
                ],
              }
            : {}),
          messageId: `<restaurant-${m.id}@restaurant.local>`,
        });
        await db.query('UPDATE outbox SET sent_at=now(),body=$2 WHERE id=$1', [
          m.id,
          '[Zugestellt]',
        ]);
      } catch {
        await db.query('UPDATE outbox SET attempts=attempts+1 WHERE id=$1', [m.id]);
      }
    }
  });
  await pool.query('DELETE FROM sessions WHERE expires_at<now()');
}
