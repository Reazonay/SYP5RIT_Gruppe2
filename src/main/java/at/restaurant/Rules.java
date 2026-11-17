package at.restaurant;

import java.time.Duration;
import java.time.Instant;

public final class Rules {
  private Rules() {}

  public static int cancellationFee(Duration before, boolean group, boolean whole, int subtotal) {
    if (before.compareTo(Duration.ofHours(1)) >= 0) return 0;
    if (group && whole) return 2000;
    if (group && before.compareTo(Duration.ofMinutes(15)) <= 0)
      return (int) Math.round(subtotal * .75);
    return 1000;
  }

  public static boolean canCheckIn(String status, Instant now, Instant start) {
    return "reserved".equals(status)
        && !now.isBefore(start.minus(Duration.ofMinutes(30)))
        && now.isBefore(start.plus(Duration.ofMinutes(15)));
  }

  public static int balance(int subtotal, int fee, int paid, boolean terminal) {
    return (terminal ? fee : subtotal + fee) - paid;
  }
}
