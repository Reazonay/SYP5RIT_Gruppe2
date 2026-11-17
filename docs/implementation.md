# Technische Umsetzung und Abnahme

## Architektur

React-Ansichten verwenden dieselbe REST-API unter `/api`. NestJS trennt HTTP-Validierung und Rollenprüfung von Reservierungslogik und Zahlungsintegration. PostgreSQL ist die einzige Quelle für Belegung, Preise, Salden und Zustände. Es gibt keine Browser-Datenbank und keine simulierten erfolgreichen Zahlungen.

- `apps/api/src/service.ts`: Buchungen, Gruppen, Zustände und Rechnungssalden.
- `apps/api/src/payments.ts`: dauerhaft gespeicherte Zahlungs-/Erstattungsaufträge und Anbieteraufrufe.
- `db/001_initial.sql`: Fremdschlüssel, Werteprüfungen, eindeutige Schlüssel und Ausschluss überlappender Platzbelegungen.

Frontend: getrennte Seiten für Reservierung, eigenen Besuch, Gruppen und Personal; wiederverwendbare Speisekarte, Verwaltung und Scanner. Zod prüft sämtliche schreibenden Benutzereingaben serverseitig. SQL-Werte werden ausschließlich parametrisiert übergeben.

## Konsistenz und Sicherheit

Alle Änderungen an einer Teilnahme sperren zuerst die gemeinsame Buchung, dann die Teilnahme. Dadurch können Gruppenaktionen und Einzelaktionen ihre Salden und Zustände nicht gleichzeitig überschreiben. Platzvergabe und Verschiebung verwenden zusätzlich dieselbe transaktionsgebundene Advisory-Sperre. Die GiST-Exclusion-Constraint verhindert Überschneidungen unabhängig von der Anwendung. Ein fehlgeschlagener Verschiebeversuch rollt auch die vorübergehende Freigabe alter Plätze zurück.

Zahlungsaufträge speichern vor dem externen Aufruf ihre UUID. Wiederholungen verwenden denselben Idempotenzschlüssel beim Anbieter. Zahlungsbetrag und Währung werden bei Bestätigung geprüft. Erstattungsaufträge werden ebenfalls vor dem Anbieteraufruf gespeichert; ein offener Erstattungsauftrag verhindert konkurrierende Erstattungen über andere Zahlungen derselben Teilnahme. Providerfehler führen nicht zu einer lokalen Erfolgsbuchung. Idempotenzgarantien der Anbieter haben zeitliche Grenzen; liegen Aufträge länger ungeklärt, müssen sie anhand der Anbietertransaktionen geprüft werden.

Personalpasswörter: bcrypt mit Kostenfaktor 12. Sessions: zufällige, in der Datenbank nur gehasht gespeicherte Tokens, acht Stunden gültig, HttpOnly-/SameSite-Cookies. Jede Personalroute prüft ihre erlaubten Rollen. Browser-Schreibzugriffe müssen den konfigurierten Ursprung besitzen; Loginversuche werden begrenzt. Das Rate-Limit läuft pro App-Prozess. Die mitgelieferte Bereitstellung nutzt eine App-Instanz.

Gast- und Gruppenlinks enthalten 256 Bit Zufall, keine Kontaktdaten. Tokens stehen im URL-Fragment und werden in API-Anfragen über einen Header übermittelt. Gespeichert werden ausschließlich Hashes, außer in noch zu versendenden E-Mail-Nachrichten. Nach erfolgreicher Zustellung wird der Nachrichtentext in der Outbox entfernt. Ein Link ist ein Zugangsgeheimnis: Wer ihn besitzt, kann die zugehörige Reservierung verwalten. Gruppenverwaltung zeigt Namen und Teilnahmezustände, keine fremden Bestellungen oder Zahlungen.

Beendete, stornierte und ausgebliebene Teilnahmen können weiterhin ihre Rechnung einsehen. Ihr QR-Code wird nicht mehr ausgegeben, Check-in und Bestellungen werden abgelehnt. Das ermöglicht Erstattungen, ohne einen abgeschlossenen Besuch erneut zu aktivieren.

## Verbindliche Geschäftsregeln

| Thema | Regel |
| --- | --- |
| Reservierungsdauer | 30–240 Minuten; UI bietet feste Auswahlwerte |
| Gruppe | 1–10 Personen; alle Plätze am selben Tisch; eigener QR-Code je Person |
| Reservieren | Beginn muss in der Zukunft liegen; sofortige interne Platzzuordnung |
| Belegung | Halboffenes Zeitintervall `[Beginn, Ende + 30 Minuten)` |
| Check-in | Ab 30 Minuten vor Beginn, bis ausschließlich 15 Minuten danach |
| Nichterscheinen | Ab Beginn + 15 Minuten; pro Person einmalig 10 €; Prüfung alle 15 Sekunden |
| Vor-/Nachbestellen | Vor Check-in Vorbestellung; danach Nachbestellung bis Besuchsende |
| Einzelstorno | Ab 60 Minuten vorher kostenlos, danach 10 € |
| Gruppen-Gesamtstorno | Ab 60 Minuten vorher kostenlos, danach insgesamt 20 €; kein bereits eingecheckter, beendeter oder ausgebliebener Gast |
| Gruppen-Teilstorno | Ab 60 Minuten vorher kostenlos; unter 60 bis mehr als 15 Minuten vorher 10 €; ab 15 Minuten vorher 75 % der persönlichen Vorbestellsumme |
| Prozentgebühr | Kaufmännisch auf Cent gerundet; ohne Vorbestellung 0 € |
| Gebührenzuordnung | Gesamtgebühr trägt die organisierende Person; falls bereits storniert, die erste noch aktive Teilnahme; bestehende Teilstornogebühren bleiben bestehen |
| Storno/No-show | Bestellung stornieren, Platz freigeben, QR deaktivieren; keine zusätzliche No-show-Gebühr für bereits Stornierte |
| Verschieben | Mindestens zwei Stunden vorher; neue Zeit in der Zukunft; alle Teilnahmen noch reserviert; gleiche Dauer und genug gemeinsame Plätze |
| Besuchsende | Gast nur ohne offene Forderung; Personal auch mit offener Forderung; bei frühem Ende Freigabe nach 30 Minuten |
| Überziehung | Warnung zwei Stunden nach Check-in; kein automatisches Besuchsende; ein tatsächlich belegter Platz kann nicht erneut eingecheckt werden |
| Küche | Offen → in Zubereitung → bereit; Kellner:in bestätigt bereit → serviert |
| Feedback | Einmal nach Besuchsende, 1–5 Punkte, optionaler Kommentar bis 2000 Zeichen |

Bei Gesamtrücktritt werden die 20 € nicht mit der Prozentstaffel für dieselben gerade stornierten Teilnahmen addiert. Bereits vorher einzeln stornierte Teilnahmen behalten ihre eigenen Gebühren. Gruppenmitglieder zahlen ihre eigenen Rechnungen. Onlinezahlungen sind auf Testbetrieb begrenzt.

## API und Rollen

| Bereich | Schnittstellen / Berechtigung |
| --- | --- |
| Öffentlich | `GET /api/menu`, `POST /api/reservations` |
| Eigener Gastcode | Reservierung, QR, Bestellung, Storno, Besuchsende, Zahlung, Zahlungsprüfung, Erstattung, Feedback |
| Gruppencode | `GET /api/group`, `POST /api/group/cancel`, `POST /api/group/shift` |
| Kellner:in | Plätze, Besuche, Scan/Check-in, Servieren, Barzahlungen und Barauszahlungen, Besuchsende auch ohne QR über Besuchsliste |
| Koch | Küchenansicht, Zubereitungsstatus; kein Zugriff auf Platz- oder Zahlungsverwaltung |
| Manager:in | Alle Personalfunktionen, Speisekarten- und Platzverwaltung |

Gäste benötigen kein Benutzerkonto. Personal wird über den Seed angelegt; eine Personalverwaltungsoberfläche war nicht Bestandteil der Anforderungen. Deaktivierte Artikel bleiben historisch erhalten; belegte oder zukünftig reservierte Plätze können nicht deaktiviert werden.

## Zuordnung zu Ausarbeitung.md

| Anforderung | Umsetzung / Prüfung |
| --- | --- |
| 3.1.1, R02, R15 | SQL-gesicherte Einzel-/Gruppenbuchung, dauerhafte Speicherung, Parallelitätstest |
| 3.1.2, R03, R16d | Persönliche Zufallstokens, QR-Anzeige und E-Mail-Anhang, gesperrte Zustände |
| 3.1.3, R04 | Speisekarte, Verfügbarkeit, Bestellungen mit Preis-Snapshot |
| 3.1.4, R07, R08 | Check-in, Platzanzeige, Personalübersicht, Doppel-Check-in-Test |
| 3.1.5, R06 | Küche, Zubereitung, Bereitmeldung, Servierbestätigung |
| 3.1.6, R10 | Gast- und Personalabschluss, Puffer, offene Forderungen |
| 3.1.7, R11 | Storno, Gebühren, Bestellstorno, QR-Sperre, Erstattung |
| 3.1.8 | Speisekarte hinzufügen/bearbeiten/deaktivieren, Plätze hinzufügen/deaktivieren |
| 3.2.1, R09, R26, R34 | Sandbox-Checkout, Barzahlung, Teilzahlungen, Guthaben, doppelte Buchungen verhindern |
| 3.2.2–3.2.7 | Feedback, Nachbestellung, Verschiebung, Warnung, No-show-Job und Puffer |
| 3.3.1–3.3.2 | Gruppenplätze, persönliche Einladungen, eigenes Bestellen/Zahlen, Gesamt-/Teilstorno |
| R14, R16a–c | Rollen- und HTTP-Tests, Passwort-Hashes, HTTPS-Bereitstellung |
| R16e | Gehostete Anbieter-Checkouts; keine Kartendaten im System |
| R17b | Browserablauf bei 360 Pixel Breite ohne horizontales Scrollen |
| R39a/R39b | Noch mit tatsächlichen Testpersonen, Netzwerkbedingungen und Smartphones abzunehmen |

Browser-Kamerascan verwendet die verfügbare BarcodeDetector-API. Unterstützt der Browser sie nicht oder wird der Kamerazugriff abgelehnt, kann das Personal den Reservierungslink eingeben. HTTPS beziehungsweise localhost sind für Kamerazugriff erforderlich.

Externe Sandbox-Zahlungen, reale SMTP-Zustellung und physischer Kamerascan brauchen eine abschließende Integration mit den dafür vorgesehenen Zugängen/Geräten. Lokale E-Mails gehen an Mailpit. Aufbewahrungs- und Löschfristen für personenbezogene Daten sind im Ausgangsdokument nicht festgelegt und werden hier nicht als rechtlich vollständig umgesetzt behauptet.

Anbieterreferenzen: [Stripe Checkout](https://docs.stripe.com/payments/existing-customers?platform=web&ui=stripe-hosted), [PayPal Orders API](https://developer.paypal.com/api/rest/integration/orders-api), [PayPal-Erstattungen](https://developer.paypal.com/api/payments/v2/captures-refund).
