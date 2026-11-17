# Restaurant-Reservierungen

Eine Spring-Boot-Anwendung mit Spring MVC, Thymeleaf, Spring Security, JDBC und PostgreSQL. Oberfläche, Geschäftslogik, REST-API und Hintergrundaufgaben laufen gemeinsam in einem ausführbaren JAR.

## Starten

Voraussetzungen: Java 21+, Maven 3.9+ und PostgreSQL. Für lokale PostgreSQL- und E-Mail-Testdienste liegt Docker Compose bei.

Einmalig vorbereiten:

```bash
cp .env.example .env
# SEED_PASSWORD in .env durch ein eigenes Passwort ersetzen (14 Zeichen bis 72 UTF-8-Bytes).
docker compose up -d
bash scripts/mvn.sh -DskipTests package
```

Erster Start mit Beispieldaten und Personalzugängen:

```bash
java -jar target/restaurant.jar --app.seed=true
```

Danach genügt:

```bash
java -jar target/restaurant.jar
```

**Anwendung: http://localhost:3000** · Lokale E-Mails: http://localhost:8025

PostgreSQL muss beim Start erreichbar sein. Flyway richtet das Schema automatisch ein. Seed legt 30 Plätze an drei Tischen, fünf Speisekarteneinträge und die Konten `manager@restaurant.test`, `waiter@restaurant.test`, `cook@restaurant.test` an. Alle verwenden das beim ersten Seed gesetzte Passwort. Erneutes Seeden überschreibt bestehende Passwörter nicht.

Alternativ in IntelliJ die `pom.xml` im Projektstamm öffnen und `RestaurantApplication` starten. Für Änderungen ohne vorherigen JAR-Bau: `bash scripts/mvn.sh spring-boot:run`. Die `.env` wird aus dem Arbeitsverzeichnis gelesen. Der Maven-Helfer speichert Abhängigkeiten im ignorierten Projektcache `.cache/maven`.

## Bedienung

- **Reservieren:** Personenzahl auswählen und „Anzahl übernehmen“, danach Zeitpunkt und Kontaktdaten eingeben. Jede Person erhält einen eigenen QR-Code per E-Mail.
- **Mein Besuch:** QR-/Reservierungslink öffnen oder den Code eingeben; Bestellungen ansehen, bezahlen, stornieren oder Besuch beenden.
- **Speisekarte:** Speisen und Getränke ansehen; mit geöffneter Reservierung direkt vor- oder nachbestellen.
- **Gruppe:** Termin verschieben, Teilnahmezustände ansehen oder gesamte Reservierung stornieren.
- **Personal:** Rollenabhängige Platzübersicht, Check-in, Rechnung, Küche und Verwaltung.

Alle wesentlichen Abläufe sind normale HTML-Formulare und funktionieren ohne JavaScript. Die kleine Datei `site.js` unterstützt den Kamerascan, Bestätigungsdialoge und die Übernahme bestehender QR-Links mit URL-Fragment. Es gibt keinen separaten Frontend-Build oder Frontend-Server. Zeiten beziehen sich auf `APP_ZONE_ID` (standardmäßig `Europe/Vienna`).

Gastzugänge bleiben in der Serversitzung; sie werden nicht als versteckte Zugriffscodes in jedes Formular eingebettet. Nach Ablauf der Sitzung oder einem Serverneustart kann derselbe E-Mail-Link erneut geöffnet werden. Buchungs- und Zahlungsdaten bleiben in PostgreSQL gespeichert.

## Zahlungen

Barzahlungen und Barauszahlungen bestätigt das Personal. Alle Beträge werden intern als ganze Eurocent gespeichert. Kreditkarte und PayPal laufen ausschließlich im Testbetrieb:

- `STRIPE_SECRET_KEY`: Schlüssel mit Präfix `sk_test_`.
- `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET`: PayPal-Sandbox-Händlerkonto.

Checkout und Erstattung werden beim Anbieter durchgeführt. Die Rückleitungs-URL bestätigt keine Zahlung; der Server prüft Betrag, Währung und Anbieterstatus. Ausstehende Zahlungen werden regelmäßig und auf Wunsch manuell geprüft. Dauerhaft gespeicherte Auftragskennungen verhindern doppelte Buchungen bei Wiederholung nach Verbindungsfehlern.

Nach Storno zeigt die Rechnung Gebühren oder Guthaben. Onlineerstattungen startet „Erstattung anfordern“; Barauszahlungen bestätigt das Personal. Gebühren von Barzahlern bleiben als offene Forderung bestehen, bis eine Zahlung erfasst wurde. Echte externe Sandbox-Checkouts benötigen eigene Testkonten und eine abschließende Anbieterprüfung.

## Tests

```bash
# Regel-, QR- und Zahlungsanbieter-Vertragstests
bash scripts/mvn.sh test

# Echte PostgreSQL-, MVC-, Sicherheits-, E-Mail- und Migrationsprüfungen
bash scripts/test-db.sh

# Einmalig den Testbrowser installieren
bash scripts/mvn.sh -q test-compile exec:java \
  -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.classpathScope=test \
  '-Dexec.args=install chromium'

# Vollständige Browserabläufe gegen das einzelne Spring-JAR
bash scripts/test-browser.sh

# Java-Formatierung prüfen
bash scripts/mvn.sh spotless:check
```

DB- und Browsertestskripte legen eigene temporäre Datenbanken an und löschen ausschließlich diese. PostgreSQL aus `compose.yaml` muss dafür laufen. Der Browsertest benötigt einen freien Port 3000 und beendet seinen Testserver selbst. Sämtliche Tests sind Java: JUnit, MockMvc, MockRestServiceServer und Playwright für Java. Die CI prüft dieselben Abläufe. Performance mit echten Testpersonen und Kamerascan auf physischen Smartphones sind separate Abnahmeschritte.

## Bereitstellung mit HTTPS

`compose.production.yaml` enthält PostgreSQL, das Spring-JAR und Caddy als HTTPS-Proxy. In `.env` konfigurieren:

- `PUBLIC_HOST`: Domain mit passendem DNS-Eintrag.
- `PUBLIC_URL=https://...`: öffentlicher Ursprung für Links und Zugriffsschutz.
- `DB_PASSWORD`: langes, URL-taugliches Passwort, beispielsweise Hex-Zeichen.
- SMTP-Zugang: `SMTP_HOST`, `SMTP_PORT`, `SMTP_FROM`, gegebenenfalls `SMTP_USER`, `SMTP_PASSWORD`.

```bash
docker compose -f compose.production.yaml build
docker compose -f compose.production.yaml up -d db
docker compose -f compose.production.yaml run --rm app java -jar app.jar \
  --spring.main.web-application-type=none --app.jobs.enabled=false --app.command=seed
docker compose -f compose.production.yaml up -d
```

Caddy verwaltet HTTPS-Zertifikate. Im Produktionsmodus werden sichere Cookies und SMTP-TLS verlangt. Für implizites SMTP-TLS `SMTP_SSL=true` setzen. Eine alte Datenbank mit `migrations.name=001_initial` wird bei Flyway-Version 1 übernommen; unbekannte bestehende Schemas werden nicht automatisch übernommen. Datenbank-Backups sind unabhängig von Releases mit `pg_dump` einzurichten. Es wurde kein Produktionsserver veröffentlicht.

Weitere Details: [Architektur und Geschäftsregeln](docs/implementation.md).

## Gestaltung

Die Oberfläche folgt den mobilen Tavola-Entwürfen aus `stitch_restaurant_reservierung_ui_redesign.zip`: Cremeflächen, Terrakotta-Akzente, Plus Jakarta Sans, Formularkarten und eine untere Navigation auf Smartphones. Auf größeren Bildschirmen wechseln Navigation und Formularaufteilung in ein breiteres Layout. Schrift und Restaurantbild werden lokal aus dem JAR ausgeliefert; es gibt keine CDN- oder Frontend-Build-Abhängigkeit. Die Schriftlizenz liegt unter `src/main/resources/static/assets/OFL-jakarta.txt`. Beispielbestände, Allergene und Servicefunktionen der Designvorlage werden nicht als echte Daten dargestellt.
