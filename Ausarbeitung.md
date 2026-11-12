# Ausarbeitung von Requirements und Userstories für Gruppe 1

## Bewertung und Evaluierung auf Skala von 0 bis 9

Requirements Dokument wurde mit den Kriterien aus dem swebok-v4 überprüft.

Die Bewertung erfolgt auf einer Skala von 0 bis 9.


**Gesamtbewertung: 5 von 9.** Die Inhalte passen gut zur Angabe, aber einige Aussagen sind noch nicht genau genug, und es fehlen User Stories, nicht-funktionale Anforderungen und eindeutige IDs.

### Kriterien und Bewertung

#### Kriterien

- Ist es eindeutig? Gibt es nur eine Lesart?

- Ist es testbar? Kann man klar sagen, ob es erfüllt ist?

- Ist es atomar? Steckt nur eine Entscheidung drin?

- Passt es zur Angabe und widerspricht es keinem anderen Requirement?

#### Bewertungen

### 3.1 Must-have

#### 3.1.1 Reservieren
- Der Gast kann einen oder mehrere Plätze für einen bestimmten Zeitpunkt und eine bestimmte Dauer reservieren.

  *5/9 Punkte*

- Das System prüft, ob im gewünschten Zeitraum genug Plätze frei sind.

  *6/9 Punkte*

- Der Gast gibt Name, Telefonnummer, E-Mail und Zahlungsart (Kreditkarte, PayPal oder Cash) an.

  *7/9 Punkte*

#### 3.1.2 QR-Code
- Wenn die Reservierung klappt, erstellt das System einen QR-Code und schickt ihn dem Gast.

  *6/9 Punkte*

#### 3.1.3 Vorbestellen
- Der Gast kann mit dem QR-Code Speisen und Getränke vorbestellen.

  *5/9 Punkte*

#### 3.1.4 Check-in und Platzzuweisung
- Die Kellner:in scannt den QR-Code beim Check-in.

  *7/9 Punkte*

- Der Gast bekommt seinen reservierten Platz zugewiesen.

  *6/9 Punkte*

#### 3.1.5 Servieren
- Die vorbestellten Speisen und Getränke werden serviert.

  *4/9 Punkte*

#### 3.1.6 Besuch beenden, Platz freigeben
- Der Gast kann seinen Besuch selbst beenden (Kontextmenü im QR-Code). Danach ist der Platz wieder frei.

  *7/9 Punkte*

- Der Kellner kann den Besuch beenden und den Platz freigeben, wenn sie merkt, dass der Gast gegangen ist.

  *7/9 Punkte*

#### 3.1.7 Stornieren
- Der Gast kann seine Reservierung stornieren.

  *6/9 Punkte*

- Bis 1 Stunde vor dem Termin ist das kostenlos, danach kostet es 10 €.

  *7/9 Punkte*

#### 3.1.8 Verwaltung durch Manager:in
- Der Manager kann Speisen, Getränke und Plätze im System verwalten.

  *4/9 Punkte*

### 3.2 Maybe

#### 3.2.1 Zahlung 

- Der Gast kann im Voraus online bezahlen (Kreditkarte oder PayPal). Die Bezahlung wird im QR-Code bestätigt.
- Der Gast kann beim Check-in bar bezahlen.
- Beim Beenden des Besuchs kann der Gast online bezahlen oder bar beim Kellner.

  *4/9 Punkte (gesamt)*

#### 3.2.2 Feedback 

- Nach dem Besuch kann der Gast online Feedback geben.

  *4/9 Punkte*

#### 3.2.3 Nachbuchung 

- Der Gast kann bis zum Ende des Besuchs noch Speisen und Getränke nachbestellen, z. B. einen Salat oder ein weiteres Getränk.

  *6/9 Punkte*

#### 3.2.4 Terminverschiebung 

- Der Gast kann den Termin bis 2 Stunden vorher verschieben, wenn dann noch genug Plätze frei sind.
- Sind keine Plätze frei, kann er nur stornieren.

  *7/9 Punkte (gesamt)*

#### 3.2.5 Timeout-Warnung

- Wenn ein Besuch nach 2 Stunden nicht beendet ist, wird der Kellner vom System darauf aufmerksam gemacht.

  *6/9 Punkte*

#### 3.2.6 Absence-Gebühr 

- Wenn der Gast nicht erscheint (kein Check-in), bekommt er eine Gebühr von 10 € verrechnet.

  *5/9 Punkte*

#### 3.2.7 Pufferzeit 

- Zwischen zwei Reservierungen gibt es eine Pufferzeit von 30 Minuten, damit der Platz geräumt und gesäubert werden kann.

  *7/9 Punkte*

### 3.3 Not necessary

#### 3.3.1 Gruppenreservierung
- Eine Person kann für eine Gruppe reservieren, z. B. für 10 Personen an einem Tisch.
- Die Person bekommt einen QR-Code und kann über das Kontextmenü die anderen Gäste benachrichtigen.
- Jeder weitere Gast bekommt seinen eigenen QR-Code.
- Jeder Gast kann mit seinem Code selbst Speisen und Getränke vorbestellen und bezahlen.

  *4/9 Punkte (gesamt)*

#### 3.3.2 Gruppen-Gebühren und Teilstorno
- Wird eine Gruppenreservierung zu spät storniert, kostet das 20 €.
- Kommt ein Gast der Gruppe nicht zum Check-in, bekommt er eine Absence-Gebühr von 10 €. Das gilt auch, wenn die ganze Gruppe nicht kommt.
- Jeder Gast der Gruppe kann bis 1 Stunde vorher kostenlos stornieren.
- Danach, bis kurz vor dem Termin (z. B. 30 Minuten), kostet es 10 € pro Person.
- In den letzten 15 Minuten und danach fällt eine Gebühr von z. B. 75 % des Preises an.

  *3/9 Punkte (gesamt)*

#### Bewertung der übrigen Abschnitte

| Abschnitt | Note | Begründung |
| ---- | ---- | ---- |
| 1 Einleitung (Problem, Ziel, Scope) | 7 | Problem und Ziel sind gut verständlich. Der Scope grenzt Gruppen ab, ist aber im Widerspruch zu 3.1.1 a. |
| 2 Stakeholder | 6 | Alle vier Rollen sind beschrieben. Der Koch hat aber kein einziges Requirement, und das Kassieren von Cash (Kellner:in) kommt in den Requirements nicht vor. |
| Struktur und Form | 4 | Keine eindeutigen IDs (nur Kapitelnummern), keine User Stories, keine nicht-funktionalen Anforderungen, Kapitel 4 fehlt (Nummerierung springt von 3 auf 5), uneinheitliche Schreibweise (Kellner / Kellner:in). |

#### Zusammenfassung

- Must-Have: Durchschnitt ca. 5,9. Grundablauf ist verständlich, aber Servieren und Verwalten sind zu ungenau.
- Maybe: Durchschnitt ca. 5,6. Zahlung und Feedback müssen aufgeteilt und genauer werden.
- Not necessary: Durchschnitt 3,5. Die Gebührenregeln für Gruppen sind widersprüchlich und nicht testbar.
- Größte Lücken: nicht-funktionale Anforderungen, Koch-Sicht, Verspätungsregel, Bestellfrist, Datenschutz.


---

## Neu hinzugefügte Requirements und User Stories

### Neue funktionale Requirements

#### R04 Speisekarte (Must-have)
- Der Gast kann die Speisekarte mit Preisen und verfügbaren Speisen sehen.

  *Fehlt bisher, obwohl der Gast Essen vorbestellen kann.*

#### R06 Küchenansicht (Must-have)
- Der Koch sieht alle Vorbestellungen mit Menge, Gast und Ankunftszeit.
- Stornierte Bestellungen werden nicht mehr als offen angezeigt.

  *Der Koch kommt bei den Stakeholdern vor, hat aber kein eigenes Requirement.*

#### R08 Platzübersicht (Must-have)
- Die Kellner:in kann für einen bestimmten Zeitpunkt sehen, welche Plätze frei, reserviert oder belegt sind.
- Die Übersicht wird beim Check-in, beim Stornieren und beim Beenden des Besuchs aktualisiert.

  *Die Platzübersicht für das Personal fehlt.*

#### R09 Barzahlung erfassen (Maybe)
- Die Kellner:in kann eine Barzahlung mit Betrag und Zeitpunkt im System eintragen.
- Der offene Betrag wird angepasst. Dieselbe Zahlung darf nicht doppelt erfasst werden.

  *Barzahlung ist bereits vorgesehen, aber nicht, wie sie im System erfasst wird.*

#### R11 Stornierung (Must-have)
- Wenn der Gast seine Reservierung storniert, wird auch seine Vorbestellung storniert und der QR-Code ungültig.

  *Diese Folgen einer Stornierung wurden nicht beschrieben.*

#### R14 Rollen und Berechtigungen (Must-have)
- Das Personal muss sich anmelden.
- Kellner:in, Koch und Manager können nur die Funktionen ihrer eigenen Rolle verwenden. Das System prüft die Berechtigung bei jedem geschützten Aufruf.
- Der Gast kann nur seine eigene Reservierung sehen.

  *Die Rollen sind beschrieben, aber die Anmeldung und die Berechtigungen fehlen.*

### Neue nicht-funktionale Requirements

#### R15 Datenerhalt (Must-have)
- Reservierungen, Bestellungen und Zahlungen bleiben auch nach einem normalen Neustart gespeichert.

#### R16a Datenschutz (Must-have)
- Der Gast kann keine Daten von fremden Reservierungen einsehen. Das Personal hat nur Zugriff auf die Daten, für die es berechtigt ist.

#### R16d QR-Sicherheit (Must-have)
- Die Zugriffscodes werden zufällig erzeugt (Vorschlag: mindestens 128 Bit Zufall).
- Im QR-Code stehen keine direkt lesbaren Kontaktdaten. Gesperrte Codes werden vom System abgelehnt.

#### R17b Smartphone-Bedienung (Must-have)
- Der Gast kann am Handy reservieren und seine Reservierung aufrufen.
- Vorschlag für den Test: Bei einer Bildschirmbreite von 360 Pixeln sind alle wichtigen Funktionen ohne seitliches Scrollen erreichbar.

#### R39a Reservierungsdauer (Maybe)
- Der Gast soll eine Reservierung in weniger als 2 Minuten abschließen können. Gemessen wird vom Öffnen des Formulars bis zur Bestätigung.

#### R39b Check-in-Reaktionszeit (Maybe)
- Nach dem Scannen und Erkennen des QR-Codes sollen die Daten innerhalb von 2 Sekunden angezeigt werden.

Die Testbedingungen und die beiden Zeitwerte müssen noch festgelegt bzw. bestätigt werden.

### Technische Sicherheitsvorgaben

#### R16b Passwörter (Must-have)
- Passwörter werden gehasht gespeichert und nicht im Klartext.

#### R16c Verbindung (Must-have)
- Die App verwendet bei der Bereitstellung HTTPS.

#### R16e Kreditkartendaten (Maybe)
- Wenn die Kreditkartenzahlung umgesetzt wird, dürfen keine Kreditkartendaten im eigenen System gespeichert werden.

Die Zahlungsfunktionen bleiben bei Maybe, wie im ursprünglichen Requirements-Dokument. Wenn eine Zahlungsfunktion umgesetzt wird, müssen auch die dazugehörigen Sicherheitsvorgaben erfüllt werden.

### Änderungsvorschläge für bestehende Requirements

- 3.1.1: Bei der normalen Reservierung nur einen Platz erlauben, da Gruppenreservierungen erst später kommen sollen. Doppelbuchungen müssen auch bei gleichzeitigen Anfragen verhindert werden (R02).
- 3.1.2, 3.1.4 und 3.1.6: Ungültige QR-Codes und doppelte Check-ins verhindern. Nach dem Besuch darf der QR-Code nicht mehr verwendet werden (R03, R07, R10).
- 3.1.3: Festlegen, bis wann der Gast vorbestellen kann.
- 3.1.6 und 3.2.1: Klären, was beim Beenden des Besuchs passiert, wenn noch etwas zu bezahlen ist (R10, R34).
- 3.1.7: Bei Onlinezahlung festlegen, wie das Geld abzüglich der Gebühren zurückerstattet wird. Doppelte Rückerstattungen verhindern (R26).
- 3.2.1: Die Zahlung in drei Requirements aufteilen (online im Voraus, bar beim Check-in und beim Beenden des Besuchs).
- 3.2.6: Festlegen, ab wann ein Gast als nicht erschienen gilt.
- 3.2.7: Die Pufferzeit von Maybe auf Must-have setzen, damit sie bei der Reservierung berücksichtigt wird.
- 3.3.2: Die Zeiten und Gebühren genau festlegen und in einer Tabelle zusammenfassen.
- Allen Requirements eine eindeutige ID geben und die Bezeichnungen der Rollen vereinheitlichen.
- Die Nummerierung korrigieren, da Kapitel 4 fehlt.

### Neue User Stories

#### US04 Speisekarte (R04)
Als Gast möchte ich die Speisekarte mit Preisen sehen, damit ich mein Essen auswählen kann.

Akzeptanzkriterien:
- Die aktuellen Preise und die Verfügbarkeit werden angezeigt.
- Nicht verfügbare Speisen können nicht bestellt werden.

#### US06 Küchenansicht (R06)
Als Koch möchte ich alle Vorbestellungen sehen, damit ich weiß, was ich wann vorbereiten muss.

Akzeptanzkriterien:
- Der Koch sieht die Speisen, Mengen, Gäste und Ankunftszeiten.
- Stornierte Bestellungen werden nicht mehr als offen angezeigt.

#### US08 Platzübersicht (R08)
Als Kellner:in möchte ich die Platzbelegung sehen, damit ich weiß, welche Plätze frei sind.

Akzeptanzkriterien:
- Die Plätze werden für den ausgewählten Zeitpunkt als frei, reserviert oder belegt angezeigt.
- Änderungen werden in der Übersicht übernommen.

#### US09 Barzahlung (R09)
Als Kellner:in möchte ich Barzahlungen eintragen, damit ich sehe, wie viel der Gast noch bezahlen muss.

Akzeptanzkriterien:
- Wenn von 30 € offenen Kosten 20 € bezahlt werden, bleiben 10 € offen.
- Dieselbe Zahlung darf nicht zweimal eingetragen werden.
- Beträge von 0 € oder weniger werden abgelehnt.

#### US11 Stornierung (R11)
Als Gast möchte ich meine Reservierung stornieren können, damit mein Platz wieder frei wird.

Akzeptanzkriterien:
- Eine Reservierung kann vor dem Check-in storniert werden.
- Die Vorbestellung wird storniert, der Platz freigegeben und der QR-Code gesperrt.
- Nach dem Check-in kann die Reservierung nicht mehr auf diese Weise storniert werden.

#### US14 Rollen (R14)
Als Mitarbeiter möchte ich mich anmelden können, damit ich die Funktionen meiner Rolle verwenden kann.

Akzeptanzkriterien:
- Das Personal muss angemeldet sein.
- Auch bei einem direkten Aufruf einer Funktion wird die Berechtigung geprüft.
- Gäste können nur ihre eigene Reservierung sehen.

#### US15 Datenerhalt (R15)
Als Mitarbeiter möchte ich nach einem Neustart noch auf meine gespeicherten Daten zugreifen können, damit ich weiterarbeiten kann.

Akzeptanzkriterium:
- Reservierungen, Bestellungen und Zahlungen sind nach einem normalen Neustart noch vorhanden.

#### US16 Sicherheit (R16a–e)
Als Gast möchte ich, dass meine Daten geschützt sind, damit andere nicht darauf zugreifen können.

Akzeptanzkriterien:
- Fremde Reservierungen können nicht aufgerufen werden.
- Passwörter werden gehasht gespeichert und die Verbindung verwendet HTTPS.
- QR-Zugriffscodes werden zufällig erzeugt.
- Bei Kreditkartenzahlung werden keine Kreditkartendaten im eigenen System gespeichert.

#### US17 Smartphone-Bedienung (R17b)
Als Gast möchte ich am Handy reservieren können, damit ich auch unterwegs einen Platz buchen kann.

Akzeptanzkriterium:
- Reservieren und Aufrufen der Reservierung sind am Handy möglich.

#### US39 Geschwindigkeit (R39a, R39b)
Als Nutzer möchte ich schnell reservieren und einchecken können, damit ich in der Mittagspause weniger Zeit verliere.

Akzeptanzkriterien (Vorschlag):
- Eine Reservierung dauert weniger als 2 Minuten.
- Die Daten werden nach der QR-Code-Erkennung innerhalb von 2 Sekunden angezeigt.

Die Testbedingungen müssen noch festgelegt werden.

---

### Ergebnis

Das ursprüngliche Requirements-Dokument deckt den wichtigsten Ablauf ab. Es fehlen aber noch einige Funktionen für das Personal und Anforderungen an die Sicherheit. Außerdem sind manche Requirements zu ungenau. Deshalb wurde das Dokument mit 5 von 9 Punkten bewertet. Mit den Ergänzungen und Änderungen wären aus unserer Sicht 7 bis 8 Punkte möglich.
