# Restaurant-Reservierungen

Web-Anwendung auf Grundlage von `Ausarbeitung.md`: Einzel- und Gruppenreservierung, persönliche QR-Codes, Vor- und Nachbestellungen, Küche, Check-in, Platzverwaltung, Barzahlung, Sandbox-Zahlungen, Gebühren, Rückerstattungen und Feedback.

## Lokal starten

Voraussetzungen: Node.js **24 oder neuer**, npm und Docker Compose. Die Anwendung bleibt in diesem Repository; es wurde kein externer Hosting-Dienst eingerichtet.

```bash
cp .env.example .env
# SEED_PASSWORD in .env durch ein eigenes Passwort mit mindestens 14 Zeichen ersetzen.
npm ci
docker compose up -d
npm run migrate -w apps/api
npm run seed -w apps/api
npm run dev:api
```

In einem zweiten Terminal:

```bash
npm run dev:web
```

- Anwendung: http://localhost:5173
- Lokale Test-E-Mails mit QR-Anhang: http://localhost:8025
- Personal: `manager@restaurant.test`, `waiter@restaurant.test`, `cook@restaurant.test`; Passwort jeweils der beim erstmaligen Seed gesetzte Wert.
- Seed legt 30 Plätze an drei Tischen und fünf Speisekarteneinträge an. Wiederholtes Ausführen überschreibt keine bestehenden Personalpasswörter.
- Nur PostgreSQL benötigt ein dauerhaftes Volume. Ein normaler Anwendungsneustart verändert keine Buchungsdaten.

## Zahlungen

Barzahlungen werden ausschließlich von Kellner:innen oder Manager:innen bestätigt. Beträge werden intern in **ganzen Eurocent** geführt. Zahlungsart bei der Reservierung ist eine Präferenz; beim Bezahlen kann die Person eine andere unterstützte Methode wählen.

Für Onlinezahlungen in `.env` setzen:

- `STRIPE_SECRET_KEY`: Stripe-Schlüssel mit Präfix `sk_test_`; Live-Schlüssel werden abgelehnt.
- `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET`: Zugang eines PayPal-Sandbox-Händlerkontos.

Die App öffnet den Checkout des Anbieters. Zahlungsstatus wird serverseitig anhand des Anbieters geprüft, niemals aus der Rückleitungs-URL übernommen. Der Hintergrundjob prüft ausstehende Zahlungen alle 15 Sekunden; zusätzlich gibt es „Zahlungsstatus prüfen“. Bei technischen Fehlern kann ein noch nicht erzeugter Zahlungslink mit derselben Auftragskennung wiederhergestellt werden.

Nach Storno zeigt die Rechnung Gebühren beziehungsweise Guthaben. Onlineerstattungen werden über die Schaltfläche „Erstattung anfordern“ gestartet; Barauszahlungen bestätigt das Personal. Eine Reservierung mit mehreren Zahlungen kann entsprechend mehrere Teilerstattungen benötigen. Gebühren bei Barzahlern bleiben als Forderung bestehen, bis das Personal eine Zahlung erfasst.

Anbieterzugänge sind nicht Bestandteil des Repositorys. Die echten Sandbox-Checkout-Oberflächen und tatsächliche externe Erstattungen müssen mit eigenen Testkonten abgenommen werden. Automatisierte Tests verwenden kontrollierte Anbieterantworten für PayPal und prüfen die Datenbankbuchungen.

## Prüfen

```bash
npm run build
npm test
# PostgreSQL muss laufen; jedes Skript erzeugt eine eigene Testdatenbank und löscht nur diese.
bash scripts/test-db.sh
node node_modules/playwright/cli.js install chromium
bash scripts/test-browser.sh
```

`npm test` führt ohne `RUN_DATABASE_TESTS=1` nur die Regeln aus und überspringt DB-/HTTP-Tests. `test-db.sh` aktiviert diese mit einer separaten Datenbank. Das Backend muss vor HTTP- und Browsertests gebaut sein. Browsertests verwenden die freien Ports 3000 und 5173; HTTP-Tests Port 43127.

CI prüft Build, Test-Typen, Regeln, SQL-Constraints, HTTP-Berechtigungen und Browserabläufe auf PostgreSQL. Browsertests prüfen den Gastablauf bei 360 Pixel Breite sowie Gruppenreservierung und Gesamtstorno. Gemessene Benutzerzeiten unter realen Netzwerkbedingungen und Kamerascan mit physischen Smartphones sind eigene Abnahmeschritte.

## Bereitstellung mit HTTPS

`compose.production.yaml` ist eine eigenständige Konfiguration mit internem PostgreSQL, App und Caddy als HTTPS-Proxy. Vorher `.env` konfigurieren:

- `PUBLIC_HOST`: Domain mit DNS-Eintrag auf den Server.
- `PUBLIC_URL=https://...`: exakt der öffentliche Ursprung; wird auch für E-Mail-Links und CSRF-Prüfung verwendet.
- `DB_PASSWORD`: langes, URL-taugliches Passwort, beispielsweise ausschließlich Hex-Zeichen.
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_FROM` und gegebenenfalls `SMTP_USER`, `SMTP_PASSWORD`: erreichbarer Mailserver; `localhost` bezeichnet innerhalb des Containers die App selbst.

```bash
docker compose -f compose.production.yaml build
docker compose -f compose.production.yaml up -d db
docker compose -f compose.production.yaml run --rm app node apps/api/dist/migrate.js
docker compose -f compose.production.yaml run --rm app node apps/api/dist/seed.js
docker compose -f compose.production.yaml up -d
```

HTTPS-Zertifikate verwaltet Caddy. Sichere Session-Cookies werden im Produktionsmodus aktiviert. Datenbank-Backups sind über `pg_dump` unabhängig von App-Releases einzurichten; Änderungen am Schema benötigen neue Migrationen. Es wurde kein Produktionsserver bereitgestellt.

Architektur, Geschäftsregeln und Anforderungszuordnung: [docs/implementation.md](docs/implementation.md).
