export type Menu = {
  id: number;
  name: string;
  category: string;
  price: number;
  available: boolean;
};
export type Run = (fn: () => Promise<void>) => Promise<void>;
export type ReservationStatus = 'reserved' | 'checked_in' | 'ended' | 'cancelled' | 'no_show';
export interface Order {
  id: string;
  name: string;
  quantity: number;
  unit_price: number;
  status: 'open' | 'preparing' | 'ready' | 'served' | 'cancelled';
}
export interface Payment {
  id: string;
  request_key: string;
  method: 'cash' | 'card' | 'paypal';
  amount: number;
  status: 'pending' | 'paid' | 'failed';
  refunded: number;
  checkout_url: string | null;
}
export interface Guest {
  seat_label: string;
  id: string;
  name: string;
  starts_at: string;
  ends_at: string;
  status: ReservationStatus;
  subtotal: number;
  fee: number;
  paid: number;
  due: number;
  usable: boolean;
  orders: Order[];
  payments: Payment[];
}
export interface GroupView {
  id: string;
  starts_at: string;
  ends_at: string;
  people: { id: string; name: string; status: ReservationStatus }[];
}
export interface Seat {
  id: number;
  label: string;
  table_name: string;
  active: boolean;
  status: 'free' | 'reserved' | 'occupied';
}
export interface VisitSummary {
  id: string;
  name: string;
  starts_at: string;
  label: string;
  status: ReservationStatus;
  overdue: boolean;
}
export interface KitchenOrder extends Order {
  guest: string;
  label: string;
  starts_at: string;
}
export type StaffRole = 'manager' | 'waiter' | 'cook';
