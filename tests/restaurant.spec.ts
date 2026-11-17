import { test, expect } from '@playwright/test';
test('mobile reservation, preorder, staff check-in, cash payment, checkout and feedback', async ({
  page,
}) => {
  await page.setViewportSize({ width: 360, height: 800 });
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Platz reservieren' })).toBeVisible();
  await expect
    .poll(() => page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
    .toBe(true);
  const start = new Date(Date.now() + 5 * 60000);
  const local = new Date(+start - start.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  await page.getByLabel('Beginn', { exact: true }).fill(local);
  await page.getByLabel('Name', { exact: true }).fill('Browsergast');
  await page.getByLabel('E-Mail', { exact: true }).fill('browser@example.test');
  await page.getByLabel('Telefon', { exact: true }).fill('0123456789');
  await page.getByRole('button', { name: 'Verbindlich reservieren' }).click();
  await expect(page.getByRole('heading', { name: 'Browsergast' })).toBeVisible();
  await expect(page.getByAltText('QR-Code für den Check-in')).toBeVisible();
  const access = new URLSearchParams(new URL(page.url()).hash.slice(1)).get('guest')!;
  expect(access).toHaveLength(64);
  const curry = page.locator('.menu li').filter({ hasText: 'Gemüsecurry' });
  await curry.getByRole('button', { name: 'Bestellen', exact: true }).click();
  await expect(page.getByText('1 × Gemüsecurry')).toBeVisible();
  await page.getByRole('button', { name: 'Personal', exact: true }).click();
  await page.getByLabel('E-Mail', { exact: true }).fill('waiter@restaurant.test');
  await page.getByLabel('Passwort').fill(process.env.SEED_PASSWORD!);
  await page.getByRole('button', { name: 'Anmelden', exact: true }).click();
  await page.getByRole('button', { name: 'Check-in & Rechnung' }).click();
  await page.getByLabel('Reservierungszugang').fill(access);
  await page.getByRole('button', { name: 'Einchecken', exact: true }).click();
  await expect(page.getByText(/· Eingecheckt/)).toBeVisible();
  await page.getByRole('button', { name: 'Barzahlung erfassen' }).click();
  await expect(page.getByText(/Offen:.*0,00/)).toBeVisible();
  await page.getByRole('button', { name: 'Mein Besuch', exact: true }).click();
  await page.getByRole('button', { name: 'Aktualisieren', exact: true }).click();
  await page.getByRole('button', { name: 'Besuch beenden', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Feedback', exact: true })).toBeVisible();
  await expect(page.getByAltText('QR-Code für den Check-in')).toHaveCount(0);
  await page.getByLabel('Kommentar').fill('Hat funktioniert.');
  await page.getByRole('button', { name: 'Feedback senden' }).click();
  await expect(page.getByRole('status')).toContainText('Gespeichert.');
});
test('group reservation and cancellation', async ({ page }) => {
  await page.goto('/');
  const start = new Date(Date.now() + 180 * 60000);
  await page
    .getByLabel('Beginn', { exact: true })
    .fill(new Date(+start - start.getTimezoneOffset() * 60000).toISOString().slice(0, 16));
  await page.getByLabel('Personen').fill('2');
  for (let i = 0; i < 2; i++) {
    await page.getByLabel('Name', { exact: true }).nth(i).fill(`Gruppe ${i}`);
    await page.getByLabel('E-Mail', { exact: true }).nth(i).fill(`group${i}@example.test`);
    await page.getByLabel('Telefon', { exact: true }).nth(i).fill('12345');
  }
  await page.getByRole('button', { name: 'Verbindlich reservieren' }).click();
  await expect(page.getByRole('heading', { name: 'Gruppe 0' })).toBeVisible();
  await page.getByRole('button', { name: 'Gruppe', exact: true }).click();
  await expect(page.getByText('Gruppe 1 · Reserviert')).toBeVisible();
  page.once('dialog', (d) => d.accept());
  await page.getByRole('button', { name: 'Gesamte Reservierung stornieren' }).click();
  await expect(page.getByText('Gruppe 1 · Storniert')).toBeVisible();
});
