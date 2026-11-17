package at.restaurant;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class QrCodes {
  public byte[] png(String text) {
    try {
      var output = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(
          new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 256, 256), "PNG", output);
      return output.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("QR generation failed", e);
    }
  }

  public String dataUrl(String text) {
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(png(text));
  }
}
