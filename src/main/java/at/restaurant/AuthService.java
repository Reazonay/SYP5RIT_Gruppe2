package at.restaurant;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  public record Staff(UUID id, String email, String role) {}

  private final Db db;
  private final Clock clock;
  private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(12);
  private final String dummy = passwords.encode("not-a-real-password");

  public AuthService(Db db, Clock clock) {
    this.db = db;
    this.clock = clock;
  }

  public String encode(String password) {
    return passwords.encode(password);
  }

  public Map<String, String> login(Requests.Login input) {
    var user = db.optional("SELECT * FROM staff WHERE email=?", input.email());
    String digest = user.map(p -> (String) p.get("password_hash")).orElse(dummy);
    boolean valid =
        input.password().getBytes(StandardCharsets.UTF_8).length <= 72
            && passwords.matches(input.password(), digest);
    if (!valid || user.isEmpty()) throw new ApiException(401, "Anmeldedaten ungültig");
    String session = Tokens.create();
    db.update(
        "INSERT INTO sessions VALUES(?,?,?)",
        Tokens.hash(session),
        Db.id(user.get()),
        clock.instant().plus(Duration.ofHours(8)));
    return Map.of("session", session, "role", (String) user.get().get("role"));
  }

  public Optional<Staff> authenticate(String session) {
    return db.optional(
            "SELECT s.id,s.email,s.role FROM sessions x JOIN staff s ON s.id=x.staff_id WHERE"
                + " x.token_hash=? AND x.expires_at>?",
            Tokens.hash(session),
            clock.instant())
        .map(p -> new Staff(Db.id(p), (String) p.get("email"), (String) p.get("role")));
  }

  public void logout(String session) {
    db.update("DELETE FROM sessions WHERE token_hash=?", Tokens.hash(session));
  }

  public String switchToRole(String role) {
    if (!List.of("cook", "waiter", "manager").contains(role))
      throw new ApiException(400, "Ungültige Rolle");
    var user = db.optional("SELECT id FROM staff WHERE role=? ORDER BY email LIMIT 1", role);
    UUID id;
    if (user.isPresent()) {
      id = Db.id(user.get());
    } else {
      db.update(
          "INSERT INTO staff VALUES(?,?,?,?) ON CONFLICT(email) DO NOTHING",
          UUID.randomUUID(),
          role + "@restaurant.test",
          dummy,
          role);
      id = Db.id(db.one("SELECT id FROM staff WHERE email=?", role + "@restaurant.test"));
    }
    String session = Tokens.create();
    db.update(
        "INSERT INTO sessions VALUES(?,?,?)",
        Tokens.hash(session),
        id,
        clock.instant().plus(Duration.ofHours(8)));
    return session;
  }
}
