package at.restaurant;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
  private final Db db;
  private final ReservationService reservations;
  private final PaymentGateway gateway;
  private final AppSettings settings;

  public PaymentService(
      Db db, ReservationService reservations, PaymentGateway gateway, AppSettings settings) {
    this.db = db;
    this.reservations = reservations;
    this.gateway = gateway;
    this.settings = settings;
  }

  public Map<String, Object> pay(String access, Requests.Payment input, boolean staff) {
    gateway.validate(input.method());
    var intent =
        db.tx(
            () -> {
              var p = reservations.lockGuest(access);
              // Authorization is also checked on replay, not only when inserting the payment.
              if ("cash".equals(input.method()) && !staff)
                throw ApiException.conflict("Barzahlung muss Personal bestätigen");
              var existing =
                  db.optional("SELECT * FROM payments WHERE request_key=?", input.requestKey());
              if (existing.isPresent()) {
                var old = existing.get();
                if (!p.get("id").equals(old.get("participant_id"))
                    || Db.number(old, "amount") != input.amount()
                    || !input.method().equals(old.get("method")))
                  throw ApiException.conflict("Zahlungsschlüssel bereits anderweitig verwendet");
                return old;
              }
              if (!db.rows(
                      "SELECT 1 FROM payments WHERE participant_id=? AND status='pending'",
                      Db.id(p))
                  .isEmpty()) throw ApiException.conflict("Vorherige Zahlung bitte zuerst prüfen");
              if (input.amount() > Db.number(reservations.totals(Db.id(p)), "due"))
                throw ApiException.conflict("Betrag übersteigt offenen Saldo");
              return db.one(
                  "INSERT INTO payments(id,participant_id,request_key,method,amount,status) VALUES(?,?,?,?,?,?) RETURNING *",
                  UUID.randomUUID(),
                  Db.id(p),
                  input.requestKey(),
                  input.method(),
                  input.amount(),
                  "cash".equals(input.method()) ? "paid" : "pending");
            });
    if (!"pending".equals(intent.get("status")) || intent.get("provider_id") != null) return intent;
    // The committed intent survives network failures and is reused by provider idempotency keys.
    return db.tx(
        () -> {
          var current = db.one("SELECT * FROM payments WHERE id=? FOR UPDATE", Db.id(intent));
          if (current.get("provider_id") != null || !"pending".equals(current.get("status")))
            return current;
          var checkout =
              gateway.checkout(
                  input.method(),
                  input.amount(),
                  input.requestKey(),
                  settings.publicUrl() + "/#guest=" + access);
          return db.one(
              "UPDATE payments SET provider_id=?,checkout_url=? WHERE id=? RETURNING *",
              checkout.id(),
              checkout.url(),
              Db.id(current));
        });
  }

  public Map<String, Object> reconcile(String access, UUID id) {
    return db.tx(
        () -> {
          var guest =
              access == null
                  ? reservations.lockParticipant(
                      (UUID)
                          db.one("SELECT participant_id FROM payments WHERE id=?", id)
                              .get("participant_id"))
                  : reservations.lockGuest(access);
          var p =
              db.one(
                  "SELECT * FROM payments WHERE id=? AND participant_id=? FOR UPDATE",
                  id,
                  Db.id(guest));
          if (!"pending".equals(p.get("status"))) return Map.of("status", p.get("status"));
          if (p.get("provider_id") == null)
            throw ApiException.conflict("Zahlungslink bitte zuerst wiederherstellen");
          String status =
              gateway.check(
                  (String) p.get("method"),
                  (String) p.get("provider_id"),
                  Db.number(p, "amount"),
                  Db.id(p));
          db.update("UPDATE payments SET status=? WHERE id=?", status, id);
          return Map.of("status", status);
        });
  }

  public Map<String, Object> refund(String access, UUID id, boolean staff) {
    var intent =
        db.tx(
            () -> {
              var guest = reservations.lockGuest(access);
              var p =
                  db.one(
                      "SELECT * FROM payments WHERE id=? AND participant_id=? FOR UPDATE",
                      id,
                      Db.id(guest));
              var totals = reservations.totals(Db.id(guest));
              if (!"paid".equals(p.get("status")) || Db.number(totals, "due") >= 0)
                throw ApiException.conflict("Keine Erstattung offen");
              if ("cash".equals(p.get("method")) && !staff)
                throw ApiException.conflict("Barauszahlung durch Personal erforderlich");
              var pending =
                  db.optional(
                      "SELECT r.* FROM refunds r JOIN payments p ON p.id=r.payment_id WHERE p.participant_id=? AND r.status='pending' FOR UPDATE OF r",
                      Db.id(guest));
              if (pending.isPresent()) {
                if (!id.equals(pending.get().get("payment_id")))
                  throw ApiException.conflict("Bitte vorherige Erstattung abschließen");
                return pending.get();
              }
              return db.one(
                  "INSERT INTO refunds(id,payment_id,amount) VALUES(?,?,?) RETURNING *",
                  UUID.randomUUID(),
                  id,
                  Math.min(
                      -Db.number(totals, "due"),
                      Db.number(p, "amount") - Db.number(p, "refunded")));
            });
    return db.tx(
        () -> {
          reservations.lockGuest(access);
          var r = db.one("SELECT * FROM refunds WHERE id=? FOR UPDATE", Db.id(intent));
          if ("done".equals(r.get("status"))) return Map.of("refunded", r.get("amount"));
          var p = db.one("SELECT * FROM payments WHERE id=? FOR UPDATE", id);
          String provider =
              "cash".equals(p.get("method"))
                  ? null
                  : gateway.refund(
                      (String) p.get("method"),
                      (String) p.get("provider_id"),
                      Db.number(r, "amount"),
                      Db.id(r));
          db.update(
              "UPDATE refunds SET status='done',provider_id=? WHERE id=?", provider, Db.id(r));
          db.update("UPDATE payments SET refunded=refunded+? WHERE id=?", r.get("amount"), id);
          return Map.of("refunded", r.get("amount"));
        });
  }

  public void reconcilePending() {
    for (var row :
        db.rows(
            "SELECT id FROM payments WHERE status='pending' AND provider_id IS NOT NULL ORDER BY created_at LIMIT 20")) {
      try {
        reconcile(null, Db.id(row));
      } catch (RuntimeException e) {
        org.slf4j.LoggerFactory.getLogger(getClass())
            .warn("Payment reconciliation deferred: {}", row.get("id"));
      }
    }
  }
}
