package at.restaurant;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RulesTest {
  @ParameterizedTest
  @CsvSource({
    "60,false,false,5000,0",
    "60,true,false,5000,0",
    "59,false,false,5000,1000",
    "10,true,true,100000,2000",
    "16,true,false,5000,1000",
    "15,true,false,5000,3750",
    "0,true,false,0,0",
    "5,true,false,999,749",
    "-1,true,false,1000,750"
  })
  void cancellationBoundaries(
      int minutes, boolean group, boolean whole, int subtotal, int expected) {
    assertEquals(
        expected, Rules.cancellationFee(Duration.ofMinutes(minutes), group, whole, subtotal));
  }

  @Test
  void checkInIncludesOpeningAndExcludesNoShowBoundary() {
    Instant start = Instant.parse("2030-01-01T12:00:00Z");
    assertTrue(Rules.canCheckIn("reserved", start.minusSeconds(1800), start));
    assertFalse(Rules.canCheckIn("reserved", start.minusSeconds(1801), start));
    assertTrue(Rules.canCheckIn("reserved", start.plusSeconds(899), start));
    assertFalse(Rules.canCheckIn("reserved", start.plusSeconds(900), start));
    assertFalse(Rules.canCheckIn("checked_in", start, start));
  }

  @Test
  void partialPaymentAndCancellationCredit() {
    assertEquals(1000, Rules.balance(3000, 0, 2000, false));
    assertEquals(-2000, Rules.balance(3000, 1000, 3000, true));
  }

  @Test
  void tokensHave256BitsAndAreNotStoredInPlaintext() {
    String a = Tokens.create(), b = Tokens.create();
    assertEquals(64, a.length());
    assertNotEquals(a, b);
    assertNotEquals(a, Tokens.hash(a));
    assertEquals(Tokens.hash(a), Tokens.hash(a));
    assertThrows(ApiException.class, () -> Tokens.validate("invalid"));
  }

  @Test
  void qrEncodesTheOriginalLink() throws Exception {
    String text = "https://restaurant.example/#guest=" + Tokens.create();
    var image =
        javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(new QrCodes().png(text)));
    var source = new com.google.zxing.client.j2se.BufferedImageLuminanceSource(image);
    String decoded =
        new com.google.zxing.MultiFormatReader()
            .decode(
                new com.google.zxing.BinaryBitmap(
                    new com.google.zxing.common.HybridBinarizer(source)))
            .getText();
    assertEquals(text, decoded);
  }
}
