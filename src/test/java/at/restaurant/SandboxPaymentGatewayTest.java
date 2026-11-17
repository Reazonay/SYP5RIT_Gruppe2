package at.restaurant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SandboxPaymentGatewayTest {
  MockRestServiceServer server;
  SandboxPaymentGateway gateway;

  @BeforeEach
  void setup() {
    var builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    gateway = new SandboxPaymentGateway(settings("sk_test_example"), builder);
  }

  AppSettings settings(String stripe) {
    return new AppSettings(
        "http://localhost:3000", false, stripe, "client", "secret", "restaurant@example.test");
  }

  void auth() {
    server
        .expect(requestTo(SandboxPaymentGateway.PAYPAL + "/v1/oauth2/token"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Basic Y2xpZW50OnNlY3JldA=="))
        .andRespond(withSuccess("{\"access_token\":\"access\"}", MediaType.APPLICATION_JSON));
  }

  @AfterEach
  void verify() {
    server.verify();
  }

  @Test
  void liveStripeKeysAreRejected() {
    var live = new SandboxPaymentGateway(settings("sk_live_not_allowed"), RestClient.builder());
    assertThrows(ApiException.class, () -> live.validate("card"));
  }

  @Test
  void stripeCheckoutUsesThePersistentIdempotencyKey() {
    UUID key = UUID.randomUUID();
    server
        .expect(requestTo(SandboxPaymentGateway.STRIPE + "/checkout/sessions"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Idempotency-Key", key.toString()))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("unit_amount%5D=3000")))
        .andRespond(
            withSuccess(
                "{\"id\":\"cs_test_1\",\"url\":\"https://checkout.stripe.com/test\"}",
                MediaType.APPLICATION_JSON));
    assertEquals(
        "cs_test_1",
        gateway.checkout("card", 3000, key, "https://restaurant.example/#guest=secret").id());
  }

  @Test
  void stripePaidRequiresCorrectAmountAndCurrency() {
    server
        .expect(requestTo(SandboxPaymentGateway.STRIPE + "/checkout/sessions/cs_test_1"))
        .andRespond(
            withSuccess(
                "{\"payment_status\":\"paid\",\"amount_total\":100,\"currency\":\"eur\"}",
                MediaType.APPLICATION_JSON));
    assertEquals("pending", gateway.check("card", "cs_test_1", 3000, UUID.randomUUID()));
  }

  @Test
  void stripeSuccessfulPaymentIsConfirmed() {
    server
        .expect(requestTo(SandboxPaymentGateway.STRIPE + "/checkout/sessions/cs_test_1"))
        .andRespond(
            withSuccess(
                "{\"payment_status\":\"paid\",\"amount_total\":3000,\"currency\":\"eur\"}",
                MediaType.APPLICATION_JSON));
    assertEquals("paid", gateway.check("card", "cs_test_1", 3000, UUID.randomUUID()));
  }

  @Test
  void stripeExpiredSessionIsFailed() {
    server
        .expect(requestTo(SandboxPaymentGateway.STRIPE + "/checkout/sessions/cs_test_1"))
        .andRespond(withSuccess("{\"status\":\"expired\"}", MediaType.APPLICATION_JSON));
    assertEquals("failed", gateway.check("card", "cs_test_1", 3000, UUID.randomUUID()));
  }

  @Test
  void stripeRefundUsesPaymentIntentAndStableKey() {
    UUID key = UUID.randomUUID();
    server
        .expect(requestTo(SandboxPaymentGateway.STRIPE + "/checkout/sessions/cs_test_1"))
        .andRespond(withSuccess("{\"payment_intent\":\"pi_test_1\"}", MediaType.APPLICATION_JSON));
    server
        .expect(requestTo(SandboxPaymentGateway.STRIPE + "/refunds"))
        .andExpect(header("Idempotency-Key", key.toString()))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("payment_intent=pi_test_1")))
        .andRespond(
            withSuccess(
                "{\"id\":\"re_test_1\",\"status\":\"succeeded\"}", MediaType.APPLICATION_JSON));
    assertEquals("re_test_1", gateway.refund("card", "cs_test_1", 1000, key));
  }

  @Test
  void providerFailureNeverBecomesSuccess() {
    server
        .expect(requestTo(SandboxPaymentGateway.STRIPE + "/checkout/sessions/cs_test_1"))
        .andRespond(withServerError());
    assertEquals(
        503,
        assertThrows(
                ApiException.class,
                () -> gateway.check("card", "cs_test_1", 3000, UUID.randomUUID()))
            .status());
  }

  @Test
  void paypalCheckoutUsesOnlyTheSandboxAndPassesTheKey() {
    UUID key = UUID.randomUUID();
    auth();
    server
        .expect(requestTo(SandboxPaymentGateway.PAYPAL + "/v2/checkout/orders"))
        .andExpect(header("PayPal-Request-Id", key.toString()))
        .andExpect(
            content()
                .json(
                    "{\"intent\":\"CAPTURE\",\"purchase_units\":[{\"amount\":{\"currency_code\":\"EUR\",\"value\":\"30.00\"}}]}"))
        .andRespond(
            withSuccess(
                "{\"id\":\"ORDER\",\"links\":[{\"rel\":\"payer-action\",\"href\":\"https://www.sandbox.paypal.com/checkoutnow?token=ORDER\"}]}",
                MediaType.APPLICATION_JSON));
    assertEquals(
        "ORDER",
        gateway.checkout("paypal", 3000, key, "https://restaurant.example/#guest=secret").id());
  }

  @Test
  void paypalCapturesAnApprovedOrderAndChecksTheAmount() {
    UUID key = UUID.randomUUID();
    auth();
    server
        .expect(requestTo(SandboxPaymentGateway.PAYPAL + "/v2/checkout/orders/ORDER"))
        .andRespond(withSuccess("{\"status\":\"APPROVED\"}", MediaType.APPLICATION_JSON));
    auth();
    server
        .expect(requestTo(SandboxPaymentGateway.PAYPAL + "/v2/checkout/orders/ORDER/capture"))
        .andExpect(header("PayPal-Request-Id", key.toString()))
        .andRespond(
            withSuccess(
                "{\"status\":\"COMPLETED\",\"purchase_units\":[{\"payments\":{\"captures\":[{\"status\":\"COMPLETED\",\"amount\":{\"currency_code\":\"EUR\",\"value\":\"30.00\"}}]}}]}",
                MediaType.APPLICATION_JSON));
    assertEquals("paid", gateway.check("paypal", "ORDER", 3000, key));
  }

  @Test
  void paypalWrongCurrencyRemainsPending() {
    auth();
    server
        .expect(requestTo(SandboxPaymentGateway.PAYPAL + "/v2/checkout/orders/ORDER"))
        .andRespond(
            withSuccess(
                "{\"status\":\"COMPLETED\",\"purchase_units\":[{\"payments\":{\"captures\":[{\"status\":\"COMPLETED\",\"amount\":{\"currency_code\":\"USD\",\"value\":\"30.00\"}}]}}]}",
                MediaType.APPLICATION_JSON));
    assertEquals("pending", gateway.check("paypal", "ORDER", 3000, UUID.randomUUID()));
  }

  @Test
  void paypalRefundChecksTheProviderStatus() {
    UUID key = UUID.randomUUID();
    auth();
    server
        .expect(requestTo(SandboxPaymentGateway.PAYPAL + "/v2/checkout/orders/ORDER"))
        .andRespond(
            withSuccess(
                "{\"purchase_units\":[{\"payments\":{\"captures\":[{\"id\":\"CAPTURE\"}]}}]}",
                MediaType.APPLICATION_JSON));
    auth();
    server
        .expect(requestTo(SandboxPaymentGateway.PAYPAL + "/v2/payments/captures/CAPTURE/refund"))
        .andExpect(header("PayPal-Request-Id", key.toString()))
        .andRespond(
            withSuccess(
                "{\"id\":\"REFUND\",\"status\":\"COMPLETED\"}", MediaType.APPLICATION_JSON));
    assertEquals("REFUND", gateway.refund("paypal", "ORDER", 1000, key));
  }
}
