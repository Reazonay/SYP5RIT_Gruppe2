package at.restaurant;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record AppSettings(
    @Value("${app.public-url}") String publicUrl,
    @Value("${app.production}") boolean production,
    @Value("${app.stripe-secret-key}") String stripeSecret,
    @Value("${app.paypal-client-id}") String paypalId,
    @Value("${app.paypal-client-secret}") String paypalSecret,
    @Value("${app.smtp-from}") String smtpFrom) {
  public AppSettings {
    URI url = URI.create(publicUrl);
    if (url.getHost() == null || (production && !"https".equals(url.getScheme())))
      throw new IllegalArgumentException(
          "PUBLIC_URL must be an absolute URL and use HTTPS in production");
    publicUrl = publicUrl.replaceAll("/+$", "");
  }

  public String origin() {
    URI uri = URI.create(publicUrl);
    return uri.getScheme() + "://" + uri.getRawAuthority();
  }
}
