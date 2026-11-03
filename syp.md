# Requirements Engineering – REAP


## 1. Requirements

### Ziel und Grundlage

- **Ziel:** Restaurantplätze reservieren, vorbestellen und Besuch mit QR-Code abwickeln.
- **Umfang:** Ein Restaurant. Zuerst vollständiger Kernablauf, danach Erweiterungen.


### Stakeholder

| Rolle | Aufgabe |
|---|---|
| Gast  | Reservieren, bestellen und bezahlen |
| Kellner | Check-in, Platzübersicht, Zahlung und Besuchsende |
| Koch | Bestellungen und Ankunftszeiten überblicken |
| Manager | Speisekarte, Plätze, Einstellungen und Personal verwalten |
| Projektteam | Umsetzung und Tests |

### Priorisierung

- **Must have**
- **Should have** 
- **Could have**
- **Won’t have** 


### Must have

- **R01 – Plätze reservieren:**  
  Gast wählt Datum, Ankunftszeit, Aufenthaltsdauer und Sitzplätze. Pflichtangaben: Name, Telefonnummer, E-Mail und Zahlungsart. Fehlende oder ungültige Angaben verhindern das Speichern.  
  **Abhängigkeit:** Benötigt R02 für Verfügbarkeit und R13 für angelegte Plätze.


- **R02 – Freie Plätze prüfen:**  
  System prüft alle Plätze für den gesamten Zeitraum. Keine überlappenden Buchungen, auch bei gleichzeitigen Anfragen. Pufferzeit berücksichtigen, sobald R33 umgesetzt ist.  
  **Abhängigkeit:** Gemeinsame Verfügbarkeitsregeln mit R29 und R36; R33 ergänzt die Pufferzeit.


- **R03 – QR-Code:**  
  Nach erfolgreicher Reservierung eindeutigen QR-Code anzeigen. Damit eigene Reservierung aufrufen. Nach Storno oder Besuchsende aktive Funktionen sperren.  
  **Abhängigkeit:** Benötigt erfolgreiche Reservierung aus R01. Sperrung bei R10 und R11.


- **R04 – Speisekarte:**  
  Gast sieht Speisen, Preise und Verfügbarkeit.

- **R05 – Essen vorbestellen:**  
  Gast bestellt verfügbare Speisen über QR-Code vor. Menge und Gesamtpreis vor Bestätigung anzeigen.  
  **Abhängigkeit:** Benötigt R03 und R04.


- **R06 – Küchenansicht:**  
  Küche sieht Bestellung, Mengen, Gast und Ankunftszeit. Stornierte Bestellungen nicht als offen anzeigen.  
  **Abhängigkeit:** Benötigt gespeicherte Bestellungen aus R05; Änderungen aus R21 und Stornos aus R11 übernehmen.


- **R07 – Check-in:**  
  Kellner scannt QR-Code. Anzeige: Platz, Bestellung und Zahlungsstatus. Bestätigter Check-in setzt Besuch auf aktiv und Platz auf belegt. Ungültige Codes ablehnen, keinen doppelten Check-in erzeugen.  
  **Abhängigkeit:** Benötigt R01, R03 und R14. Zahlungsstand aus R09 anzeigen; Onlinezahlungen aus R23 berücksichtigen, sobald umgesetzt. Personalansicht ist Must, Gastübersicht R24 ist Should.


- **R08 – Platzübersicht:**  
  Personal sieht für einen gewählten Zeitpunkt: frei, reserviert oder belegt. Nach Check-in, Storno und Besuchsende aktualisieren.  
  **Abhängigkeit:** Benötigt Reservierungen aus R01. Zustände nach R07, R10 und R11 übernehmen.


- **R09 – Barzahlung:**  
  Kellner trägt Betrag und Zeitpunkt ein. Offenen Betrag aktualisieren. Keine doppelte Erfassung derselben Zahlung.  
  **Abhängigkeit:** Benötigt zugeordnete Bestellung aus R05 und Personalberechtigung aus R14.


- **R10 – Besuch beenden:**  
  Kellner beendet aktiven Besuch nur bei offenem Betrag von 0 €. Platz freigeben, QR-Code sperren. Andere Reservierungen erhalten und gegebenenfalls Puffer beachten.  
  **Abhängigkeit:** Benötigt aktiven Besuch aus R07 und korrekten Zahlungsstand aus R09, gegebenenfalls R23. Puffer aus R33 beachten, sobald umgesetzt.


- **R11 – Stornieren:**  
  Gast sagt vor dem Check-in ab. Platz wieder frei, Vorbestellung storniert und QR-Code ungültig.  
  **Abhängigkeit:** Benötigt R01 und R03. Vorbestellungen aus R05 stornieren. Mit R26 und R30 abstimmen, sobald umgesetzt.


- **R12 – Speisekarte verwalten:**  
  Manager ändert Speisen, Getränke, Preise und Verfügbarkeit. Bestätigte Bestellungen behalten ihre Preise.  
  **Abhängigkeit:** Benötigt Managerberechtigung aus R14. Änderungen für neue Bestellungen aus R05 verwenden.


- **R13 – Tische verwalten:**  
  Manager legt Tische und Sitzplätze an oder ändert sie. Entfernen bei aktiver oder zukünftiger Buchung verhindern.  
  **Abhängigkeit:** Benötigt Managerberechtigung aus R14. Buchungen aus R01 beim Entfernen prüfen.


- **R14 – Rollen:**  
  Personal muss sich anmelden. Funktionen je Rolle freigeben: Kellner, Koch und Manager. Berechtigungen bei jedem geschützten Aufruf prüfen. Gast sieht nur eigene Reservierung.

### Should-have

- **R18 – Ohne Konto:**  
  Reservieren nur mit Kontaktdaten, keine Registrierung nötig.  
  **Abhängigkeit:** Basisablauf für R01 angenommen. Priorität noch bestätigen; R41 bleibt optional.


- **R19 – Bestätigungs-Mail:**  
  Gast bekommt Reservierungsdaten und QR-Code per Mail. Mailfehler hebt die Buchung nicht auf.  
  **Abhängigkeit:** Benötigt R01 und R03.


- **R20 – Getränke vorbestellen:**  
  Gast kann auch verfügbare Getränke vorbestellen.  
  **Abhängigkeit:** Erweitert R05; verfügbare Getränke aus R12 verwenden.


- **R21 – Bestellung ändern:**  
  Gast ändert oder löscht Vorbestellung vor festgelegter Frist. Betrag und Küchenansicht aktualisieren. Frist und Umgang mit bezahlten oder zubereiteten Positionen noch offen.  
  **Abhängigkeit:** Benötigt R05. Küchenansicht R06 und gegebenenfalls Erstattung R26 berücksichtigen.


- **R22 – Nachbestellen:**  
  Weitere Speisen und Getränke bestellen, solange Besuch aktiv ist. Neue Positionen erhöhen den offenen Betrag.  
  **Abhängigkeit:** Benötigt aktiven Besuch aus R07 sowie Angebot aus R04/R12. Zahlungsstand aktualisieren.


- **R23 – Online zahlen:**  
  Gast zahlt vor Besuch oder am Ende per Kreditkarte oder PayPal. Simulation fürs Schulprojekt vorgeschlagen und sichtbar kennzeichnen. Erfolgreiche Zahlung genau einmal verbuchen. Abbruch oder Fehler ändern Restbetrag nicht.  
  **Abhängigkeit:** Benötigt zugeordnete offene Bestellbeträge. Zahlungsstand mit R09 und R24 abstimmen.


- **R24 – Zahlungsstatus:**  
  Über QR-Code sichtbar: Gesamtbetrag, bezahlt und noch offen.  
  **Abhängigkeit:** Benötigt R03 und korrekten Zahlungsstand aus R09, gegebenenfalls R23.


- **R25 – Weitere Zahlungen:**  
  Barzahlung beim Check-in möglich. Nachbestellungen online oder bar zahlen.  
  **Abhängigkeit:** Benötigt R07 und R09; Nachbestellungen aus R22 und Onlinezahlung aus R23, sobald umgesetzt.


- **R26 – Rückerstattung:**  
  Bei Storno erstattbare Zahlungen abzüglich Gebühren zurückgeben. Erstattungsbetrag mindestens 0 €. Keine doppelte Erstattung. Erstattbare Positionen noch offen.  
  **Abhängigkeit:** Benötigt Storno aus R11 und erfasste Zahlungen. Gebühren mit R30, R31 und R47 abstimmen; doppelte Belastungen vermeiden.


- **R27 – Gruppenreservierung:**  
  Einer reserviert für mehrere. Alle am gleichen Tisch. Keine teilweise Gruppenbuchung.  
  **Abhängigkeit:** Erweitert R01. R02 muss alle benötigten Plätze am selben Tisch prüfen.


- **R28 – Gruppe einladen:**  
  Organisator lädt Mitglieder ein. Jeder erhält eigenen QR-Code und kann selbst bestellen und zahlen. Gemeinsame Beträge und Gebühren noch klären.  
  **Abhängigkeit:** Benötigt R27 und R03. Bestellungen und Zahlungen einzelnen Mitgliedern eindeutig zuordnen.


- **R29 – Termin verschieben:**  
  Bis einschließlich zwei Stunden vor bisheriger Ankunft möglich. Nur wenn alle benötigten Plätze frei sind. Sonst ursprüngliche Reservierung erhalten.  
  **Abhängigkeit:** Benötigt R01 und R02. Dieselben Zeitregeln wie R33 und R36 verwenden.


- **R30 – Stornogebühren:**  
  Bis einschließlich eine Stunde vor Ankunft gratis. Danach: einzeln 10 €, Gruppe 20 €. Gebühr vor Bestätigung anzeigen. Einhebung offener Gebühren noch klären.  
  **Abhängigkeit:** Benötigt R11. Mit R26, R31 und R47 abstimmen; doppelte Belastungen vermeiden.


- **R31 – Nichterscheinen:**  
  Personal erfasst Nichterscheinen. Vorgesehen: 10 € je fehlender Person. Wartefrist und Abgrenzung zu Stornogebühren noch offen. Bis dahin keine automatische Gebühr.  
  **Abhängigkeit:** Benötigt Reservierung aus R01 und Check-in-Status aus R07. Mit R30 und R47 abstimmen; doppelte Belastungen vermeiden.


- **R32 – Änderungen melden:**  
  Storno und Verschiebung dem Gast bestätigen. Betroffene Personalansichten aktualisieren.  
  **Abhängigkeit:** Benötigt Storno aus R11 oder Verschiebung aus R29. E-Mail-Versand aus R19 nutzen, wenn verfügbar.


- **R33 – Pufferzeit:**  
  30 Minuten zwischen Reservierungen desselben Platzes zum Abräumen und Putzen.  
  **Abhängigkeit:** Erweitert Verfügbarkeitsprüfung R02. Dieselbe Pufferregel bei R29 und R36 verwenden.


- **R34 – Selbst beenden:**  
  Gast beendet Besuch über QR-Code-Menü. Nur wenn alles bezahlt ist.  
  **Abhängigkeit:** Benötigt R03 und aktiven Besuch aus R07. Dieselben Abschluss- und Zahlungsregeln wie R10.


- **R35 – Erinnerung:**  
  Besuch zwei Stunden nach Check-in noch offen → einmalige Meldung an Kellner.  
  **Abhängigkeit:** Benötigt Check-in-Zeit aus R07 und Besuchsstatus aus R10 beziehungsweise R34.


- **R36 – Einstellungen:**  
  Manager ändert Öffnungszeiten, Reservierungszeiten, Gebühren und Zeitgrenzen. Bestehende Buchungen und verbuchte Gebühren nicht stillschweigend ändern.  
  **Abhängigkeit:** Benötigt Managerberechtigung aus R14. Einstellungen mit R02, R29, R30, R31 und R33 abstimmen.


- **R37 – Personalkonten:**  
  Manager legt Konten an, weist Rollen zu und deaktiviert Konten. Deaktivierte Konten dürfen keine geschützten Funktionen aufrufen.  
  **Abhängigkeit:** Benötigt Rollen und Managerberechtigung aus R14.


### Could have

- **R41 – Gastkonto:**  
  Konto erstellen und eigene Reservierungen ansehen. Konto bleibt optional.  
  **Abhängigkeit:** Benötigt sichere Zuordnung zu R01. Buchung ohne Konto aus R18 bleibt möglich.


- **R42 – Alternative Termine:**  
  Termin voll → andere freie Zeiten für passende Personenzahl und Aufenthaltsdauer anzeigen.  
  **Abhängigkeit:** Benötigt Verfügbarkeitsprüfung aus R02. Puffer R33 und Öffnungszeiten R36 berücksichtigen, sobald umgesetzt.


- **R43 – Anmerkungen:**  
  Allergien oder Sonderwünsche zur Bestellung dazuschreiben. Küche sieht die Hinweise.  
  **Abhängigkeit:** Erweitert R05. Hinweise in R06 anzeigen.


- **R44 – Rechnung:**  
  Positionen, Zahlungen und offenen Betrag anzeigen. Nach vollständiger Zahlung Beleg bereitstellen. Belegform noch offen.  
  **Abhängigkeit:** Benötigt gespeicherte Bestellpositionen und Zahlungen aus R05/R09, gegebenenfalls R22/R23.


- **R45 – Feedback:**  
  Gast bewertet abgeschlossenen Besuch. Höchstens eine Bewertung pro Besuch. Bewertungsskala noch offen.  
  **Abhängigkeit:** Benötigt abgeschlossenen Besuch aus R10 oder R34.


- **R46 – Gruppenübersicht:**  
  Organisator sieht: eingeladen, zugesagt und eingecheckt.  
  **Abhängigkeit:** Benötigt R27, R28 und Check-in-Status aus R07.


- **R47 – Einzeln absagen:**  
  Gruppenmitglied storniert eigenen Platz vor Check-in. Andere Plätze bleiben reserviert. Gebühren noch offen.  
  **Abhängigkeit:** Benötigt R27 und R28. Mit R26, R30 und R31 abstimmen; Gebührenregel noch offen.


- **R48 – Auswertungen:**  
  Manager sieht Auslastung, erfassten Umsatz, Nichterscheinen und Bewertungen für gewählten Zeitraum. Simulierte Zahlungen kennzeichnen.  
  **Abhängigkeit:** Benötigt gespeicherte Besuche, Zahlungen, Nichterscheinen R31 und Bewertungen R45 für die jeweiligen Kennzahlen.


- **R49 – Protokoll:**  
  Änderungen an Reservierungen, Zahlungen und Gebühren mit Zeitpunkt, Benutzer und Art speichern. Nur für berechtigtes Personal einsehbar.  
  **Abhängigkeit:** Benötigt Benutzerzuordnung aus R14. Änderungen an Reservierungen, Zahlungen und Gebühren erfassen.


### Qualitätsanforderungen

#### Must have
- **R15 – Datenerhalt**  
  Bestätigte Reservierungen, Bestellungen und Zahlungen bleiben nach regulärem Neustart vollständig vorhanden. Prüfung: Daten anlegen, neu starten und vergleichen.

- **R16a – Datenschutz:**  
  Gäste sehen nur eigene Reservierungsdaten. Personalzugriff entsprechend R14. Fremde oder unberechtigte Zugriffe ablehnen.  
  **Abhängigkeit:** Berechtigungen aus R14 anwenden.


- **R16d – QR-Sicherheit Must:**  
  QR-Zugriffstokens zufällig erzeugen. Vorschlag: mindestens 128 Bit Zufall. Keine direkt lesbaren Kontaktdaten im Code. Gesperrte Tokens ablehnen.  
  **Abhängigkeit:** Gilt für QR-Codes aus R03 und R28.


- **R17b – Smartphone Must:**  
  Reservierung und Gastansicht am Handy bedienbar. Testvorschlag: 360 Pixel Breite, kein horizontales Scrollen und alle Hauptaktionen erreichbar.  
  **Abhängigkeit:** Gilt für Reservierung R01 und Gastansicht R03.


#### Should have

- **R38 – Verständlichkeit**  
  Klare Beschriftungen, Pflichtfelder und Fehlermeldungen. Testvorschlag: Drei Testpersonen reservieren und bestellen ohne Einführung.

- **R39a – Reservierungsdauer**  
  Ziel: Reservierung in weniger als zwei Minuten abschließen. Messung vom Öffnen des Formulars bis zur Bestätigung. Testbedingungen noch vereinbaren.  
  **Abhängigkeit:** Misst den Ablauf aus R01.


- **R39b – Check-in-Reaktionszeit**  
  Ziel: Anzeige höchstens zwei Sekunden nach QR-Erkennung. Lasttestvorschlag: 20 gleichzeitig aktive Nutzer. Hardware, Netzwerk und Messverfahren noch vereinbaren.  
  **Abhängigkeit:** Misst die Anzeige aus R07.


### Technische Vorgaben

#### Must have

- **R16b – Passwörter**  
  Mit geeignetem Passwort-Hashverfahren speichern. Kein Klartext.

- **R16c – Verbindung**  
  Bereitgestellte Anwendung verwendet HTTPS.

- **R16e – Kreditkartendaten**  
  Keine Kreditkartendaten selbst speichern.

- **R17a – Java**  
  Java verwenden. Arbeitsannahme: Java-Backend mit Weboberfläche. Architektur noch bestätigen.

#### Should have

- **R40a – Codeaufbau**  
  Oberfläche, Geschäftslogik und Datenzugriff trennen. Im Code-Review prüfen.

- **R40d – Datenbank**  
  Relationale Datenbank

### Projektvorgaben

#### Must have

- **R17c – Zeitrahmen**  
  Kernumfang innerhalb eines Projektsemesters fertigstellen. Abgabetermin und verfügbaren Aufwand festlegen.

- **R17d – Reihenfolge**
  Must-haves zuerst. Umfang regelmäßig mit verfügbarer Zeit abgleichen. Bei Bedarf Erweiterungen verschieben.

#### Should have

- **R40b – Tests**  
  Konkurrierende Buchungen, Berechtigungen, Zahlungen, Storno und Besuchsende testen. Ergebnisse dokumentieren.

- **R40c – Versionsverwaltung**  
  Code und Requirements in Git verwalten. Änderungen nachvollziehbar festhalten.

### Won’t have

- **R50 – Lieferdienst / Abholung:**  
  Nur vor Ort.

- **R51 – Mehrere Restaurants:**  
  Nur ein Restaurant.

- **R52 – Bonuspunkte:**  
  Kein Treueprogramm.

- **R53 – Buchhaltung:**  
  Keine vollständige Buchhaltung oder Steuerabrechnung.

### Begriffe und Zustände

- **Reservierung:** Konkrete Plätze mit Datum, Ankunftszeit und Aufenthaltsdauer.
- **Besuch:** Beginnt beim Check-in. „Offen“ bedeutet aktiv und noch nicht beendet.
- **Offener Betrag:** Bestätigte Positionen und anwendbare Gebühren abzüglich Zahlungen und Gutschriften.
- **Zeitregel als Arbeitsannahme:** Ohne Puffer darf eine Folgereservierung beim geplanten Ende beginnen. Mit R33 erst 30 Minuten danach.

| Ausgang | Aktion / Bedingung | Ergebnis |
|---|---|---|
| Reserviert | Gültiger Check-in | Besuch aktiv, Platz belegt |
| Reserviert | Storno bestätigt | Storniert, Platz frei, Code gesperrt |
| Aktiv | Besuchsende, Restbetrag 0 € | Abgeschlossen, Platz freigegeben, Code gesperrt |
| Aktiv | Besuchsende, Restbetrag über 0 € | Ablehnen, Besuch bleibt aktiv |
| Storniert / abgeschlossen | Check-in oder Bestellung | Ablehnen |

Puffer betrifft die erneute Buchbarkeit. Umgang mit tatsächlicher Überziehung und Nichterscheinen noch klären.

## 2. User Stories


### Must have

- **US01 – Plätze reservieren - R01:**  
  Als Gast möchte ich Datum, Zeit und Plätze reservieren, damit ich meinen Besuch planen kann.  
  **Prüfung:** Gegeben: Plätze frei, Pflichtangaben gültig. Wenn: Gast bestätigt. Dann: Genau eine Reservierung speichern und QR-Code anzeigen. Ungültige Angaben → keine Buchung, Feldfehler anzeigen.

- **US02 – Freie Plätze prüfen - R02:**  
  Als Gast möchte ich nur freie Plätze buchen, damit meine Reservierung verlässlich ist.  
  **Prüfung:** Gegeben: Zwei Gäste buchen denselben Platz für überlappende Zeiten. Wenn: Beide gleichzeitig bestätigen. Dann: Höchstens eine Buchung bestätigen, andere ablehnen.

- **US03 – QR-Code - R03:**  
  Als Gast möchte ich einen QR-Code erhalten, damit ich meine Reservierung aufrufen kann.  
  **Akzeptanz:** Code öffnet nur zugehörige Reservierung. Nach Storno oder Abschluss aktive Funktionen sperren.

- **US04 – Speisekarte - R04:**  
  Als Gast möchte ich Speisen und Preise ansehen, damit ich mein Essen auswählen kann.  
  **Akzeptanz:** Aktuelle Preise und Verfügbarkeit anzeigen. Nicht verfügbare Artikel nicht bestellbar.

- **US05 – Essen vorbestellen - R05:**  
  Als Gast möchte ich Essen vorbestellen, damit die Küche meine Bestellung vorab kennt.  
  **Prüfung:** Gegeben: Gültiger Code und verfügbarer Artikel. Wenn: Gast Menge und Gesamtbetrag bestätigt. Dann: Bestellung speichern. Artikel inzwischen ausverkauft → Bestellung ablehnen.

- **US06 – Küchenansicht - R06:**  
  Als Koch möchte ich Vorbestellungen sehen, damit ich die Vorbereitung planen kann.  
  **Akzeptanz:** Speisen, Mengen, Gast und Ankunftszeit anzeigen. Stornierte Bestellungen nicht als offen anzeigen.

- **US07 – Check-in - R07:**  
  Als Kellner möchte ich Gäste per QR-Code einchecken, damit ich ihre Reservierung zuordnen kann.  
  **Prüfung:** Gegeben: Reservierung noch nicht eingecheckt. Wenn: Kellner gültigen Code scannt und bestätigt. Dann: Besuch aktiv, Platz belegt. Zweiter Scan → bestehenden Besuch anzeigen. Ungültiger Code → keine Änderung.

- **US08 – Platzübersicht - R08:**  
  Als Kellner möchte ich die Platzbelegung sehen, damit ich freie Plätze erkennen kann.  
  **Akzeptanz:** Frei, reserviert oder belegt für gewählten Zeitpunkt anzeigen. Änderungen übernehmen.

- **US09 – Barzahlung - R09:**  
  Als Kellner möchte ich Barzahlungen eintragen, damit der offene Betrag stimmt.  
  **Prüfung:** Gegeben: 30 € offen. Wenn: Kellner 20 € erhaltene Barzahlung erfasst. Dann: Zahlung speichern, 10 € offen. Wiederholtes Absenden derselben Zahlung → weiterhin 10 € offen. Betrag von 0 € oder weniger ablehnen.

- **US10 – Besuch beenden - R10:**  
  Als Kellner möchte ich bezahlte Besuche beenden, damit Plätze wieder verfügbar werden.  
  **Prüfung:** Gegeben: Aktiver Besuch. Wenn: Kellner Besuch beendet. Dann: Bei Restbetrag 0 € abschließen, Code sperren und Platz freigeben. Bei offenem Betrag ablehnen. Puffer beachten.

- **US11 – Stornieren - R11:**  
  Als Gast möchte ich meine Reservierung absagen, damit der Platz wieder vergeben werden kann.  
  **Akzeptanz:** Vor Check-in stornieren, Vorbestellung absagen, Platz freigeben und Code sperren. Aktiver Besuch kann nicht mehr über diesen Ablauf storniert werden.

- **US12 – Speisekarte verwalten - R12:**  
  Als Manager möchte ich die Speisekarte bearbeiten, damit das Angebot aktuell bleibt.  
  **Akzeptanz:** Artikel, Preise und Verfügbarkeit speichern. Bestehende Bestellpreise unverändert lassen.

- **US13 – Tische verwalten - R13:**  
  Als Manager möchte ich Tische und Plätze verwalten, damit der Restaurantaufbau stimmt.  
  **Akzeptanz:** Tische und Plätze anlegen und ändern. Entfernen bei aktiver oder zukünftiger Buchung verhindern.

- **US14 – Rollen - R14:**  
  Als Mitarbeiter möchte ich mit meiner Rolle arbeiten, damit ich die passenden Funktionen nutzen kann.  
  **Akzeptanz:** Anmeldung nötig. Berechtigungen auch bei direkten Aufrufen prüfen. Gäste sehen nur eigene Reservierung.

- **US15 – Daten speichern - R15:**  
  Als Mitarbeiter möchte ich gespeicherte Daten wiederfinden, damit ich nach einem Neustart weiterarbeiten kann.  
  **Akzeptanz:** Reservierungen, Bestellungen und Zahlungen nach regulärem Neustart vollständig vorhanden.

- **US16 – Sicherheit - R16a–e:**  
  Als Gast möchte ich meine Daten geschützt wissen, damit ich die Anwendung sicher nutzen kann.  
  **Akzeptanz:** Keine fremden Reservierungen zugänglich. Passwörter gehasht, HTTPS und zufällige QR-Tokens. Keine Kreditkartendaten selbst speichern.

- **US17 – Smartphone-Bedienung - R17b:**  
  Als Gast möchte ich die Anwendung am Handy nutzen, damit ich unterwegs reservieren kann.  

### Should have

- **US18 – Ohne Konto - R18:**  
  Als Gast möchte ich ohne Konto reservieren, damit ich direkt buchen kann.  
  **Akzeptanz:** Kontaktdaten reichen aus. Keine Registrierung verlangen.

- **US19 – Bestätigungs-Mail - R19:**  
  Als Gast möchte ich eine Bestätigungs-Mail erhalten, damit ich Reservierung und QR-Code griffbereit habe.  
  **Akzeptanz:** Nach Buchung Daten und Code versenden. Mailfehler hebt Buchung nicht auf.

- **US20 – Getränke vorbestellen - R20:**  
  Als Gast möchte ich Getränke vorbestellen, damit meine Bestellung vollständig ist.  
  **Akzeptanz:** Verfügbare Getränke mit Menge und Preis wie Speisen bestellen.

- **US21 – Bestellung ändern - R21:**  
  Als Gast möchte ich meine Vorbestellung ändern, damit ich sie anpassen kann.  
  **Akzeptanz:** Vor festgelegter Frist ändern oder löschen. Betrag und Küchenansicht aktualisieren. Frist noch offen.

- **US22 – Nachbestellen - R22:**  
  Als Gast möchte ich während des Besuchs nachbestellen, damit ich weitere Artikel bestellen kann.  
  **Akzeptanz:** Nur bei aktivem Besuch. Neue Positionen erhöhen Restbetrag.

- **US23 – Online zahlen - R23:**  
  Als Gast möchte ich online zahlen, damit ich meinen Betrag direkt begleichen kann.  
  **Prüfung:** Gegeben: Offener Betrag. Wenn: Zahlung erfolgreich bestätigt wird. Dann: Genau einmal verbuchen. Abbruch oder Fehler → Betrag unverändert. Simulation kennzeichnen.

- **US24 – Zahlungsstatus - R24:**  
  Als Gast möchte ich den Zahlungsstand sehen, damit ich offene Beträge kenne.  
  **Akzeptanz:** Gesamtbetrag, bezahlt und offen korrekt anzeigen.

- **US25 – Weitere Zahlungen - R25:**  
  Als Gast möchte ich beim Check-in und nach einer Nachbestellung zahlen, damit ich alle Beträge begleichen kann.  
  **Akzeptanz:** Barzahlung beim Check-in zulassen. Nachbestellungen online oder bar bezahlen.

- **US26 – Rückerstattung - R26:**  
  Als Gast möchte ich bei Storno eine Erstattung erhalten, damit ich erstattbare Beträge zurückbekomme.  
  **Akzeptanz:** Gebühren abziehen. Erstattung mindestens 0 €. Keine doppelte Erstattung.

- **US27 – Gruppenreservierung - R27:**  
  Als Organisator möchte ich für mehrere Personen reservieren, damit wir gemeinsam sitzen können.  
  **Akzeptanz:** Alle Plätze am selben Tisch verfügbar. Keine teilweise Gruppenbuchung.

- **US28 – Gruppe einladen - R28:**  
  Als Gruppenmitglied möchte ich einen eigenen QR-Code nutzen, damit ich selbst bestellen und zahlen kann.  
  **Akzeptanz:** Eigener Code je Mitglied. Bestellungen und Zahlungen richtig zuordnen. Keine fremden persönlichen Daten anzeigen.

- **US29 – Termin verschieben - R29:**  
  Als Gast möchte ich meinen Termin verschieben, damit ich meine Planung ändern kann.  
  **Prüfung:** Gegeben: Termin um 18:00 Uhr. Wenn: Gast um 16:00 Uhr auf freien Termin verschiebt. Dann: Zulassen. Nach 16:00 Uhr → ablehnen. Neuer Termin belegt → alte Buchung erhalten.

- **US30 – Stornogebühren - R30:**  
  Als Gast möchte ich Stornogebühren vorab sehen, damit ich die Kosten kenne.  
  **Prüfung:** Gegeben: Ankunft um 18:00 Uhr. Wenn: Gast um 17:00 Uhr storniert. Dann: Gebühr 0 €. Nach 17:00 Uhr → einzeln 10 €, Gruppe 20 € vor Bestätigung anzeigen.

- **US31 – Nichterscheinen - R31:**  
  Als Kellner möchte ich Nichterscheinen erfassen, damit ich fehlende Gäste dokumentieren kann.  
  **Akzeptanz:** Vorgesehen sind 10 € pro fehlender Person. Wartefrist und Abgrenzung zu Stornogebühren zuerst klären.

- **US32 – Änderungen melden - R32:**  
  Als Gast möchte ich über Änderungen informiert werden, damit ich den aktuellen Stand kenne.  
  **Akzeptanz:** Storno und Verschiebung bestätigen. Personalansichten aktualisieren.

- **US33 – Pufferzeit - R33:**  
  Als Kellner möchte ich Zeit zum Abräumen haben, damit ich den Platz vorbereiten kann.  
  **Prüfung:** Gegeben: Ende 18:00 Uhr, Puffer 30 Minuten. Wenn: Derselbe Platz ab 18:15 Uhr angefragt wird. Dann: Ablehnen. Ab 18:30 Uhr zulassen, sofern sonst frei.

- **US34 – Selbst beenden - R34:**  
  Als Gast möchte ich meinen Besuch selbst beenden, damit ich ohne zusätzliche Nachfrage gehen kann.  
  **Akzeptanz:** Nur aktiver Besuch mit Restbetrag 0 €. Platz freigeben, Code sperren und Puffer beachten.

- **US35 – Erinnerung - R35:**  
  Als Kellner möchte ich an offene Besuche erinnert werden, damit ich deren Status prüfen kann.  
  **Prüfung:** Gegeben: Check-in um 18:00 Uhr. Wenn: Besuch um 20:00 Uhr noch aktiv ist. Dann: Einmalige Erinnerung. Bereits abgeschlossen → keine Erinnerung.

- **US36 – Einstellungen - R36:**  
  Als Manager möchte ich Betriebseinstellungen ändern, damit ich Reservierungen steuern kann.  
  **Akzeptanz:** Öffnungszeiten, Reservierungszeiten, Gebühren und Grenzen speichern. Bestehende Buchungen nicht stillschweigend ändern.

- **US37 – Personalkonten - R37:**  
  Als Manager möchte ich Personalkonten verwalten, damit ich Zugänge vergeben kann.  
  **Akzeptanz:** Konten anlegen, Rollen zuweisen und deaktivieren. Deaktivierte Konten haben keinen Zugriff.

- **US38 – Bedienung - R38:**  
  Als Gast möchte ich eine übersichtliche Oberfläche nutzen, damit ich ohne Erklärung zurechtkomme.  
  **Akzeptanz:** Klare Beschriftungen, sichtbare Pflichtfelder und verständliche Fehlermeldungen. Bedienbarkeit mit Testpersonen prüfen.

- **US39 – Geschwindigkeit - R39a,b:**  
  Als Nutzer möchte ich Abläufe schnell erledigen, damit ich wenig Zeit benötige.  
  **Akzeptanz:** Zielwerte: Reservierung unter zwei Minuten, Check-in-Anzeige höchstens zwei Sekunden nach Codeerkennung. Testbedingungen noch festlegen.

- **US40 – Projektaufbau - R40a–d:**  
  Als Entwickler möchte ich einen geordneten Projektaufbau nutzen, damit ich Änderungen nachvollziehen kann.  
  **Akzeptanz:** Oberfläche, Logik und Datenzugriff trennen. Git verwenden und kritische Abläufe testen. Relationale Datenbank vorgeschlagen.

### Could have

- **US41 – Gastkonto · R41:**  
  Als Gast möchte ich ein Konto erstellen, damit ich meine Reservierungen gesammelt sehe.  
  **Akzeptanz:** Nur eigene zugeordnete Reservierungen anzeigen. Konto bleibt optional.

- **US42 – Alternative Termine · R42:**  
  Als Gast möchte ich alternative Termine sehen, damit ich bei voller Belegung anders buchen kann.  
  **Akzeptanz:** Freie Zeiten für passende Personenzahl und Dauer vorschlagen. Vor Bestätigung erneut prüfen.

- **US43 – Anmerkungen · R43:**  
  Als Gast möchte ich Hinweise zur Bestellung ergänzen, damit die Küche meine Wünsche kennt.  
  **Akzeptanz:** Text zur Bestellung speichern und in Küchenansicht anzeigen.

- **US44 – Rechnung · R44:**  
  Als Gast möchte ich Abrechnung und Beleg ansehen, damit ich meine Ausgaben nachvollziehen kann.  
  **Akzeptanz:** Positionen, Zahlungen und Restbetrag anzeigen. Beleg nach Zahlung bereitstellen.

- **US45 – Feedback · R45:**  
  Als Gast möchte ich meinen Besuch bewerten, damit ich Feedback geben kann.  
  **Akzeptanz:** Nur nach abgeschlossenem Besuch. Höchstens eine Bewertung pro Besuch.

- **US46 – Gruppenübersicht · R46:**  
  Als Organisator möchte ich den Gruppenstatus sehen, damit ich die Teilnahme überblicken kann.  
  **Akzeptanz:** Eingeladen, zugesagt und eingecheckt je Mitglied anzeigen.

- **US47 – Einzeln absagen · R47:**  
  Als Gruppenmitglied möchte ich meinen eigenen Platz absagen, damit die übrige Gruppe weiter reserviert bleibt.  
  **Akzeptanz:** Eigenen Platz vor Check-in stornieren. Andere Plätze erhalten. Gebührenregel noch offen.

- **US48 – Auswertungen · R48:**  
  Als Manager möchte ich Kennzahlen ansehen, damit ich den Betrieb überblicken kann.  
  **Akzeptanz:** Kennzahlen nach Zeitraum anzeigen. Simulierte Zahlungen kennzeichnen.

- **US49 – Protokoll · R49:**  
  Als Manager möchte ich Änderungen nachvollziehen, damit ich Buchungen und Zahlungen prüfen kann.  
  **Akzeptanz:** Änderungen mit Zeitpunkt, Benutzer und Art speichern. Nur berechtigt einsehbar.


## 3. Team-Blog

**Motyka**

- Requirements: R01–R13.
- User Stories: US01–US13, US16–US17.

**Hamburger**

- Requirements: R18–R28.
- User Stories: US18–US28, US38–US39.

**Maier**

- Requirements: R14, R29–R37.
- User Stories: US14–US15, US29–US37.

**Rappold**

- Requirements: R41–R49.
- User Stories: US40–US49.

