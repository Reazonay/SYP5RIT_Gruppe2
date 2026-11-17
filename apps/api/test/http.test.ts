import { test } from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import bcrypt from 'bcryptjs';
import { pool } from '../src/db.js';
const enabled = process.env.RUN_DATABASE_TESTS === '1';
test('HTTP authentication, authorization, validation, and CSRF', { skip: !enabled }, async (t) => {
  const port = 43127,
    origin = 'http://localhost:5173',
    base = `http://127.0.0.1:${port}/api`;
  const child = spawn(process.execPath, ['dist/main.js'], {
    env: { ...process.env, PORT: String(port), PUBLIC_URL: origin },
    stdio: 'pipe',
  });
  let logs = '';
  child.stdout.on('data', (d) => (logs += d));
  child.stderr.on('data', (d) => (logs += d));
  try {
    let ready = false;
    for (let i = 0; i < 100; i++) {
      try {
        if ((await fetch(`${base}/health`)).ok) {
          ready = true;
          break;
        }
      } catch {}
      await new Promise((r) => setTimeout(r, 50));
    }
    assert.ok(ready, logs);
    const request = (path: string, body?: unknown, cookie?: string, requestOrigin = origin) =>
      fetch(`${base}/${path}`, {
        method: body === undefined ? 'GET' : 'POST',
        headers: {
          Origin: requestOrigin,
          ...(body ? { 'Content-Type': 'application/json' } : {}),
          ...(cookie ? { Cookie: cookie } : {}),
        },
        body: body ? JSON.stringify(body) : undefined,
      });
    await t.test('anonymous staff access is rejected', async () =>
      assert.equal((await request('staff/seats?at=2030-01-01T12:00:00Z')).status, 401),
    );
    await t.test('cross-origin writes are rejected', async () =>
      assert.equal(
        (await request('reservations', {}, undefined, 'https://other.example')).status,
        403,
      ),
    );
    await t.test('invalid reservation is rejected', async () =>
      assert.equal((await request('reservations', {})).status, 400),
    );
    for (const role of ['cook', 'waiter', 'manager']) {
      const email = `${role}-${randomUUID()}@example.test`,
        password = randomUUID();
      await pool.query('INSERT INTO staff VALUES($1,$2,$3,$4)', [
        randomUUID(),
        email,
        await bcrypt.hash(password, 4),
        role,
      ]);
      await t.test(`${role} receives only authorized functions`, async () => {
        const login = await request('login', { email, password });
        assert.equal(login.status, 201);
        const cookie = login.headers.get('set-cookie')!.split(';')[0];
        assert.ok(login.headers.get('set-cookie')!.includes('HttpOnly'));
        assert.equal((await request('staff/me', undefined, cookie)).status, 200);
        assert.equal(
          (await request('staff/seats?at=2030-01-01T12:00:00Z', undefined, cookie)).status,
          role === 'cook' ? 403 : 200,
        );
        assert.equal(
          (
            await request(
              'staff/menu',
              { name: 'HTTP test', category: 'food', price: 100, available: true },
              cookie,
            )
          ).status,
          role === 'manager' ? 201 : 403,
        );
        assert.equal((await request('logout', {}, cookie)).status, 201);
        assert.equal((await request('staff/me', undefined, cookie)).status, 401);
      });
    }
    await t.test('stored records survive an independent API process', async () => {
      const menu = (await (await request('menu')).json()) as unknown[];
      assert.ok(menu.length > 0);
    });
  } finally {
    child.kill('SIGTERM');
    await new Promise<void>((resolve) => {
      if (child.exitCode !== null) return resolve();
      child.once('exit', () => resolve());
    });
    await pool.end();
  }
});
