import { BadRequestException, ConflictException, NotFoundException } from '@nestjs/common';
import { randomUUID } from 'node:crypto';
import type { PoolClient } from 'pg';
import { hash, pool, token, transaction } from './db.js';
import { balance, cancellationFee, canCheckIn, MINUTE } from './rules.js';
import { reservation } from './validation.js';
export const publicUrl = process.env.PUBLIC_URL || 'http://localhost:5173';
export async function lockParticipant(db: PoolClient, participantId: string) {
  const lookup = await db.query('SELECT booking_id FROM participants WHERE id=$1', [participantId]);
  if (!lookup.rowCount) throw new NotFoundException('Reservierung nicht gefunden');
  // All mutations lock the booking before its participants, including group operations.
  await db.query('SELECT id FROM bookings WHERE id=$1 FOR UPDATE', [lookup.rows[0].booking_id]);
  return (
    await db.query(
      'SELECT p.*,b.starts_at,b.ends_at FROM participants p JOIN bookings b ON b.id=p.booking_id WHERE p.id=$1 FOR UPDATE OF p',
      [participantId],
    )
  ).rows[0];
}
export async function lockGuest(db: PoolClient, access: string) {
  const lookup = await db.query('SELECT id FROM participants WHERE token_hash=$1', [hash(access)]);
  if (!lookup.rowCount) throw new NotFoundException('Reservierung nicht gefunden');
  return lockParticipant(db, lookup.rows[0].id);
}
export async function totals(db: PoolClient, id: string) {
  const result = await db.query(
    `SELECT p.fee,p.status,COALESCE((SELECT sum(quantity*unit_price) FROM orders WHERE participant_id=p.id AND status<>'cancelled'),0)::int subtotal, COALESCE((SELECT sum(amount-refunded) FROM payments WHERE participant_id=p.id AND status='paid'),0)::int paid FROM participants p WHERE p.id=$1`,
    [id],
  );
  const t = result.rows[0];
  return {
    ...t,
    due: balance(t.subtotal, t.fee, t.paid, ['cancelled', 'no_show'].includes(t.status)),
  };
}
async function mail(db: PoolClient, email: string, subject: string, body: string) {
  await db.query('INSERT INTO outbox(recipient,subject,body) VALUES($1,$2,$3)', [
    email,
    subject,
    body,
  ]);
}
export async function reserve(input: unknown) {
  const data = reservation.parse(input);
  const start = new Date(data.startsAt);
  const end = new Date(+start + data.duration * MINUTE);
  if (+start <= Date.now()) throw new BadRequestException('Beginn muss in der Zukunft liegen');
  return transaction(async (db) => {
    await db.query('SELECT pg_advisory_xact_lock(80402)');
    const seats = await db.query(
      `SELECT s.* FROM seats s WHERE active AND NOT EXISTS(SELECT 1 FROM allocations a WHERE a.seat_id=s.id AND a.active AND a.period && tstzrange($1,$2,'[)')) ORDER BY table_name,id FOR UPDATE`,
      [start, new Date(+end + 30 * MINUTE)],
    );
    const groups = Map.groupBy(seats.rows, (s: any) => s.table_name);
    const selected = [...groups.values()].find((s) => s.length >= data.people.length);
    if (!selected) throw new ConflictException('Nicht genügend zusammenhängende Plätze verfügbar');
    const id = randomUUID(),
      organizer = token();
    await db.query('INSERT INTO bookings VALUES($1,$2,$3,$4)', [id, hash(organizer), start, end]);
    let guestToken = '';
    for (let i = 0; i < data.people.length; i++) {
      const p = data.people[i],
        access = token(),
        pid = randomUUID();
      if (i === 0) guestToken = access;
      await db.query(
        'INSERT INTO participants(id,booking_id,token_hash,name,email,phone,method,is_organizer) VALUES($1,$2,$3,$4,$5,$6,$7,$8)',
        [pid, id, hash(access), p.name, p.email, p.phone, p.method, i === 0],
      );
      await db.query("INSERT INTO allocations VALUES($1,$2,tstzrange($3,$4,'[)'),true)", [
        pid,
        selected[i].id,
        start,
        new Date(+end + 30 * MINUTE),
      ]);
      await mail(
        db,
        p.email,
        'Reservierungsbestätigung',
        `Ihre Reservierung: ${publicUrl}/#guest=${access}\nBeginn: ${start.toISOString()}\n${i === 0 ? `Gruppenverwaltung: ${publicUrl}/#group=${organizer}` : ''}`,
      );
    }
    return { id, guestToken, organizerToken: organizer };
  });
}
export async function view(access: string) {
  return transaction(async (db) => {
    const p = await lockGuest(db, access);
    const seat = (
      await db.query(
        'SELECT s.label FROM allocations a JOIN seats s ON s.id=a.seat_id WHERE a.participant_id=$1',
        [p.id],
      )
    ).rows[0];
    const orders = await db.query(
      'SELECT * FROM orders WHERE participant_id=$1 ORDER BY created_at',
      [p.id],
    );
    const payments = await db.query(
      'SELECT id,request_key,method,amount,status,refunded,checkout_url FROM payments WHERE participant_id=$1 ORDER BY created_at',
      [p.id],
    );
    return {
      ...p,
      token_hash: undefined,
      seat_label: seat?.label,
      orders: orders.rows,
      payments: payments.rows,
      ...(await totals(db, p.id)),
      usable: ['reserved', 'checked_in'].includes(p.status),
    };
  });
}
export async function order(access: string, input: { menuId: number; quantity: number }) {
  return transaction(async (db) => {
    const p = await lockGuest(db, access);
    if (
      !['reserved', 'checked_in'].includes(p.status) ||
      (p.status === 'reserved' && Date.now() >= +new Date(p.starts_at) + 15 * MINUTE)
    )
      throw new ConflictException('Bestellen nicht mehr möglich');
    const item = await db.query('SELECT * FROM menu WHERE id=$1 AND available FOR SHARE', [
      input.menuId,
    ]);
    if (!item.rowCount) throw new ConflictException('Artikel nicht verfügbar');
    const m = item.rows[0];
    await db.query(
      'INSERT INTO orders(id,participant_id,menu_id,name,quantity,unit_price) VALUES($1,$2,$3,$4,$5,$6)',
      [randomUUID(), p.id, m.id, m.name, input.quantity, m.price],
    );
    return { ok: true };
  });
}
export async function cancelPerson(db: PoolClient, p: any, fee: number, status = 'cancelled') {
  if (p.status !== 'reserved') throw new ConflictException('Nur vor Check-in möglich');
  await db.query('UPDATE participants SET status=$2,fee=$3 WHERE id=$1', [p.id, status, fee]);
  await db.query(
    "UPDATE orders SET status='cancelled' WHERE participant_id=$1 AND status<>'cancelled'",
    [p.id],
  );
  await db.query('UPDATE allocations SET active=false WHERE participant_id=$1', [p.id]);
  await mail(
    db,
    p.email,
    status === 'no_show' ? 'Nicht erschienen' : 'Stornierungsbestätigung',
    `Gebühr: ${(fee / 100).toFixed(2)} EUR. Offene Beträge und Erstattungen sind über Ihren Reservierungslink einsehbar.`,
  );
}
export async function cancel(access: string) {
  return transaction(async (db) => {
    const p = await lockGuest(db, access);
    const count = await db.query('SELECT count(*) FROM participants WHERE booking_id=$1', [
      p.booking_id,
    ]);
    const t = await totals(db, p.id);
    const fee = cancellationFee(
      (+new Date(p.starts_at) - Date.now()) / MINUTE,
      Number(count.rows[0].count) > 1,
      false,
      t.subtotal,
    );
    await cancelPerson(db, p, fee);
    return { fee };
  });
}
export async function checkIn(access: string) {
  return transaction(async (db) => {
    const p = await lockGuest(db, access);
    if (!canCheckIn(p.status, Date.now(), +new Date(p.starts_at)))
      throw new ConflictException('Check-in nur 30 Minuten vor bis 15 Minuten nach Beginn möglich');
    const seat = await db.query('SELECT seat_id FROM allocations WHERE participant_id=$1', [p.id]);
    await db.query('SELECT id FROM seats WHERE id=$1 FOR UPDATE', [seat.rows[0].seat_id]);
    const occupied = await db.query(
      "SELECT 1 FROM participants p JOIN allocations a ON a.participant_id=p.id WHERE a.seat_id=$1 AND p.status='checked_in'",
      [seat.rows[0].seat_id],
    );
    if (occupied.rowCount)
      throw new ConflictException('Platz noch belegt – Personal muss Konflikt lösen');
    await db.query("UPDATE participants SET status='checked_in',checked_in_at=now() WHERE id=$1", [
      p.id,
    ]);
    return { seatId: seat.rows[0].seat_id };
  });
}
export async function finish(access: string | undefined, staff = false, participantId?: string) {
  return transaction(async (db) => {
    const p = access ? await lockGuest(db, access) : await lockParticipant(db, participantId!);
    if (p.status !== 'checked_in') throw new ConflictException('Besuch nicht aktiv');
    const t = await totals(db, p.id);
    if (t.due > 0 && !staff) throw new ConflictException('Bitte zuerst offenen Betrag bezahlen');
    await db.query("UPDATE participants SET status='ended',ended_at=now() WHERE id=$1", [p.id]);
    await db.query(
      "UPDATE allocations SET period=tstzrange(lower(period),LEAST(upper(period),now()+interval '30 minutes'),'[)') WHERE participant_id=$1",
      [p.id],
    );
    return { ok: true };
  });
}
export async function group(access: string) {
  const b = await pool.query('SELECT id,starts_at,ends_at FROM bookings WHERE organizer_hash=$1', [
    hash(access),
  ]);
  if (!b.rowCount) throw new NotFoundException();
  const people = await pool.query(
    'SELECT id,name,status FROM participants WHERE booking_id=$1 ORDER BY name',
    [b.rows[0].id],
  );
  return { ...b.rows[0], people: people.rows };
}
export async function cancelGroup(access: string) {
  return transaction(async (db) => {
    const b = await db.query('SELECT * FROM bookings WHERE organizer_hash=$1 FOR UPDATE', [
      hash(access),
    ]);
    if (!b.rowCount) throw new NotFoundException();
    const people = await db.query(
      'SELECT * FROM participants WHERE booking_id=$1 ORDER BY is_organizer DESC,id FOR UPDATE',
      [b.rows[0].id],
    );
    if (people.rows.some((p) => !['reserved', 'cancelled'].includes(p.status)))
      throw new ConflictException('Gesamtstorno nach Check-in oder No-show nicht möglich');
    const active = people.rows.filter((p) => p.status === 'reserved');
    if (!active.length) throw new ConflictException('Bereits storniert');
    const fee = cancellationFee(
      (+new Date(b.rows[0].starts_at) - Date.now()) / MINUTE,
      people.rows.length > 1,
      true,
      0,
    );
    for (let i = 0; i < active.length; i++) await cancelPerson(db, active[i], i === 0 ? fee : 0);
    return { fee };
  });
}
export async function shift(access: string, startsAt: string) {
  return transaction(async (db) => {
    await db.query('SELECT pg_advisory_xact_lock(80402)');
    const b = await db.query('SELECT * FROM bookings WHERE organizer_hash=$1 FOR UPDATE', [
      hash(access),
    ]);
    if (!b.rowCount) throw new NotFoundException();
    const booking = b.rows[0];
    if (
      +new Date(booking.starts_at) - Date.now() < 120 * MINUTE ||
      +new Date(startsAt) <= Date.now()
    )
      throw new ConflictException('Verschiebungsfrist überschritten oder neues Datum ungültig');
    const people = await db.query(
      'SELECT * FROM participants WHERE booking_id=$1 ORDER BY is_organizer DESC,id FOR UPDATE',
      [booking.id],
    );
    if (people.rows.some((p) => p.status !== 'reserved'))
      throw new ConflictException('Verschiebung nur bei vollständig offener Reservierung möglich');
    const end = new Date(
      +new Date(startsAt) + (+new Date(booking.ends_at) - +new Date(booking.starts_at)),
    );
    await db.query(
      'UPDATE allocations SET active=false WHERE participant_id IN (SELECT id FROM participants WHERE booking_id=$1)',
      [booking.id],
    );
    const seats = await db.query(
      "SELECT s.* FROM seats s WHERE active AND NOT EXISTS(SELECT 1 FROM allocations a WHERE a.active AND a.seat_id=s.id AND a.period && tstzrange($1,$2,'[)')) ORDER BY table_name,id FOR UPDATE",
      [startsAt, new Date(+end + 30 * MINUTE)],
    );
    const selection = [...Map.groupBy(seats.rows, (s: any) => s.table_name).values()].find(
      (s) => s.length >= people.rows.length,
    );
    if (!selection) throw new ConflictException('Keine zusammenhängenden Plätze verfügbar');
    for (let i = 0; i < people.rows.length; i++) {
      await db.query(
        "UPDATE allocations SET seat_id=$2,period=tstzrange($3,$4,'[)'),active=true WHERE participant_id=$1",
        [people.rows[i].id, selection[i].id, startsAt, new Date(+end + 30 * MINUTE)],
      );
      await mail(
        db,
        people.rows[i].email,
        'Reservierung verschoben',
        `Neuer Beginn: ${startsAt}. Ihr Reservierungslink bleibt gültig.`,
      );
    }
    await db.query('UPDATE bookings SET starts_at=$2,ends_at=$3 WHERE id=$1', [
      booking.id,
      startsAt,
      end,
    ]);
    return { ok: true };
  });
}
