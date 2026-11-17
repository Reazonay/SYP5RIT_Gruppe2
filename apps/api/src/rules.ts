export type Status = 'reserved' | 'checked_in' | 'ended' | 'cancelled' | 'no_show';
export const MINUTE = 60_000;
export function cancellationFee(
  minutesBefore: number,
  group: boolean,
  whole: boolean,
  subtotal: number,
): number {
  if (minutesBefore >= 60) return 0;
  if (group && whole) return 2000;
  if (group && minutesBefore <= 15) return Math.round(subtotal * 0.75);
  return 1000;
}
export function canCheckIn(status: Status, now: number, start: number): boolean {
  return status === 'reserved' && now >= start - 30 * MINUTE && now < start + 15 * MINUTE;
}
export function balance(subtotal: number, fee: number, paid: number, terminal: boolean): number {
  return (terminal ? fee : subtotal + fee) - paid;
}
