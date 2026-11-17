import {
  Body,
  ConflictException,
  Controller,
  Get,
  Param,
  Patch,
  Post,
  Query,
  Req,
  Res,
  UnauthorizedException,
  UseGuards,
} from '@nestjs/common';
import bcrypt from 'bcryptjs';
import type { Request, Response } from 'express';
import QRCode from 'qrcode';
import { z } from 'zod';
import { Roles, StaffGuard } from './auth.js';
import { hash, pool, token, transaction } from './db.js';
import * as payments from './payments.js';
import * as service from './service.js';
import { amount, uuid } from './validation.js';
const access = (req: Request) =>
  z
    .string()
    .regex(/^[a-f0-9]{64}$/)
    .parse(req.headers['x-reservation-token']);
const paymentBody = z.object({
  method: z.enum(['cash', 'card', 'paypal']),
  requestKey: uuid,
  amount,
});
@Controller('api')
@UseGuards(StaffGuard)
export class ApiController {
  @Get('health') async health() {
    await pool.query('SELECT 1');
    return { ok: true };
  }
  @Get('menu') async menu() {
    return (await pool.query('SELECT * FROM menu ORDER BY category,id')).rows;
  }
  @Post('reservations') reserve(@Body() body: unknown) {
    return service.reserve(body);
  }
  @Get('reservation') view(@Req() req: Request) {
    return service.view(access(req));
  }
  @Get('qr') async qr(@Req() req: Request) {
    const a = access(req),
      p = await service.view(a);
    if (!p.usable) throw new ConflictException('QR-Code gesperrt');
    return { image: await QRCode.toDataURL(`${service.publicUrl}/#guest=${a}`) };
  }
  @Post('orders') order(@Req() req: Request, @Body() body: unknown) {
    return service.order(
      access(req),
      z
        .object({ menuId: z.number().int().positive(), quantity: z.number().int().min(1).max(99) })
        .parse(body),
    );
  }
  @Post('cancel') cancel(@Req() req: Request) {
    return service.cancel(access(req));
  }
  @Post('finish') finish(@Req() req: Request) {
    return service.finish(access(req));
  }
  @Get('group') group(@Req() req: Request) {
    return service.group(access(req));
  }
  @Post('group/cancel') cancelGroup(@Req() req: Request) {
    return service.cancelGroup(access(req));
  }
  @Post('group/shift') shift(@Req() req: Request, @Body() body: unknown) {
    return service.shift(
      access(req),
      z.object({ startsAt: z.string().datetime({ offset: true }) }).parse(body).startsAt,
    );
  }
  @Post('payments') pay(@Req() req: Request, @Body() body: unknown) {
    const b = paymentBody.parse(body);
    return payments.pay(access(req), b.method, b.requestKey, b.amount, false);
  }
  @Post('payments/:id/check') checkPayment(@Req() req: Request, @Param('id') id: string) {
    return payments.reconcile(access(req), uuid.parse(id));
  }
  @Post('payments/:id/refund') refund(@Req() req: Request, @Param('id') id: string) {
    return payments.refund(access(req), uuid.parse(id), false);
  }
  @Post('feedback') feedback(@Req() req: Request, @Body() body: unknown) {
    const b = z
      .object({ rating: z.number().int().min(1).max(5), comment: z.string().trim().max(2000) })
      .parse(body);
    return transaction(async (db) => {
      const p = await service.lockGuest(db, access(req));
      if (p.status !== 'ended')
        throw new ConflictException('Feedback erst nach Besuchsende möglich');
      await db.query('INSERT INTO feedback VALUES($1,$2,$3,now())', [p.id, b.rating, b.comment]);
      return { ok: true };
    });
  }
  @Post('login') async login(@Body() body: unknown, @Res({ passthrough: true }) res: Response) {
    const b = z.object({ email: z.string().email(), password: z.string().max(256) }).parse(body);
    const user = await pool.query('SELECT * FROM staff WHERE email=$1', [b.email]);
    const valid = await bcrypt.compare(
      b.password,
      user.rows[0]?.password_hash || '$2b$12$C6UzMDM.H6dfI/f/IKcEe.9xZsqCuPXo8I5I7M0fS5GYmKQfD0gGq',
    );
    if (!user.rowCount || !valid) throw new UnauthorizedException('Anmeldedaten ungültig');
    const session = token();
    await pool.query("INSERT INTO sessions VALUES($1,$2,now()+interval '8 hours')", [
      hash(session),
      user.rows[0].id,
    ]);
    res.cookie('session', session, {
      httpOnly: true,
      sameSite: 'strict',
      secure: process.env.NODE_ENV === 'production',
      maxAge: 8 * 3600000,
      path: '/api',
    });
    return { role: user.rows[0].role };
  }
  @Post('logout') async logout(@Req() req: Request, @Res({ passthrough: true }) res: Response) {
    await pool.query('DELETE FROM sessions WHERE token_hash=$1', [
      hash(req.cookies?.session || ''),
    ]);
    res.clearCookie('session', { path: '/api' });
    return { ok: true };
  }
  @Get('staff/me') @Roles('manager', 'waiter', 'cook') me(@Req() req: any) {
    return req.staff;
  }
  @Get('staff/seats') @Roles('manager', 'waiter') async seats(@Query('at') at: string) {
    const date = z.string().datetime({ offset: true }).parse(at);
    return (
      await pool.query(
        `SELECT s.*,CASE WHEN EXISTS(SELECT 1 FROM allocations a JOIN participants p ON p.id=a.participant_id WHERE a.seat_id=s.id AND p.status='checked_in') THEN 'occupied' WHEN EXISTS(SELECT 1 FROM allocations a WHERE a.seat_id=s.id AND a.active AND a.period @> $1::timestamptz) THEN 'reserved' ELSE 'free' END status FROM seats s ORDER BY table_name,id`,
        [date],
      )
    ).rows;
  }
  @Get('staff/visits') @Roles('manager', 'waiter') async visits() {
    return (
      await pool.query(
        `SELECT p.id,p.name,p.status,p.checked_in_at,b.starts_at,s.label, p.status='checked_in' AND p.checked_in_at<=now()-interval '2 hours' AS overdue FROM participants p JOIN bookings b ON b.id=p.booking_id JOIN allocations a ON a.participant_id=p.id JOIN seats s ON s.id=a.seat_id WHERE b.starts_at>=now()-interval '1 day' OR p.status='checked_in' ORDER BY b.starts_at`,
      )
    ).rows;
  }
  @Post('staff/check-in') @Roles('waiter', 'manager') checkIn(@Req() req: Request) {
    return service.checkIn(access(req));
  }
  @Get('staff/reservation') @Roles('waiter', 'manager') staffView(@Req() req: Request) {
    return service.view(access(req));
  }
  @Post('staff/finish') @Roles('waiter', 'manager') staffFinish(@Req() req: Request) {
    return service.finish(access(req), true);
  }
  @Post('staff/payments') @Roles('waiter', 'manager') cash(
    @Req() req: Request,
    @Body() body: unknown,
  ) {
    const b = paymentBody.extend({ method: z.literal('cash') }).parse(body);
    return payments.pay(access(req), b.method, b.requestKey, b.amount, true);
  }
  @Post('staff/payments/:id/refund') @Roles('waiter', 'manager') cashRefund(
    @Req() req: Request,
    @Param('id') id: string,
  ) {
    return payments.refund(access(req), uuid.parse(id), true);
  }
  @Post('staff/visits/:id/finish')
  @Roles('waiter', 'manager')
  endAbsentGuest(@Param('id') id: string) {
    return service.finish(undefined, true, uuid.parse(id));
  }
  @Get('staff/kitchen') @Roles('cook', 'manager', 'waiter') async kitchen() {
    return (
      await pool.query(
        `SELECT o.*,p.name guest,b.starts_at,s.label FROM orders o JOIN participants p ON p.id=o.participant_id JOIN bookings b ON b.id=p.booking_id JOIN allocations a ON a.participant_id=p.id JOIN seats s ON s.id=a.seat_id WHERE o.status NOT IN ('cancelled','served') ORDER BY b.starts_at,o.created_at`,
      )
    ).rows;
  }
  @Patch('staff/orders/:id') @Roles('cook', 'manager', 'waiter') async orderStatus(
    @Req() req: any,
    @Param('id') id: string,
    @Body() body: unknown,
  ) {
    const b = z.object({ status: z.enum(['preparing', 'ready', 'served']) }).parse(body);
    if (
      (req.staff.role === 'cook' && b.status === 'served') ||
      (req.staff.role === 'waiter' && b.status !== 'served')
    )
      throw new UnauthorizedException('Statuswechsel nicht erlaubt');
    const previous = { preparing: 'open', ready: 'preparing', served: 'ready' }[b.status];
    const result = await pool.query(
      'UPDATE orders SET status=$2 WHERE id=$1 AND status=$3 RETURNING id',
      [uuid.parse(id), b.status, previous],
    );
    if (!result.rowCount) throw new ConflictException('Bestellstatus hat sich geändert');
    return { ok: true };
  }
  @Post('staff/menu') @Roles('manager') async addMenu(@Body() body: unknown) {
    const b = z
      .object({
        name: z.string().trim().min(1).max(120),
        category: z.enum(['food', 'drink']),
        price: z.number().int().min(0).max(100000),
        available: z.boolean(),
      })
      .parse(body);
    return (
      await pool.query(
        'INSERT INTO menu(name,category,price,available) VALUES($1,$2,$3,$4) RETURNING *',
        [b.name, b.category, b.price, b.available],
      )
    ).rows[0];
  }
  @Patch('staff/menu/:id') @Roles('manager') async editMenu(
    @Param('id') id: string,
    @Body() body: unknown,
  ) {
    const b = z
      .object({
        name: z.string().trim().min(1).max(120),
        category: z.enum(['food', 'drink']),
        price: z.number().int().min(0).max(100000),
        available: z.boolean(),
      })
      .parse(body);
    return (
      await pool.query(
        'UPDATE menu SET name=$2,category=$3,price=$4,available=$5 WHERE id=$1 RETURNING *',
        [z.coerce.number().int().positive().parse(id), b.name, b.category, b.price, b.available],
      )
    ).rows[0];
  }
  @Post('staff/seats') @Roles('manager') async addSeat(@Body() body: unknown) {
    const b = z
      .object({
        label: z.string().trim().min(1).max(80),
        tableName: z.string().trim().min(1).max(80),
      })
      .parse(body);
    return (
      await pool.query('INSERT INTO seats(label,table_name) VALUES($1,$2) RETURNING *', [
        b.label,
        b.tableName,
      ])
    ).rows[0];
  }
  @Patch('staff/seats/:id') @Roles('manager') async seatActive(
    @Param('id') id: string,
    @Body() body: unknown,
  ) {
    const b = z.object({ active: z.boolean() }).parse(body);
    return transaction(async (db) => {
      await db.query('SELECT pg_advisory_xact_lock(80402)');
      const sid = z.coerce.number().int().positive().parse(id);
      if (!b.active) {
        const reserved = await db.query(
          'SELECT 1 FROM allocations WHERE seat_id=$1 AND active AND upper(period)>now()',
          [sid],
        );
        if (reserved.rowCount) throw new ConflictException('Platz hat noch Reservierungen');
      }
      return (await db.query('UPDATE seats SET active=$2 WHERE id=$1 RETURNING *', [sid, b.active]))
        .rows[0];
    });
  }
}
