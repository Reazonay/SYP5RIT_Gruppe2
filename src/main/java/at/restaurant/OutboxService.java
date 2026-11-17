package at.restaurant;

import jakarta.mail.internet.MimeMessage;
import java.time.Clock;
import java.util.regex.Pattern;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {
  private static final Pattern LINK = Pattern.compile("https?://[^\\s]+/#guest=[a-f0-9]{64}");
  private final Db db;
  private final JavaMailSender sender;
  private final QrCodes qr;
  private final AppSettings settings;
  private final Clock clock;

  public OutboxService(
      Db db, JavaMailSender sender, QrCodes qr, AppSettings settings, Clock clock) {
    this.db = db;
    this.sender = sender;
    this.qr = qr;
    this.settings = settings;
    this.clock = clock;
  }

  public void deliver() {
    db.tx(
        () -> {
          for (var row :
              db.rows(
                  "SELECT * FROM outbox WHERE sent_at IS NULL AND attempts<20 ORDER BY id LIMIT 10 FOR UPDATE SKIP LOCKED")) {
            try {
              MimeMessage message = sender.createMimeMessage();
              var helper = new MimeMessageHelper(message, true, "UTF-8");
              helper.setFrom(settings.smtpFrom());
              helper.setTo((String) row.get("recipient"));
              helper.setSubject((String) row.get("subject"));
              helper.setText((String) row.get("body"));
              var link = LINK.matcher((String) row.get("body"));
              if (link.find())
                helper.addAttachment(
                    "reservierung.png", new ByteArrayResource(qr.png(link.group())), "image/png");
              message.setHeader(
                  "Message-ID", "<restaurant-" + row.get("id") + "@restaurant.local>");
              sender.send(message);
              db.update(
                  "UPDATE outbox SET sent_at=?,body='[Zugestellt]' WHERE id=?",
                  clock.instant(),
                  row.get("id"));
            } catch (Exception e) {
              db.update("UPDATE outbox SET attempts=attempts+1 WHERE id=?", row.get("id"));
            }
          }
          return null;
        });
  }
}
