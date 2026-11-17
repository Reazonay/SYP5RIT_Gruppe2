CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS migrations (
  name text PRIMARY KEY
);

CREATE TABLE staff (
  id uuid PRIMARY KEY,
  email text UNIQUE NOT NULL,
  password_hash text NOT NULL,
  role text NOT NULL CHECK (role IN ('manager','waiter','cook'))
);

CREATE TABLE sessions (
  token_hash text PRIMARY KEY,
  staff_id uuid REFERENCES staff NOT NULL,
  expires_at timestamptz NOT NULL
);

CREATE TABLE seats (
  id serial PRIMARY KEY,
  label text UNIQUE NOT NULL,
  table_name text NOT NULL,
  active boolean NOT NULL DEFAULT true
);

CREATE TABLE menu (
  id serial PRIMARY KEY,
  name text NOT NULL,
  category text NOT NULL CHECK(category IN ('food','drink')),
  price integer NOT NULL CHECK(price>=0),
  available boolean NOT NULL DEFAULT true
);

CREATE TABLE bookings (
  id uuid PRIMARY KEY,
  organizer_hash text UNIQUE NOT NULL,
  starts_at timestamptz NOT NULL,
  ends_at timestamptz NOT NULL,
  CHECK(ends_at>starts_at)
);

CREATE TABLE participants (
  id uuid PRIMARY KEY,
  booking_id uuid NOT NULL REFERENCES bookings,
  token_hash text UNIQUE NOT NULL,
  is_organizer boolean NOT NULL DEFAULT false,
  name text NOT NULL,
  email text NOT NULL,
  phone text NOT NULL,
  method text NOT NULL CHECK(method IN ('cash','card','paypal')),
  status text NOT NULL DEFAULT 'reserved' CHECK(status IN ('reserved','checked_in','ended','cancelled','no_show')),
  checked_in_at timestamptz,
  ended_at timestamptz,
  fee integer NOT NULL DEFAULT 0 CHECK(fee>=0)
);

CREATE TABLE allocations (
  participant_id uuid PRIMARY KEY REFERENCES participants,
  seat_id integer NOT NULL REFERENCES seats,
  period tstzrange NOT NULL,
  active boolean NOT NULL DEFAULT true,
  EXCLUDE USING gist (seat_id WITH =, period WITH &&) WHERE (active)
);

CREATE TABLE orders (
  id uuid PRIMARY KEY,
  participant_id uuid NOT NULL REFERENCES participants,
  menu_id integer NOT NULL REFERENCES menu,
  name text NOT NULL,
  quantity integer NOT NULL CHECK(quantity BETWEEN 1 AND 99),
  unit_price integer NOT NULL CHECK(unit_price>=0),
  status text NOT NULL DEFAULT 'open' CHECK(status IN ('open','preparing','ready','served','cancelled')),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE payments (
  id uuid PRIMARY KEY,
  participant_id uuid NOT NULL REFERENCES participants,
  request_key uuid UNIQUE NOT NULL,
  method text NOT NULL CHECK(method IN ('cash','card','paypal')),
  amount integer NOT NULL CHECK(amount>0),
  status text NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','paid','failed')),
  provider_id text UNIQUE,
  checkout_url text,
  refunded integer NOT NULL DEFAULT 0 CHECK(refunded>=0 AND refunded<=amount),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE refunds (
  id uuid PRIMARY KEY,
  payment_id uuid NOT NULL REFERENCES payments,
  amount integer NOT NULL CHECK(amount>0),
  status text NOT NULL DEFAULT 'pending' CHECK(status IN ('pending','done')),
  provider_id text
);

CREATE TABLE feedback (
  participant_id uuid PRIMARY KEY REFERENCES participants,
  rating integer NOT NULL CHECK(rating BETWEEN 1 AND 5),
  comment text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE outbox (
  id bigserial PRIMARY KEY,
  recipient text NOT NULL,
  subject text NOT NULL,
  body text NOT NULL,
  sent_at timestamptz,
  attempts integer NOT NULL DEFAULT 0
);
