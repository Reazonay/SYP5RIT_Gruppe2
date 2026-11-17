package at.restaurant;

import java.math.*;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("format")
public class UiFormat {
  private final ZoneId zone;
  private static final Map<String, String> LABELS =
      Map.ofEntries(
          Map.entry("reserved", "Reserviert"),
          Map.entry("checked_in", "Eingecheckt"),
          Map.entry("ended", "Beendet"),
          Map.entry("cancelled", "Storniert"),
          Map.entry("no_show", "Nicht erschienen"),
          Map.entry("free", "Frei"),
          Map.entry("occupied", "Belegt"),
          Map.entry("open", "Offen"),
          Map.entry("preparing", "In Zubereitung"),
          Map.entry("ready", "Bereit"),
          Map.entry("served", "Serviert"),
          Map.entry("paid", "Bezahlt"),
          Map.entry("pending", "Ausständig"),
          Map.entry("failed", "Fehlgeschlagen"));

  public UiFormat(@Value("${app.zone-id}") String zone) {
    this.zone = ZoneId.of(zone);
  }

  public String money(Number cents) {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("de-AT"))
        .format(BigDecimal.valueOf(cents.longValue(), 2));
  }

  public String decimal(Number cents) {
    return BigDecimal.valueOf(cents.longValue(), 2).toPlainString();
  }

  public String status(String value) {
    return LABELS.getOrDefault(value, value);
  }

  public String date(Object value) {
    if (value == null) return "";
    Instant instant =
        value instanceof java.sql.Timestamp t
            ? t.toInstant()
            : value instanceof Instant i ? i : Instant.parse(value.toString());
    return DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm").withZone(zone).format(instant);
  }

  public Instant instant(String value) {
    try {
      return LocalDateTime.parse(value).atZone(zone).toInstant();
    } catch (java.time.DateTimeException e) {
      throw new ApiException(400, "Zeitpunkt ungültig");
    }
  }

  public String localNow() {
    return LocalDateTime.now(zone).withSecond(0).withNano(0).toString();
  }

  public int cents(BigDecimal value) {
    try {
      return value.movePointRight(2).intValueExact();
    } catch (ArithmeticException e) {
      throw new ApiException(400, "Betrag muss auf Cent genau sein");
    }
  }
}
