import { readFile } from 'node:fs/promises';
import { pool, transaction } from './db.js';
await transaction(async (db) => {
  await db.query('SELECT pg_advisory_xact_lock(80401)');
  await db.query('CREATE TABLE IF NOT EXISTS migrations (name text PRIMARY KEY)');
  const done = await db.query("SELECT 1 FROM migrations WHERE name='001_initial'");
  if (!done.rowCount) {
    await db.query(await readFile(new URL('../../../db/001_initial.sql', import.meta.url), 'utf8'));
    await db.query("INSERT INTO migrations VALUES ('001_initial')");
  }
});
await pool.end();
