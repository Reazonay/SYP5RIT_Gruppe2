package at.restaurant;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class OperationsService {
  private final Db db;

  public OperationsService(Db db) {
    this.db = db;
  }

  public List<Map<String, Object>> menu() {
    return db.rows("SELECT * FROM menu ORDER BY category,id");
  }

  public List<Map<String, Object>> seats(Instant at) {
    return db.rows(
        "SELECT s.*,CASE WHEN EXISTS(SELECT 1 FROM allocations a JOIN participants p ON p.id=a.participant_id WHERE a.seat_id=s.id AND p.status='checked_in') THEN 'occupied' WHEN EXISTS(SELECT 1 FROM allocations a WHERE a.seat_id=s.id AND a.active AND a.period @> ?::timestamptz) THEN 'reserved' ELSE 'free' END status FROM seats s ORDER BY table_name,id",
        at);
  }

  public List<Map<String, Object>> visits() {
    return db.rows(
        "SELECT p.id,p.name,p.status,p.checked_in_at,b.starts_at,s.label,p.status='checked_in' AND p.checked_in_at<=now()-interval '2 hours' AS overdue FROM participants p JOIN bookings b ON b.id=p.booking_id JOIN allocations a ON a.participant_id=p.id JOIN seats s ON s.id=a.seat_id WHERE b.starts_at>=now()-interval '1 day' OR p.status='checked_in' ORDER BY b.starts_at");
  }

  public List<Map<String, Object>> kitchen() {
    return db.rows(
        "SELECT o.*,p.name guest,b.starts_at,s.label FROM orders o JOIN participants p ON p.id=o.participant_id JOIN bookings b ON b.id=p.booking_id JOIN allocations a ON a.participant_id=p.id JOIN seats s ON s.id=a.seat_id WHERE o.status NOT IN ('cancelled','served') ORDER BY b.starts_at,o.created_at");
  }

  public void orderStatus(UUID id, String status, String role) {
    if (("cook".equals(role) && "served".equals(status))
        || ("waiter".equals(role) && !"served".equals(status)))
      throw new ApiException(403, "Statuswechsel nicht erlaubt");
    String previous =
        Map.of("preparing", "open", "ready", "preparing", "served", "ready").get(status);
    if (db.update("UPDATE orders SET status=? WHERE id=? AND status=?", status, id, previous) == 0)
      throw ApiException.conflict("Bestellstatus hat sich geändert");
  }

  public Map<String, Object> addMenu(Requests.Menu input) {
    return db.one(
        "INSERT INTO menu(name,category,price,available) VALUES(?,?,?,?) RETURNING *",
        input.name().trim(),
        input.category(),
        input.price(),
        input.available());
  }

  public Map<String, Object> editMenu(int id, Requests.Menu input) {
    return db.one(
        "UPDATE menu SET name=?,category=?,price=?,available=? WHERE id=? RETURNING *",
        input.name().trim(),
        input.category(),
        input.price(),
        input.available(),
        id);
  }

  public Map<String, Object> addSeat(Requests.Seat input) {
    return db.one(
        "INSERT INTO seats(label,table_name) VALUES(?,?) RETURNING *",
        input.label().trim(),
        input.tableName().trim());
  }

  public Map<String, Object> seatActive(int id, boolean active) {
    return db.tx(
        () -> {
          db.rows("SELECT pg_advisory_xact_lock(80402)");
          if (!active
              && !db.rows(
                      "SELECT 1 FROM allocations WHERE seat_id=? AND active AND upper(period)>now()",
                      id)
                  .isEmpty()) throw ApiException.conflict("Platz hat noch Reservierungen");
          return db.one("UPDATE seats SET active=? WHERE id=? RETURNING *", active, id);
        });
  }
}
