import bcrypt from 'bcryptjs';
import { randomUUID } from 'node:crypto';
import { pool, transaction } from './db.js';
const password = process.env.SEED_PASSWORD;
if (!password || password.length < 14)
  throw Error('SEED_PASSWORD must contain at least 14 characters');
const digest = await bcrypt.hash(password, 12);
await transaction(async (db) => {
  for (const role of ['manager', 'waiter', 'cook'])
    await db.query('INSERT INTO staff VALUES ($1,$2,$3,$4) ON CONFLICT(email) DO NOTHING', [
      randomUUID(),
      `${role}@restaurant.test`,
      digest,
      role,
    ]);
  for (let i = 1; i <= 30; i++)
    await db.query(
      'INSERT INTO seats(label,table_name) VALUES($1,$2) ON CONFLICT(label) DO NOTHING',
      [`Platz ${i}`, `Tisch ${Math.ceil(i / 10)}`],
    );
  const count = await db.query('SELECT count(*) FROM menu');
  if (Number(count.rows[0].count) === 0)
    for (const [name, category, price] of [
      ['Tagessuppe', 'food', 590],
      ['Gemüsecurry', 'food', 1290],
      ['Salat', 'food', 790],
      ['Apfelsaft', 'drink', 350],
      ['Mineralwasser', 'drink', 290],
    ])
      await db.query('INSERT INTO menu(name,category,price) VALUES($1,$2,$3)', [
        name,
        category,
        price,
      ]);
});
await pool.end();
