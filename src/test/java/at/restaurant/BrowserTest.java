package at.restaurant;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "RUN_BROWSER_TESTS", matches = "1")
class BrowserTest {
  Playwright playwright;
  Browser browser;
  BrowserContext context;
  Page page;

  @BeforeEach
  void browser() {
    playwright =
        Playwright.create(
            new Playwright.CreateOptions()
                .setEnv(java.util.Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
    browser = playwright.chromium().launch();
    context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 800));
    page = context.newPage();
    page.setDefaultTimeout(8000);
    page.navigate("http://localhost:3000");
  }

  @AfterEach
  void close(org.junit.jupiter.api.TestInfo info) throws Exception {
    if (page != null && !page.isClosed()) {
      var folder = java.nio.file.Path.of("target/browser");
      java.nio.file.Files.createDirectories(folder);
      String name = info.getTestMethod().orElseThrow().getName();
      java.nio.file.Files.writeString(folder.resolve(name + ".html"), page.content());
      page.screenshot(
          new Page.ScreenshotOptions().setPath(folder.resolve(name + ".png")).setFullPage(true));
    }
    if (context != null) context.close();
    if (browser != null) browser.close();
    if (playwright != null) playwright.close();
  }

  Locator link(String text) {
    return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(text).setExact(true));
  }

  String qrAccess(String data) {
    try {
      var image =
          javax.imageio.ImageIO.read(
              new java.io.ByteArrayInputStream(
                  java.util.Base64.getDecoder().decode(data.split(",", 2)[1])));
      var source = new com.google.zxing.client.j2se.BufferedImageLuminanceSource(image);
      String value =
          new com.google.zxing.MultiFormatReader()
              .decode(
                  new com.google.zxing.BinaryBitmap(
                      new com.google.zxing.common.HybridBinarizer(source)))
              .getText();
      return value.split("#guest=", 2)[1];
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  Locator button(String text) {
    return page.getByRole(
        AriaRole.BUTTON, new Page.GetByRoleOptions().setName(text).setExact(true));
  }

  Locator label(String text) {
    return page.getByLabel(text, new Page.GetByLabelOptions().setExact(true));
  }

  String start(int minutes) {
    return LocalDateTime.now()
        .plusMinutes(minutes)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
  }

  void person(int index, String name, String email) {
    label("Name").nth(index).fill(name);
    label("E-Mail").nth(index).fill(email);
    label("Telefon").nth(index).fill("0123456789");
  }

  @Test
  void mobileReservationPreorderCashCheckoutAndFeedback() throws Exception {
    page.setViewportSize(360, 800);
    assertThat(
            page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Platz reservieren")))
        .isVisible();
    assertEquals(Boolean.TRUE, page.evaluate("document.documentElement.scrollWidth <= innerWidth"));
    label("Beginn").fill(start(5));
    person(0, "Browsergast", "browser@example.test");
    button("Verbindlich reservieren").click();
    assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Browsergast")))
        .isVisible();
    assertThat(page.getByAltText("QR-Code für den Check-in")).isVisible();
    String data = page.getByAltText("QR-Code für den Check-in").getAttribute("src");
    String access = qrAccess(data);
    assertEquals(64, access.length());
    link("Speisekarte").click();
    page.locator(".menu li")
        .filter(new Locator.FilterOptions().setHasText("Gemüsecurry"))
        .getByRole(
            AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Bestellen").setExact(true))
        .click();
    assertThat(page.getByText("1 × Gemüsecurry")).isVisible();
    link("Personal").click();
    label("E-Mail").fill("waiter@restaurant.test");
    label("Passwort").fill(System.getenv("SEED_PASSWORD"));
    button("Anmelden").click();
    link("Check-in & Rechnung").click();
    label("Reservierungszugang").fill(access);
    button("Reservierung öffnen").click();
    button("Einchecken").click();
    assertThat(page.getByText("Eingecheckt", new Page.GetByTextOptions().setExact(true)))
        .isVisible();
    button("Barzahlung erfassen").click();
    assertThat(page.getByText(Pattern.compile("Offen:.*0,00"))).isVisible();
    link("Mein Besuch").click();
    link("Aktualisieren").click();
    button("Besuch beenden").click();
    assertThat(
            page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Feedback").setExact(true)))
        .isVisible();
    assertThat(page.getByAltText("QR-Code für den Check-in")).hasCount(0);
    label("Kommentar").fill("Hat funktioniert.");
    button("Feedback senden").click();
    assertThat(page.getByRole(AriaRole.STATUS)).containsText("Gespeichert.");
  }

  @Test
  void groupReservationAndCancellation() {
    label("Personen").fill("2");
    button("Anzahl übernehmen").click();
    label("Beginn").fill(start(180));
    person(0, "Gruppe 0", "group0@example.test");
    person(1, "Gruppe 1", "group1@example.test");
    button("Verbindlich reservieren").click();
    assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Gruppe 0")))
        .isVisible();
    link("Gruppe").click();
    assertThat(page.getByText("Gruppe 1 · Reserviert")).isVisible();
    page.onceDialog(Dialog::accept);
    button("Gesamte Reservierung stornieren").click();
    assertThat(page.getByText("Gruppe 1 · Storniert")).isVisible();
  }

  @Test
  void serverRenderedFormsWorkWithoutJavascript() {
    context.close();
    context =
        browser.newContext(
            new Browser.NewContextOptions().setJavaScriptEnabled(false).setViewportSize(360, 800));
    page = context.newPage();
    page.navigate("http://localhost:3000");
    label("Beginn").fill(start(15));
    person(0, "Ohne JavaScript", "nojs@example.test");
    button("Verbindlich reservieren").click();
    assertThat(
            page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Ohne JavaScript")))
        .isVisible();
    assertThat(page.getByAltText("QR-Code für den Check-in")).isVisible();
  }

  @Test
  void managerCanManageMenuAndCookCannotOpenManagement() {
    link("Personal").click();
    label("E-Mail").fill("cook@restaurant.test");
    label("Passwort").fill(System.getenv("SEED_PASSWORD"));
    button("Anmelden").click();
    assertThat(
            page.getByRole(
                AriaRole.HEADING, new Page.GetByRoleOptions().setName("Offene Bestellungen")))
        .isVisible();
    assertEquals(403, page.navigate("http://localhost:3000/staff/manage").status());
    page.navigate("http://localhost:3000/staff/kitchen");
    button("Abmelden").click();
    label("E-Mail").fill("manager@restaurant.test");
    label("Passwort").fill(System.getenv("SEED_PASSWORD"));
    button("Anmelden").click();
    link("Verwaltung").click();
    var form = page.locator("form[action='/staff/manage/menu']");
    form.getByLabel("Name", new Locator.GetByLabelOptions().setExact(true))
        .fill("Browsertestgericht");
    form.getByLabel("Preis in Euro").fill("8.90");
    form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Artikel hinzufügen"))
        .click();
    assertThat(page.locator("input[value='Browsertestgericht']")).isVisible();
  }

  @Test
  void stitchLayoutsFitMobileTabletAndDesktop() {
    for (int width : new int[] {360, 768, 1280}) {
      page.setViewportSize(width, 900);
      page.navigate("http://localhost:3000/");
      assertEquals(
          Boolean.TRUE, page.evaluate("document.documentElement.scrollWidth <= innerWidth"));
      assertThat(
              page.getByRole(
                  AriaRole.BUTTON,
                  new Page.GetByRoleOptions().setName("Verbindlich reservieren").setExact(true)))
          .isVisible();
      page.navigate("http://localhost:3000/menu");
      assertThat(
              page.getByRole(
                  AriaRole.HEADING,
                  new Page.GetByRoleOptions().setName("Speise- und Getränkekarte")))
          .isVisible();
      assertEquals(
          Boolean.TRUE, page.evaluate("document.documentElement.scrollWidth <= innerWidth"));
    }
    page.setViewportSize(360, 900);
    page.navigate("http://localhost:3000/");
    page.evaluate("() => document.fonts.ready.then(() => true)");
    var folder = java.nio.file.Path.of("target/browser");
    try {
      java.nio.file.Files.createDirectories(folder);
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
    page.screenshot(
        new Page.ScreenshotOptions()
            .setPath(folder.resolve("reserve-mobile.png"))
            .setFullPage(true));
    page.navigate("http://localhost:3000/menu");
    page.screenshot(
        new Page.ScreenshotOptions().setPath(folder.resolve("menu-mobile.png")).setFullPage(true));
  }
}
