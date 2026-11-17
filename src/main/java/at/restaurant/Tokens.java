package at.restaurant;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

public final class Tokens {
  private static final SecureRandom RANDOM = new SecureRandom();

  private Tokens() {}

  public static String create() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String validate(String value) {
    if (value == null || !value.matches("[a-f0-9]{64}"))
      throw new ApiException(400, "Zugriffscode ungültig");
    return value;
  }
}
