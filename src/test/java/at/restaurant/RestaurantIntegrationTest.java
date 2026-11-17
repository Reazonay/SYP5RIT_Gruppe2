package at.restaurant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "app.jobs.enabled=false",
      "app.seed=false",
      "app.public-url=http://localhost:3000"
    })
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RUN_DATABASE_TESTS", matches = "1")
class RestaurantIntegrationTest {
  static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");
  static final String ORIGIN = "http://localhost:3000";
  @Autowired Db db;
  @Autowired ReservationService reservations;
  @Autowired PaymentService payments;
  @Autowired AuthService auth;
  @Autowired OperationsService operations;
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired FakeGateway gateway;
  @Autowired OutboxService outbox;
  @Autowired CapturingMailSender mailSender;
  int menuId;

  static class CapturingMailSender extends org.springframework.mail.javamail.JavaMailSenderImpl {
    final java.util.List<jakarta.mail.internet.MimeMessage> messages = new java.util.ArrayList<>();
    boolean fail;

    @Override
    public void send(jakarta.mail.internet.MimeMessage message) {
      if (fail) throw new org.springframework.mail.MailSendException("Test failure");
      messages.add(message);
    }
  }

  @TestConfiguration
  static class Config {
    @Bean
    @Primary
    CapturingMailSender capturingMailSender() {
      return new CapturingMailSender();
    }

    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Bean
    @Primary
    FakeGateway fakeGateway() {
      return new FakeGateway();
    }
  }

  static class FakeGateway implements PaymentGateway {
    AtomicInteger checkouts = new AtomicInteger(),
        checks = new AtomicInteger(),
        refunds = new AtomicInteger();
    volatile boolean failCheckout, failRefund;
    volatile String checkedStatus = "paid";
    UUID refundKey;

    public void validate(String method) {}

    public Checkout checkout(String method, int amount, UUID key, String guestUrl) {
      checkouts.incrementAndGet();
      if (failCheckout) throw ApiException.unavailable("Network failure");
      return new Checkout("provider-" + key, "https://checkout.example/" + key);
    }

    public String check(String method, String id, int amount, UUID payment) {
      checks.incrementAndGet();
      return checkedStatus;
    }

    public String refund(String method, String id, int amount, UUID key) {
      refunds.incrementAndGet();
      if (refundKey != null) assertEquals(refundKey, key);
      refundKey = key;
      if (failRefund) throw ApiException.unavailable("Network failure");
      return "refund-" + key;
    }

    void reset() {
      checkouts.set(0);
      checks.set(0);
      refunds.set(0);
      failCheckout = false;
      failRefund = false;
      checkedStatus = "paid";
      refundKey = null;
    }
  }

  @BeforeEach
  void fixture() {
    String database = String.valueOf(db.one("SELECT current_database() AS name").get("name"));
    assertTrue(
        database.contains("_test"),
        "Integration tests require a dedicated restaurant_test database");
    db.update(
        "TRUNCATE feedback,outbox,refunds,payments,orders,allocations,participants,bookings,sessions,staff,menu,seats RESTART IDENTITY CASCADE");
    db.update("INSERT INTO seats(label,table_name) VALUES('Platz 1','Tisch 1')");
    menuId =
        Db.number(
            db.one(
                "INSERT INTO menu(name,category,price) VALUES('Testgericht','food',3000) RETURNING id"),
            "id");
    gateway.reset();
    mailSender.messages.clear();
    mailSender.fail = false;
  }

  Requests.Person person(String name) {
    return new Requests.Person(
        name, name.toLowerCase(Locale.ROOT) + "@example.test", "123456", "cash");
  }

  Map<String, Object> reserve(int minutes, Requests.Person... people) {
    return reservations.reserve(
        new Requests.Reservation(NOW.plusSeconds(minutes * 60L), 60, List.of(people)));
  }

  String access(Map<String, Object> booking) {
    return (String) booking.get("guestToken");
  }

  String organizer(Map<String, Object> booking) {
    return (String) booking.get("organizerToken");
  }

  UUID paymentId(Map<String, Object> payment) {
    return Db.id(payment);
  }

  int value(Map<String, Object> row, String key) {
    return Db.number(row, key);
  }

  Requests.Payment cash(int amount, UUID key) {
    return new Requests.Payment("cash", key, amount);
  }

  void addSecondSeat() {
    db.update("INSERT INTO seats(label,table_name) VALUES('Platz 2','Tisch 1')");
  }

  Cookie login(String role) throws Exception {
    String email = role + "@example.test", password = "integration-test-password";
    db.update(
        "INSERT INTO staff VALUES(?,?,?,?) ON CONFLICT(email) DO NOTHING",
        UUID.randomUUID(),
        email,
        auth.encode(password),
        role);
    var response =
        mvc.perform(
                post("/api/login")
                    .header("Origin", ORIGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(new Requests.Login(email, password))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    assertTrue(response.getHeader("Set-Cookie").contains("HttpOnly"));
    return new Cookie(
        "session",
        response.getHeader("Set-Cookie").split(";", 2)[0].substring("session=".length()));
  }

  @Test
  void concurrentBookingCannotClaimTheSameSeat() throws Exception {
    var executor = Executors.newFixedThreadPool(4);
    var ready = new CountDownLatch(1);
    var successes = new AtomicInteger();
    var failures = new AtomicInteger();
    try {
      var tasks = new ArrayList<Future<?>>();
      for (int i = 0; i < 4; i++)
        tasks.add(
            executor.submit(
                () -> {
                  try {
                    ready.await();
                    reserve(10, person("Guest"));
                    successes.incrementAndGet();
                  } catch (ApiException e) {
                    assertEquals(409, e.status());
                    failures.incrementAndGet();
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                  }
                }));
      ready.countDown();
      for (var task : tasks) task.get(10, TimeUnit.SECONDS);
      assertEquals(1, successes.get());
      assertEquals(3, failures.get());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void unknownTokenCannotReadReservation() {
    assertEquals(
        404, assertThrows(ApiException.class, () -> reservations.view("a".repeat(64))).status());
  }

  @Test
  void priceSnapshotAndAvailabilityAreEnforced() {
    var b = reserve(10, person("Guest"));
    reservations.order(access(b), new Requests.Order(menuId, 1));
    db.update("UPDATE menu SET price=5000,available=false WHERE id=?", menuId);
    assertEquals(3000, value(reservations.view(access(b)), "subtotal"));
    assertThrows(
        ApiException.class, () -> reservations.order(access(b), new Requests.Order(menuId, 1)));
  }

  @Test
  void cashIsIdempotentAndRequiresStaffEvenOnReplay() {
    var b = reserve(10, person("Guest"));
    reservations.order(access(b), new Requests.Order(menuId, 1));
    UUID key = UUID.randomUUID();
    assertThrows(ApiException.class, () -> payments.pay(access(b), cash(2000, key), false));
    var first = payments.pay(access(b), cash(2000, key), true);
    assertEquals(first.get("id"), payments.pay(access(b), cash(2000, key), true).get("id"));
    assertThrows(ApiException.class, () -> payments.pay(access(b), cash(2000, key), false));
    assertThrows(ApiException.class, () -> payments.pay(access(b), cash(1000, key), true));
    assertEquals(1000, value(reservations.view(access(b)), "due"));
  }

  @Test
  void checkInOnceAndStaffCanCloseAnUnpaidVisit() {
    var b = reserve(10, person("Guest"));
    reservations.order(access(b), new Requests.Order(menuId, 1));
    reservations.checkIn(access(b));
    assertThrows(ApiException.class, () -> reservations.checkIn(access(b)));
    assertThrows(ApiException.class, () -> reservations.cancel(access(b)));
    assertThrows(ApiException.class, () -> reservations.finish(access(b), false, null));
    reservations.finish(null, true, Db.id(reservations.view(access(b))));
    var view = reservations.view(access(b));
    assertEquals("ended", view.get("status"));
    assertEquals(false, view.get("usable"));
    assertEquals(3000, value(view, "due"));
    assertThrows(
        ApiException.class, () -> reservations.order(access(b), new Requests.Order(menuId, 1)));
  }

  @Test
  void databaseExclusionConstraintWorksIndependently() {
    var b = reserve(10, person("Guest"));
    var p = db.one("SELECT * FROM participants WHERE booking_id=?", b.get("id"));
    var allocation = db.one("SELECT * FROM allocations WHERE participant_id=?", Db.id(p));
    var failure =
        assertThrows(
            org.springframework.dao.DataAccessException.class,
            () ->
                db.tx(
                    () -> {
                      UUID id = UUID.randomUUID();
                      db.update(
                          "INSERT INTO participants(id,booking_id,token_hash,name,email,phone,method) VALUES(?,?,?,?,?,?,?)",
                          id,
                          b.get("id"),
                          Tokens.hash(Tokens.create()),
                          "Other",
                          "other@example.test",
                          "123",
                          "cash");
                      db.update(
                          "INSERT INTO allocations VALUES(?,?,?::tstzrange,true)",
                          id,
                          allocation.get("seat_id"),
                          allocation.get("period").toString());
                      return null;
                    }));
    assertEquals("23P01", ((java.sql.SQLException) failure.getMostSpecificCause()).getSQLState());
  }

  @Test
  void groupCancellationReleasesSeatsAndPreservesRefundBalance() {
    addSecondSeat();
    var b = reserve(10, person("Organizer"), person("Second"));
    reservations.order(access(b), new Requests.Order(menuId, 1));
    var payment = payments.pay(access(b), cash(3000, UUID.randomUUID()), true);
    assertEquals(2000, value(reservations.cancelGroup(organizer(b)), "fee"));
    var view = reservations.view(access(b));
    assertEquals(0, value(view, "subtotal"));
    assertEquals(2000, value(view, "fee"));
    assertEquals(-1000, value(view, "due"));
    payments.refund(access(b), paymentId(payment), true);
    assertEquals(0, value(reservations.view(access(b)), "due"));
    assertThrows(ApiException.class, () -> payments.refund(access(b), paymentId(payment), true));
    assertThrows(ApiException.class, () -> reservations.checkIn(access(b)));
    assertEquals(
        0, value(db.one("SELECT count(*) AS count FROM allocations WHERE active"), "count"));
  }

  @Test
  void failedShiftRollsBackTheOldAllocation() {
    var b = reserve(180, person("Guest"));
    reserve(10, person("Other"));
    var before = reservations.view(access(b));
    assertThrows(ApiException.class, () -> reservations.shift(organizer(b), NOW.plusSeconds(600)));
    var after = reservations.view(access(b));
    assertEquals(before.get("starts_at"), after.get("starts_at"));
    assertEquals(
        true,
        db.one("SELECT active FROM allocations WHERE participant_id=?", Db.id(after))
            .get("active"));
  }

  @Test
  void successfulShiftKeepsTokensAndDuration() {
    var b = reserve(180, person("Guest"));
    Instant target = NOW.plusSeconds(36000);
    reservations.shift(organizer(b), target);
    var view = reservations.view(access(b));
    assertEquals(target, Db.time(view, "starts_at"));
    assertEquals(target.plusSeconds(3600), Db.time(view, "ends_at"));
  }

  @Test
  void providerPaymentAndRefundAreRecordedOnce() {
    var b = reserve(180, person("Guest"));
    reservations.order(access(b), new Requests.Order(menuId, 1));
    var input = new Requests.Payment("paypal", UUID.randomUUID(), 3000);
    var p = payments.pay(access(b), input, false);
    assertEquals(p.get("id"), payments.pay(access(b), input, false).get("id"));
    assertEquals(1, gateway.checkouts.get());
    payments.reconcile(access(b), Db.id(p));
    payments.reconcile(access(b), Db.id(p));
    assertEquals(1, gateway.checks.get());
    reservations.cancel(access(b));
    payments.refund(access(b), Db.id(p), false);
    assertEquals(1, gateway.refunds.get());
    assertThrows(ApiException.class, () -> payments.refund(access(b), Db.id(p), false));
    assertEquals(0, value(reservations.view(access(b)), "due"));
  }

  @Test
  void interruptedCheckoutRetainsTheRequestKey() {
    var b = reserve(180, person("Guest"));
    reservations.order(access(b), new Requests.Order(menuId, 1));
    var request = new Requests.Payment("card", UUID.randomUUID(), 3000);
    gateway.failCheckout = true;
    assertThrows(ApiException.class, () -> payments.pay(access(b), request, false));
    assertEquals(
        1,
        value(
            db.one(
                "SELECT count(*) AS count FROM payments WHERE request_key=?", request.requestKey()),
            "count"));
    gateway.failCheckout = false;
    var p = payments.pay(access(b), request, false);
    assertNotNull(p.get("provider_id"));
    assertEquals(1, value(db.one("SELECT count(*) AS count FROM payments"), "count"));
  }

  @Test
  void interruptedRefundRetainsTheProviderIdempotencyKey() {
    var b = reserve(180, person("Guest"));
    reservations.order(access(b), new Requests.Order(menuId, 1));
    var p = payments.pay(access(b), new Requests.Payment("paypal", UUID.randomUUID(), 3000), false);
    payments.reconcile(access(b), Db.id(p));
    reservations.cancel(access(b));
    gateway.failRefund = true;
    assertThrows(ApiException.class, () -> payments.refund(access(b), Db.id(p), false));
    assertEquals(
        0, value(db.one("SELECT refunded FROM payments WHERE id=?", Db.id(p)), "refunded"));
    gateway.failRefund = false;
    payments.refund(access(b), Db.id(p), false);
    assertEquals(1, value(db.one("SELECT count(*) AS count FROM refunds"), "count"));
    assertEquals(0, value(reservations.view(access(b)), "due"));
  }

  @Test
  void noShowJobChargesOnceAndCancelsOrders() {
    var b = reserve(10, person("Guest"));
    reservations.order(access(b), new Requests.Order(menuId, 1));
    db.update(
        "UPDATE bookings SET starts_at=?,ends_at=? WHERE id=?",
        NOW.minusSeconds(960),
        NOW.plusSeconds(2640),
        b.get("id"));
    reservations.expireNoShows();
    reservations.expireNoShows();
    var view = reservations.view(access(b));
    assertEquals("no_show", view.get("status"));
    assertEquals(1000, value(view, "fee"));
    assertEquals(0, value(view, "subtotal"));
    assertEquals(false, view.get("usable"));
  }

  @Test
  void cleaningBufferIsHalfOpen() {
    var b = reserve(60, person("Guest"));
    assertThrows(ApiException.class, () -> reserve(149, person("Other")));
    var next = reserve(150, person("Next"));
    assertNotNull(next.get("id"));
  }

  @Test
  void kitchenTransitionsAndFeedback() {
    var b = reserve(10, person("Guest"));
    reservations.order(access(b), new Requests.Order(menuId, 1));
    UUID order = Db.id(db.one("SELECT id FROM orders"));
    assertThrows(ApiException.class, () -> operations.orderStatus(order, "ready", "cook"));
    operations.orderStatus(order, "preparing", "cook");
    operations.orderStatus(order, "ready", "cook");
    assertThrows(ApiException.class, () -> operations.orderStatus(order, "served", "cook"));
    operations.orderStatus(order, "served", "waiter");
    assertTrue(operations.kitchen().isEmpty());
    assertThrows(
        ApiException.class,
        () -> reservations.feedback(access(b), new Requests.Feedback(5, "Good")));
    reservations.checkIn(access(b));
    reservations.finish(access(b), true, null);
    reservations.feedback(access(b), new Requests.Feedback(5, "Good"));
    assertThrows(
        org.springframework.dao.DataAccessException.class,
        () -> reservations.feedback(access(b), new Requests.Feedback(5, "Again")));
  }

  @Test
  void anonymousStaffAccessIsRejected() throws Exception {
    mvc.perform(get("/api/staff/seats").param("at", NOW.toString()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void crossOriginWritesAreRejected() throws Exception {
    mvc.perform(
            post("/api/reservations")
                .header("Origin", "https://other.example")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidInputAndMalformedIdsAreRejected() throws Exception {
    mvc.perform(
            post("/api/reservations")
                .header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/reservation")).andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/payments/not-a-uuid/check")
                .header("Origin", ORIGIN)
                .header("X-Reservation-Token", "a".repeat(64)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rolesAreCheckedOnDirectHttpCallsAndLogoutRevokesSession() throws Exception {
    for (String role : List.of("cook", "waiter", "manager")) {
      Cookie cookie = login(role);
      mvc.perform(get("/api/staff/me").cookie(cookie))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.role").value(role));
      mvc.perform(get("/api/staff/seats").param("at", NOW.toString()).cookie(cookie))
          .andExpect(status().is("cook".equals(role) ? 403 : 200));
      mvc.perform(
              post("/api/staff/menu")
                  .header("Origin", ORIGIN)
                  .cookie(cookie)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      json.writeValueAsString(new Requests.Menu("HTTP test", "food", 100, true))))
          .andExpect(status().is("manager".equals(role) ? 201 : 403));
      mvc.perform(post("/api/logout").header("Origin", ORIGIN).cookie(cookie))
          .andExpect(status().isCreated());
      mvc.perform(get("/api/staff/me").cookie(cookie)).andExpect(status().isUnauthorized());
    }
  }

  @Test
  void storedRecordsAreVisibleThroughHttpAndQrIsRevoked() throws Exception {
    var b = reserve(10, person("Guest"));
    mvc.perform(get("/api/reservation").header("X-Reservation-Token", access(b)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.starts_at").isString())
        .andExpect(jsonPath("$.token_hash").doesNotExist());
    mvc.perform(get("/api/qr").header("X-Reservation-Token", access(b)))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.image").value(org.hamcrest.Matchers.startsWith("data:image/png;base64,")));
    reservations.cancel(access(b));
    mvc.perform(get("/api/qr").header("X-Reservation-Token", access(b)))
        .andExpect(status().isConflict());
  }

  @Test
  void qrEmailIsDeliveredOnceAndSecretsAreRedacted() throws Exception {
    var b = reserve(180, person("Guest"));
    outbox.deliver();
    outbox.deliver();
    assertEquals(1, mailSender.messages.size());
    var message = mailSender.messages.get(0);
    assertEquals("Reservierungsbestätigung", message.getSubject());
    var multipart = (jakarta.mail.Multipart) message.getContent();
    assertTrue(multipart.getCount() >= 2);
    assertEquals("[Zugestellt]", db.one("SELECT body FROM outbox").get("body"));
    assertNotNull(db.one("SELECT sent_at FROM outbox").get("sent_at"));
  }

  @Test
  void failedEmailRetainsTheMessageForRetry() {
    reserve(180, person("Guest"));
    mailSender.fail = true;
    outbox.deliver();
    assertEquals(1, value(db.one("SELECT attempts FROM outbox"), "attempts"));
    assertNull(db.one("SELECT sent_at FROM outbox").get("sent_at"));
    mailSender.fail = false;
    outbox.deliver();
    assertEquals(1, mailSender.messages.size());
  }

  @Test
  void fractionalAndStringDurationsAreRejected() throws Exception {
    for (Object duration : java.util.List.of(60.5, "60")) {
      var body =
          java.util.Map.of(
              "startsAt",
              NOW.plusSeconds(600).toString(),
              "duration",
              duration,
              "people",
              java.util.List.of(person("Guest")));
      mvc.perform(
              post("/api/reservations")
                  .header("Origin", ORIGIN)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsString(body)))
          .andExpect(status().isBadRequest());
    }
  }

  @Test
  void unknownRoutesAndUnsupportedMethodsKeepTheirHttpStatus() throws Exception {
    mvc.perform(get("/api/missing-route")).andExpect(status().isNotFound());
    mvc.perform(post("/api/menu").header("Origin", ORIGIN))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  void legacyBcryptHashesAndExistingSessionHashesRemainValid() {
    String password = "legacy-test-password";
    String digest = auth.encode(password).replace("$2a$", "$2b$");
    db.update(
        "INSERT INTO staff VALUES(?,?,?,?)",
        java.util.UUID.randomUUID(),
        "legacy@example.test",
        digest,
        "waiter");
    var login = auth.login(new Requests.Login("legacy@example.test", password));
    assertTrue(auth.authenticate(login.get("session")).isPresent());
    assertEquals(
        Tokens.hash(login.get("session")),
        db.one("SELECT token_hash FROM sessions").get("token_hash"));
    db.update("UPDATE sessions SET expires_at=?", NOW.minusSeconds(1));
    assertTrue(auth.authenticate(login.get("session")).isEmpty());
  }

  @Test
  void serverRenderedHomeContainsFormsAndCsrfToken() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("reserve"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Platz reservieren")));
  }

  @Test
  void nativeFormsRejectMissingCsrfTokens() throws Exception {
    mvc.perform(post("/reserve").header("Origin", ORIGIN)).andExpect(status().isForbidden());
  }

  @Test
  void nativeReservationUsesServerSessionAndPostRedirectGet() throws Exception {
    var response =
        mvc.perform(
                post("/reserve")
                    .with(
                        org.springframework.security.test.web.servlet.request
                            .SecurityMockMvcRequestPostProcessors.csrf())
                    .header("Origin", ORIGIN)
                    .param("startsAt", "2030-01-01T13:10")
                    .param("duration", "60")
                    .param("people[0].name", "Formgast")
                    .param("people[0].email", "form@example.test")
                    .param("people[0].phone", "12345")
                    .param("people[0].method", "cash"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/visit"))
            .andReturn();
    var session =
        (org.springframework.mock.web.MockHttpSession) response.getRequest().getSession(false);
    assertNotNull(session.getAttribute(PageSupport.GUEST));
    mvc.perform(get("/visit").session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Formgast")));
  }

  @Test
  void staffPagesRequireLoginAndEnforceRoles() throws Exception {
    mvc.perform(get("/staff"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/login"));
    Cookie cook = login("cook");
    mvc.perform(get("/staff/kitchen").cookie(cook)).andExpect(status().isOk());
    mvc.perform(get("/staff/manage").cookie(cook)).andExpect(status().isForbidden());
  }

  private String formCsrf(String html) {
    var matcher =
        java.util.regex.Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"")
            .matcher(html);
    assertTrue(matcher.find());
    return matcher.group(1);
  }

  @Test
  void staffCookieRequestsDoNotInvalidateNativeFormCsrfTokens() throws Exception {
    var booking = reserve(10, person("NativeStaffGuest"));
    var home = mvc.perform(get("/")).andExpect(status().isOk()).andReturn();
    var session =
        (org.springframework.mock.web.MockHttpSession) home.getRequest().getSession(false);
    String oldId = session.getId();
    session.setAttribute(PageSupport.GUEST, access(booking));
    String password = "native-staff-password";
    db.update(
        "INSERT INTO staff VALUES(?,?,?,?)",
        UUID.randomUUID(),
        "nativewaiter@example.test",
        auth.encode(password),
        "waiter");
    var login =
        mvc.perform(
                post("/staff/login")
                    .session(session)
                    .header("Origin", ORIGIN)
                    .param("_csrf", formCsrf(home.getResponse().getContentAsString()))
                    .param("email", "nativewaiter@example.test")
                    .param("password", password))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    assertNotEquals(oldId, session.getId());
    assertEquals(access(booking), session.getAttribute(PageSupport.GUEST));
    Cookie cookie =
        new Cookie(
            "session",
            login
                .getResponse()
                .getHeader("Set-Cookie")
                .split(";", 2)[0]
                .substring("session=".length()));
    var visit =
        mvc.perform(get("/staff/visit").session(session).cookie(cookie))
            .andExpect(status().isOk())
            .andReturn();
    String token = formCsrf(visit.getResponse().getContentAsString());
    String stableId = session.getId();
    mvc.perform(get("/site.css").session(session).cookie(cookie)).andExpect(status().isOk());
    assertEquals(stableId, session.getId());
    mvc.perform(
            post("/staff/access")
                .session(session)
                .cookie(cookie)
                .header("Origin", ORIGIN)
                .param("_csrf", token)
                .param("access", access(booking)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/staff/visit"));
  }

  @Test
  void publicMenuRendersRealDishesWithoutOrderingForAnonymousGuests() throws Exception {
    mvc.perform(get("/menu"))
        .andExpect(status().isOk())
        .andExpect(view().name("menu"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Testgericht")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("class=\"inline order-form\""))));
  }
}
