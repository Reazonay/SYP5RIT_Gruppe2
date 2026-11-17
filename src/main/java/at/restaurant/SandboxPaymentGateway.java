package at.restaurant;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.*;
import org.springframework.web.client.RestClient;

@Component
public class SandboxPaymentGateway implements PaymentGateway {
  static final String STRIPE = "https://api.stripe.com/v1";
  static final String PAYPAL = "https://api-m.sandbox.paypal.com";
  private final AppSettings settings;
  private final RestClient http;

  public SandboxPaymentGateway(AppSettings settings, RestClient.Builder builder) {
    this.settings = settings;
    this.http = builder.build();
  }

  public void validate(String method) {
    if ("card".equals(method) && !settings.stripeSecret().startsWith("sk_test_"))
      throw ApiException.unavailable("Stripe-Testzugang fehlt");
    if ("paypal".equals(method)
        && (settings.paypalId().isBlank() || settings.paypalSecret().isBlank()))
      throw ApiException.unavailable("PayPal-Testzugang fehlt");
  }

  private MultiValueMap<String, String> form(String... entries) {
    var values = new LinkedMultiValueMap<String, String>();
    for (int i = 0; i < entries.length; i += 2) values.add(entries[i], entries[i + 1]);
    return values;
  }

  private JsonNode stripe(String path, MultiValueMap<String, String> form, UUID key) {
    validate("card");
    try {
      RestClient.RequestBodySpec request =
          http.method(form == null ? HttpMethod.GET : HttpMethod.POST)
              .uri(STRIPE + path)
              .header("Authorization", "Bearer " + settings.stripeSecret());
      if (key != null) request.header("Idempotency-Key", key.toString());
      if (form != null) request.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form);
      return Objects.requireNonNull(request.retrieve().body(JsonNode.class));
    } catch (Exception e) {
      throw ApiException.unavailable("Stripe-Anfrage fehlgeschlagen");
    }
  }

  private JsonNode paypal(String path, Object body, UUID key) {
    validate("paypal");
    try {
      String credentials =
          Base64.getEncoder()
              .encodeToString(
                  (settings.paypalId() + ":" + settings.paypalSecret())
                      .getBytes(StandardCharsets.UTF_8));
      var auth =
          http.post()
              .uri(PAYPAL + "/v1/oauth2/token")
              .header("Authorization", "Basic " + credentials)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form("grant_type", "client_credentials"))
              .retrieve()
              .body(JsonNode.class);
      String access = Objects.requireNonNull(auth).path("access_token").asText();
      if (access.isEmpty()) throw new IllegalStateException();
      RestClient.RequestBodySpec request =
          http.method(body == null ? HttpMethod.GET : HttpMethod.POST)
              .uri(PAYPAL + path)
              .header("Authorization", "Bearer " + access);
      if (key != null) request.header("PayPal-Request-Id", key.toString());
      if (body != null) request.contentType(MediaType.APPLICATION_JSON).body(body);
      return Objects.requireNonNull(request.retrieve().body(JsonNode.class));
    } catch (Exception e) {
      throw ApiException.unavailable("PayPal-Anfrage fehlgeschlagen");
    }
  }

  private String required(JsonNode json, String key) {
    String value = json.path(key).asText();
    if (value.isBlank()) throw ApiException.unavailable("Ungültige Zahlungsanbieterantwort");
    return value;
  }

  private String euros(int cents) {
    return BigDecimal.valueOf(cents, 2).toPlainString();
  }

  public Checkout checkout(String method, int amount, UUID requestKey, String guestUrl) {
    if ("card".equals(method)) {
      var session =
          stripe(
              "/checkout/sessions",
              form(
                  "mode",
                  "payment",
                  "payment_method_types[0]",
                  "card",
                  "line_items[0][price_data][currency]",
                  "eur",
                  "line_items[0][price_data][unit_amount]",
                  Integer.toString(amount),
                  "line_items[0][price_data][product_data][name]",
                  "Restaurantrechnung",
                  "line_items[0][quantity]",
                  "1",
                  "success_url",
                  guestUrl,
                  "cancel_url",
                  guestUrl),
              requestKey);
      return new Checkout(required(session, "id"), required(session, "url"));
    }
    var order =
        paypal(
            "/v2/checkout/orders",
            Map.of(
                "intent",
                "CAPTURE",
                "purchase_units",
                List.of(Map.of("amount", Map.of("currency_code", "EUR", "value", euros(amount)))),
                "payment_source",
                Map.of(
                    "paypal",
                    Map.of(
                        "experience_context",
                        Map.of(
                            "return_url",
                            guestUrl,
                            "cancel_url",
                            guestUrl,
                            "user_action",
                            "PAY_NOW")))),
            requestKey);
    for (var link : order.path("links"))
      if (Set.of("approve", "payer-action").contains(link.path("rel").asText()))
        return new Checkout(required(order, "id"), required(link, "href"));
    throw ApiException.unavailable("PayPal-Zahlungslink fehlt");
  }

  public String check(String method, String providerId, int amount, UUID paymentId) {
    if ("card".equals(method)) {
      var session = stripe("/checkout/sessions/" + providerId, null, null);
      if ("paid".equals(session.path("payment_status").asText())
          && session.path("amount_total").asInt() == amount
          && "eur".equals(session.path("currency").asText())) return "paid";
      return "expired".equals(session.path("status").asText()) ? "failed" : "pending";
    }
    var order = paypal("/v2/checkout/orders/" + providerId, null, null);
    if ("APPROVED".equals(order.path("status").asText()))
      order = paypal("/v2/checkout/orders/" + providerId + "/capture", Map.of(), paymentId);
    for (var unit : order.path("purchase_units"))
      for (var capture : unit.path("payments").path("captures")) {
        if ("COMPLETED".equals(capture.path("status").asText())
            && "EUR".equals(capture.path("amount").path("currency_code").asText())
            && BigDecimal.valueOf(amount, 2)
                    .compareTo(new BigDecimal(capture.path("amount").path("value").asText("-1")))
                == 0) return "paid";
      }
    return "VOIDED".equals(order.path("status").asText()) ? "failed" : "pending";
  }

  public String refund(String method, String providerId, int amount, UUID refundId) {
    if ("card".equals(method)) {
      var session = stripe("/checkout/sessions/" + providerId, null, null);
      var response =
          stripe(
              "/refunds",
              form(
                  "payment_intent",
                  required(session, "payment_intent"),
                  "amount",
                  Integer.toString(amount)),
              refundId);
      if (!"succeeded".equals(response.path("status").asText()))
        throw ApiException.unavailable("Erstattung noch nicht abgeschlossen");
      return required(response, "id");
    }
    var order = paypal("/v2/checkout/orders/" + providerId, null, null);
    String capture =
        required(
            order.path("purchase_units").path(0).path("payments").path("captures").path(0), "id");
    var response =
        paypal(
            "/v2/payments/captures/" + capture + "/refund",
            Map.of("amount", Map.of("currency_code", "EUR", "value", euros(amount))),
            refundId);
    if (!"COMPLETED".equals(response.path("status").asText()))
      throw ApiException.unavailable("Erstattung noch nicht abgeschlossen");
    return required(response, "id");
  }
}
