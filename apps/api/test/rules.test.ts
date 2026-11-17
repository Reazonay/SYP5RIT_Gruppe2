import { test } from 'node:test';
import assert from 'node:assert/strict';
import { cancellationFee, canCheckIn, MINUTE, balance } from '../src/rules.js';
for (const group of [false, true])
  test(`free cancellation at exactly 60 minutes, group=${group}`, () =>
    assert.equal(cancellationFee(60, group, false, 5000), 0));
test('single cancellation under one hour costs 10 EUR', () =>
  assert.equal(cancellationFee(59.99, false, false, 5000), 1000));
test('whole group fee is charged once, independent of subtotal', () =>
  assert.equal(cancellationFee(10, true, true, 100000), 2000));
test('partial cancellation before final 15 minutes costs 10 EUR', () =>
  assert.equal(cancellationFee(15.001, true, false, 5000), 1000));
test('partial cancellation at exactly 15 minutes is 75 percent', () =>
  assert.equal(cancellationFee(15, true, false, 5000), 3750));
test('no preorder means zero percentage fee', () =>
  assert.equal(cancellationFee(0, true, false, 0), 0));
test('percentage rounds to integer cents', () =>
  assert.equal(cancellationFee(5, true, false, 999), 749));
test('check-in includes opening and excludes no-show boundary', () => {
  const start = 10_000_000;
  assert.equal(canCheckIn('reserved', start - 30 * MINUTE, start), true);
  assert.equal(canCheckIn('reserved', start - 30 * MINUTE - 1, start), false);
  assert.equal(canCheckIn('reserved', start + 15 * MINUTE - 1, start), true);
  assert.equal(canCheckIn('reserved', start + 15 * MINUTE, start), false);
  assert.equal(canCheckIn('checked_in', start, start), false);
});
test('partial payments and cancellation credit', () => {
  assert.equal(balance(3000, 0, 2000, false), 1000);
  assert.equal(balance(3000, 1000, 3000, true), -2000);
});
