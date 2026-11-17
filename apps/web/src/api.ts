export async function api<T = void>(path: string, body?: unknown, token?: string, method?: string) {
  const response = await fetch(`/api/${path}`, {
    method: method || (body === undefined ? 'GET' : 'POST'),
    headers: {
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { 'X-Reservation-Token': token } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.message || 'Anfrage fehlgeschlagen');
  return data as T;
}
export const euro = (cents: number) =>
  new Intl.NumberFormat('de-AT', { style: 'currency', currency: 'EUR' }).format(cents / 100);
export const date = (value: string) => new Date(value).toLocaleString('de-AT');
export const status: Record<string, string> = {
  reserved: 'Reserviert',
  checked_in: 'Eingecheckt',
  ended: 'Beendet',
  cancelled: 'Storniert',
  no_show: 'Nicht erschienen',
  free: 'Frei',
  occupied: 'Belegt',
  open: 'Offen',
  preparing: 'In Zubereitung',
  ready: 'Bereit',
  served: 'Serviert',
  paid: 'Bezahlt',
  pending: 'Ausständig',
  failed: 'Fehlgeschlagen',
};
