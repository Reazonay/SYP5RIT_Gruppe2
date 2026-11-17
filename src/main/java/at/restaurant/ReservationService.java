package at.restaurant;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ReservationService {
  private final Db db;
  private final Clock clock;
  private final AppSettings settings;

  public ReservationService(Db db, Clock clock, AppSettings settings) {
    this.db = db;
    this.clock = clock;
    this.settings = settings;
  }

  // Booking-first locking is shared by guest, group, payment, and scheduled operations.
  Map<String, Object> lockParticipant(UUID id) {
    var p = db.one("SELECT booking_id FROM participants WHERE id=?", id);
    db.one("SELECT id FROM bookings WHERE id=? FOR UPDATE", p.get("booking_id"));
    return db.one(
        "SELECT p.*,b.starts_at,b.ends_at FROM participants p JOIN bookings b ON b.id=p.booking_id WHERE p.id=? FOR UPDATE OF p",
        id);
  }

  Map<String, Object> lockGuest(String access) {
    return lockParticipant(
        Db.id(
            db.one(
                "SELECT id FROM participants WHERE token_hash=?",
                Tokens.hash(Tokens.validate(access)))));
  }

  Map<String, Object> totals(UUID id) {
    var t =
        new LinkedHashMap<>(
            db.one(
                "SELECT p.fee,p.status,COALESCE((SELECT sum(quantity*unit_price) FROM orders WHERE participant_id=p.id AND status<>'cancelled'),0)::int subtotal,COALESCE((SELECT sum(amount-refunded) FROM payments WHERE participant_id=p.id AND status='paid'),0)::int paid FROM participants p WHERE p.id=?",
                id));
    t.put(
        "due",
        Rules.balance(
            Db.number(t, "subtotal"),
            Db.number(t, "fee"),
            Db.number(t, "paid"),
            Set.of("cancelled", "no_show").contains(t.get("status"))));
    return t;
  }

  void mail(String email, String subject, String body) {
    db.update("INSERT INTO outbox(recipient,subject,body) VALUES(?,?,?)", email, subject, body);
  }

  private List<Map<String, Object>> available(Instant start, Instant end, int count) {
    var seats =
        db.rows(
            "SELECT s.* FROM seats s WHERE active AND NOT EXISTS(SELECT 1 FROM allocations a WHERE a.seat_id=s.id AND a.active AND a.period && tstzrange(?,?,'[)')) ORDER BY table_name,id FOR UPDATE",
            start,
            end.plus(Duration.ofMinutes(30)));
    Map<Object, List<Map<String, Object>>> tables = new LinkedHashMap<>();
    for (var seat : seats)
      tables.computeIfAbsent(seat.get("table_name"), key -> new ArrayList<>()).add(seat);
    return tables.values().stream()
        .filter(group -> group.size() >= count)
        .findFirst()
        .orElseThrow(
            () -> ApiException.conflict("Nicht genügend zusammenhängende Plätze verfügbar"));
  }

  public Map<String, Object> reserve(Requests.Reservation input) {
    Instant start = input.startsAt(), end = start.plus(Duration.ofMinutes(input.duration()));
    if (!start.isAfter(clock.instant()))
      throw new ApiException(400, "Beginn muss in der Zukunft liegen");
    return db.tx(
        () -> {
          db.rows("SELECT pg_advisory_xact_lock(80402)");
          var seats = available(start, end, input.people().size());
          UUID booking = UUID.randomUUID();
          String organizer = Tokens.create(), guest = "";
          db.update(
              "INSERT INTO bookings VALUES(?,?,?,?)", booking, Tokens.hash(organizer), start, end);
          for (int i = 0; i < input.people().size(); i++) {
            var p = input.people().get(i);
            String access = Tokens.create();
            UUID id = UUID.randomUUID();
            if (i == 0) guest = access;
            db.update(
                "INSERT INTO participants(id,booking_id,token_hash,name,email,phone,method,is_organizer) VALUES(?,?,?,?,?,?,?,?)",
                id,
                booking,
                Tokens.hash(access),
                p.name().trim(),
                p.email(),
                p.phone().trim(),
                p.method(),
                i == 0);
            db.update(
                "INSERT INTO allocations VALUES(?,?,tstzrange(?,?,'[)'),true)",
                id,
                seats.get(i).get("id"),
                start,
                end.plus(Duration.ofMinutes(30)));
            mail(
                p.email(),
                "Reservierungsbestätigung",
                "Ihre Reservierung: "
                    + settings.publicUrl()
                    + "/#guest="
                    + access
                    + "\nBeginn: "
                    + start
                    + "\n"
                    + (i == 0
                        ? "Gruppenverwaltung: " + settings.publicUrl() + "/#group=" + organizer
                        : ""));
          }
          return Map.of("id", booking, "guestToken", guest, "organizerToken", organizer);
        });
  }

  public Map<String, Object> view(String access) {
    return db.tx(
        () -> {
          var p = new LinkedHashMap<>(lockGuest(access));
          UUID id = Db.id(p);
          p.remove("token_hash");
          p.put(
              "seat_label",
              db.one(
                      "SELECT s.label FROM allocations a JOIN seats s ON s.id=a.seat_id WHERE a.participant_id=?",
                      id)
                  .get("label"));
          p.put(
              "orders",
              db.rows("SELECT * FROM orders WHERE participant_id=? ORDER BY created_at", id));
          p.put(
              "payments",
              db.rows(
                  "SELECT id,request_key,method,amount,status,refunded,checkout_url FROM payments WHERE participant_id=? ORDER BY created_at",
                  id));
          p.putAll(totals(id));
          p.put("usable", Set.of("reserved", "checked_in").contains(p.get("status")));
          return p;
        });
  }

  public void order(String access, Requests.Order input) {
    db.tx(
        () -> {
          var p = lockGuest(access);
          if (!Set.of("reserved", "checked_in").contains(p.get("status"))
              || ("reserved".equals(p.get("status"))
                  && !clock
                      .instant()
                      .isBefore(Db.time(p, "starts_at").plus(Duration.ofMinutes(15)))))
            throw ApiException.conflict("Bestellen nicht mehr möglich");
          var item =
              db.optional("SELECT * FROM menu WHERE id=? AND available FOR SHARE", input.menuId())
                  .orElseThrow(() -> ApiException.conflict("Artikel nicht verfügbar"));
          db.update(
              "INSERT INTO orders(id,participant_id,menu_id,name,quantity,unit_price) VALUES(?,?,?,?,?,?)",
              UUID.randomUUID(),
              Db.id(p),
              item.get("id"),
              item.get("name"),
              input.quantity(),
              item.get("price"));
          return null;
        });
  }

  void cancelPerson(Map<String, Object> p, int fee, String status) {
    if (!"reserved".equals(p.get("status")))
      throw ApiException.conflict("Nur vor Check-in möglich");
    UUID id = Db.id(p);
    db.update("UPDATE participants SET status=?,fee=? WHERE id=?", status, fee, id);
    db.update(
        "UPDATE orders SET status='cancelled' WHERE participant_id=? AND status<>'cancelled'", id);
    db.update("UPDATE allocations SET active=false WHERE participant_id=?", id);
    mail(
        (String) p.get("email"),
        "no_show".equals(status) ? "Nicht erschienen" : "Stornierungsbestätigung",
        "Gebühr: "
            + String.format(Locale.ROOT, "%.2f", fee / 100.0)
            + " EUR. Offene Beträge und Erstattungen sind über Ihren Reservierungslink einsehbar.");
  }

  public Map<String, Object> cancel(String access) {
    return db.tx(
        () -> {
          var p = lockGuest(access);
          int count =
              Db.number(
                  db.one(
                      "SELECT count(*) AS count FROM participants WHERE booking_id=?",
                      p.get("booking_id")),
                  "count");
          int fee =
              Rules.cancellationFee(
                  Duration.between(clock.instant(), Db.time(p, "starts_at")),
                  count > 1,
                  false,
                  Db.number(totals(Db.id(p)), "subtotal"));
          cancelPerson(p, fee, "cancelled");
          return Map.of("fee", fee);
        });
  }

  public Map<String, Object> checkIn(String access) {
    return db.tx(
        () -> {
          var p = lockGuest(access);
          if (!Rules.canCheckIn((String) p.get("status"), clock.instant(), Db.time(p, "starts_at")))
            throw ApiException.conflict(
                "Check-in nur 30 Minuten vor bis 15 Minuten nach Beginn möglich");
          Object seat =
              db.one("SELECT seat_id FROM allocations WHERE participant_id=?", Db.id(p))
                  .get("seat_id");
          db.one("SELECT id FROM seats WHERE id=? FOR UPDATE", seat);
          if (!db.rows(
                  "SELECT 1 FROM participants p JOIN allocations a ON a.participant_id=p.id WHERE a.seat_id=? AND p.status='checked_in'",
                  seat)
              .isEmpty())
            throw ApiException.conflict("Platz noch belegt – Personal muss Konflikt lösen");
          db.update(
              "UPDATE participants SET status='checked_in',checked_in_at=? WHERE id=?",
              clock.instant(),
              Db.id(p));
          return Map.of("seatId", seat);
        });
  }

  public void finish(String access, boolean staff, UUID participant) {
    db.tx(
        () -> {
          var p = access == null ? lockParticipant(participant) : lockGuest(access);
          if (!"checked_in".equals(p.get("status")))
            throw ApiException.conflict("Besuch nicht aktiv");
          if (Db.number(totals(Db.id(p)), "due") > 0 && !staff)
            throw ApiException.conflict("Bitte zuerst offenen Betrag bezahlen");
          Instant now = clock.instant();
          db.update("UPDATE participants SET status='ended',ended_at=? WHERE id=?", now, Db.id(p));
          db.update(
              "UPDATE allocations SET period=tstzrange(lower(period),LEAST(upper(period),?::timestamptz),'[)') WHERE participant_id=?",
              now.plus(Duration.ofMinutes(30)),
              Db.id(p));
          return null;
        });
  }

  public Map<String, Object> group(String access) {
    return db.tx(
        () -> {
          var booking =
              new LinkedHashMap<>(
                  db.one(
                      "SELECT id,starts_at,ends_at FROM bookings WHERE organizer_hash=?",
                      Tokens.hash(Tokens.validate(access))));
          booking.put(
              "people",
              db.rows(
                  "SELECT id,name,status FROM participants WHERE booking_id=? ORDER BY name",
                  Db.id(booking)));
          return booking;
        });
  }

  public Map<String, Object> cancelGroup(String access) {
    return db.tx(
        () -> {
          var booking =
              db.one(
                  "SELECT * FROM bookings WHERE organizer_hash=? FOR UPDATE",
                  Tokens.hash(Tokens.validate(access)));
          var all =
              db.rows(
                  "SELECT * FROM participants WHERE booking_id=? ORDER BY is_organizer DESC,id FOR UPDATE",
                  Db.id(booking));
          if (all.stream()
              .anyMatch(p -> !Set.of("reserved", "cancelled").contains(p.get("status"))))
            throw ApiException.conflict("Gesamtstorno nach Check-in oder No-show nicht möglich");
          var active = all.stream().filter(p -> "reserved".equals(p.get("status"))).toList();
          if (active.isEmpty()) throw ApiException.conflict("Bereits storniert");
          int fee =
              Rules.cancellationFee(
                  Duration.between(clock.instant(), Db.time(booking, "starts_at")),
                  all.size() > 1,
                  true,
                  0);
          for (int i = 0; i < active.size(); i++)
            cancelPerson(active.get(i), i == 0 ? fee : 0, "cancelled");
          return Map.of("fee", fee);
        });
  }

  public void shift(String access, Instant start) {
    db.tx(
        () -> {
          db.rows("SELECT pg_advisory_xact_lock(80402)");
          var booking =
              db.one(
                  "SELECT * FROM bookings WHERE organizer_hash=? FOR UPDATE",
                  Tokens.hash(Tokens.validate(access)));
          if (Duration.between(clock.instant(), Db.time(booking, "starts_at"))
                      .compareTo(Duration.ofHours(2))
                  < 0
              || !start.isAfter(clock.instant()))
            throw ApiException.conflict(
                "Verschiebungsfrist überschritten oder neues Datum ungültig");
          var people =
              db.rows(
                  "SELECT * FROM participants WHERE booking_id=? ORDER BY is_organizer DESC,id FOR UPDATE",
                  Db.id(booking));
          if (people.stream().anyMatch(p -> !"reserved".equals(p.get("status"))))
            throw ApiException.conflict(
                "Verschiebung nur bei vollständig offener Reservierung möglich");
          Instant end =
              start.plus(
                  Duration.between(Db.time(booking, "starts_at"), Db.time(booking, "ends_at")));
          db.update(
              "UPDATE allocations SET active=false WHERE participant_id IN (SELECT id FROM participants WHERE booking_id=?)",
              Db.id(booking));
          var seats = available(start, end, people.size());
          for (int i = 0; i < people.size(); i++) {
            db.update(
                "UPDATE allocations SET seat_id=?,period=tstzrange(?,?,'[)'),active=true WHERE participant_id=?",
                seats.get(i).get("id"),
                start,
                end.plus(Duration.ofMinutes(30)),
                Db.id(people.get(i)));
            mail(
                (String) people.get(i).get("email"),
                "Reservierung verschoben",
                "Neuer Beginn: " + start + ". Ihr Reservierungslink bleibt gültig.");
          }
          db.update(
              "UPDATE bookings SET starts_at=?,ends_at=? WHERE id=?", start, end, Db.id(booking));
          return null;
        });
  }

  public void feedback(String access, Requests.Feedback input) {
    db.tx(
        () -> {
          var p = lockGuest(access);
          if (!"ended".equals(p.get("status")))
            throw ApiException.conflict("Feedback erst nach Besuchsende möglich");
          db.update(
              "INSERT INTO feedback VALUES(?,?,?,?)",
              Db.id(p),
              input.rating(),
              input.comment().trim(),
              clock.instant());
          return null;
        });
  }

  public void expireNoShows() {
    for (var row :
        db.rows(
            "SELECT p.id FROM participants p JOIN bookings b ON b.id=p.booking_id WHERE p.status='reserved' AND b.starts_at<=?",
            clock.instant().minus(Duration.ofMinutes(15)))) {
      db.tx(
          () -> {
            var p = lockParticipant(Db.id(row));
            if ("reserved".equals(p.get("status"))
                && !Db.time(p, "starts_at").isAfter(clock.instant().minus(Duration.ofMinutes(15))))
              cancelPerson(p, 1000, "no_show");
            return null;
          });
    }
  }
}
