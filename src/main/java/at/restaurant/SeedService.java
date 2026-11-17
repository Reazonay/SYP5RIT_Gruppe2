package at.restaurant;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class SeedService {
  private final Db db;
  private final AuthService auth;

  public SeedService(Db db, AuthService auth) {
    this.db = db;
    this.auth = auth;
  }

  public void seed(String password) {
    if (password == null
        || password.length() < 14
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException(
          "SEED_PASSWORD must contain at least 14 characters and at most 72 UTF-8 bytes");
    String digest = auth.encode(password);
    db.tx(
        () -> {
          db.rows("SELECT pg_advisory_xact_lock(80403)");
          for (String role : List.of("manager", "waiter", "cook"))
            db.update(
                "INSERT INTO staff VALUES(?,?,?,?) ON CONFLICT(email) DO NOTHING",
                UUID.randomUUID(),
                role + "@restaurant.test",
                digest,
                role);
          for (int i = 1; i <= 30; i++)
            db.update(
                "INSERT INTO seats(label,table_name) VALUES(?,?) ON CONFLICT(label) DO NOTHING",
                "Platz " + i,
                "Tisch " + ((i - 1) / 10 + 1));
          if (Db.number(db.one("SELECT count(*) AS count FROM menu"), "count") == 0) {
            for (Object[] item :
                List.of(
                    new Object[] {"Tagessuppe", "food", 590},
                    new Object[] {"Gemüsecurry", "food", 1290},
                    new Object[] {"Salat", "food", 790},
                    new Object[] {"Apfelsaft", "drink", 350},
                    new Object[] {"Mineralwasser", "drink", 290}))
              db.update("INSERT INTO menu(name,category,price) VALUES(?,?,?)", item);
          }
          return null;
        });
  }
}
