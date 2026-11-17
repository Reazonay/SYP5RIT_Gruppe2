package at.restaurant;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class Db {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transactions;

  public Db(JdbcTemplate jdbc, org.springframework.transaction.PlatformTransactionManager manager) {
    this.jdbc = jdbc;
    this.transactions = new TransactionTemplate(manager);
  }

  public <T> T tx(Supplier<T> work) {
    return transactions.execute(status -> work.get());
  }

  public List<Map<String, Object>> rows(String sql, Object... args) {
    return jdbc.queryForList(sql, parameters(args));
  }

  public Map<String, Object> one(String sql, Object... args) {
    var values = rows(sql, args);
    if (values.isEmpty()) throw ApiException.notFound("Datensatz nicht gefunden");
    return values.get(0);
  }

  public Optional<Map<String, Object>> optional(String sql, Object... args) {
    return rows(sql, args).stream().findFirst();
  }

  public int update(String sql, Object... args) {
    return jdbc.update(sql, parameters(args));
  }

  private Object[] parameters(Object[] args) {
    return Arrays.stream(args).map(v -> v instanceof Instant i ? Timestamp.from(i) : v).toArray();
  }

  public static int number(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).intValue();
  }

  public static UUID id(Map<String, Object> row) {
    return (UUID) row.get("id");
  }

  public static Instant time(Map<String, Object> row, String key) {
    Object v = row.get(key);
    return v instanceof Timestamp t
        ? t.toInstant()
        : v instanceof OffsetDateTime o ? o.toInstant() : (Instant) v;
  }
}
