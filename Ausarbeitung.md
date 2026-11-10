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

**Must-Have Requirements**

| Nr. | Requirement | Note | Begründung |
| ---- | ---- | ---- | ---- |
| 3.1.1 a | Plätze für Zeitpunkt und Dauer reservieren | 5 | "einen oder mehrere Plätze" ist unklar (Gruppe?). Keine Grenzen für die Dauer. |
| 3.1.1 b | System prüft freie Plätze | 6 | Klar und testbar. Die Pufferzeit ist aber nicht berücksichtigt (steht bei Maybe). |
| 3.1.1 c | Name, Tel., E-Mail, Zahlungsart angeben | 7 | Passt genau zur Angabe. Zahlungsart ist hier Must, "Zahlung" aber Maybe. |
| 3.1.2 | QR-Code erstellen und schicken | 6 | Gut, aber "schicken" ist offen (E-Mail? App?). |
| 3.1.3 | Mit QR-Code vorbestellen | 5 | Keine Frist, obwohl der Koch "rechtzeitig" planen soll. |
| 3.1.4 a | Kellner:in scannt QR-Code | 7 | Klar und testbar. |
| 3.1.4 b | Gast bekommt Platz zugewiesen | 6 | Offen: Was passiert bei Verspätung oder wenn der Platz noch belegt ist? |
| 3.1.5 | Essen und Getränke werden serviert | 4 | Sehr vage. Wer, wann, wie wird das geprüft? |
| 3.1.6 a | Gast beendet Besuch, Platz frei | 7 | Klar, passt zur Angabe. |
| 3.1.6 b | Kellner:in beendet Besuch | 7 | Klar, passt zur Angabe. |
| 3.1.7 a | Gast kann stornieren | 6 | Offen: Was passiert mit Vorbestellung und bereits Bezahltem? |
| 3.1.7 b | Bis 1 h kostenlos, danach 10 € | 7 | Zahlen sind testbar. Wie die 10 € verrechnet werden, steht nirgends. |
| 3.1.8 | Manager verwaltet Speisen, Getränke, Plätze | 4 | "Verwalten" ist vage (anlegen? ändern? löschen?) und drei Dinge in einem. |

**Maybe Requirements**
| Nr. | Requirement | Note | Begründung |
| ---- | ---- | ---- | ---- |
| 3.2.1 | Zahlung (online im Voraus, bar beim Check-in, beim Beenden) | 4 | Drei verschiedene Abläufe in einem Requirement (nicht atomar). "Bezahlung wird im QR-Code bestätigt" ist unklar. Widerspricht teilweise 3.1.1 c, wo die Zahlungsart schon Must ist. Bar-Kassieren durch Kellner:in steht nur bei den Stakeholdern. |
| 3.2.2 | Feedback nach dem Besuch | 4 | Zu vage: Sterne, Text, Pflicht oder freiwillig? Nicht testbar. |
| 3.2.3 | Nachbestellen bis Ende des Besuchs | 6 | Klar verständlich und testbar. Fehlt: Wie weiß der Koch davon, und gilt eine Frist? |
| 3.2.4 | Termin bis 2 h vorher verschieben | 7 | Zahlen sind testbar, Rückfall auf Storno ist klar. Offen: Gilt die Pufferzeit? Kostet Verschieben etwas? Unterschied zu Storno-Grenze (1 h) sollte begründet werden. |
| 3.2.5 | Warnung nach 2 h Besuchsdauer | 6 | Gut testbar (2 h). "aufmerksam gemacht" ist offen (Push? Anzeige im System?). Widerspricht evtl. der frei wählbaren Dauer aus 3.1.1 a. |
| 3.2.6 | Absence-Gebühr 10 € | 5 | Es fehlt, ab wann "kein Check-in" gilt (z. B. 15 min nach Termin?). Nicht klar, wie die Gebühr eingezogen wird, wenn nicht online bezahlt wurde. |
| 3.2.7 | Pufferzeit 30 Minuten | 7 | Klar, atomar und testbar. Sollte aber Must sein, weil 3.1.1 b davon abhängt. |

**Not necessary Requirements**

| Nr. | Requirement | Note | Begründung |
| ---- | ---- | ---- | ---- |
| 3.3.1 | Gruppenreservierung | 4 | Vier Dinge in einem (Reservieren, Benachrichtigen, eigene QR-Codes, Einzelbestellung). Widerspruch: Scope sagt "Gruppen erst später", aber 3.1.1 a erlaubt schon "mehrere Plätze". Maximale Gruppengröße fehlt. |
| 3.3.2 | Gruppen-Gebühren und Teilstorno | 3 | Schwach und teils widersprüchlich: "z. B." bei Zeiten und Prozentwerten ist nicht testbar. Die Lücke zwischen "kurz vor dem Termin" und "letzte 15 Minuten" ist nicht definiert. Absence-Gebühr 10 € "auch wenn die ganze Gruppe nicht kommt" kollidiert mit den 20 € bei Gruppenstorno. Sehr viele Entscheidungen in einem Block. |

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

In diesem Abschnitt befinden sich alle neu hizugefügenten Requirements. Eingeteilt in funktional (FR) und nicht-funktional (NFR) Requirements.

### Neue funktionale Requirements

| ID | Priorität | Requirement | Begründung / Lücke |
| ---- | ---- | ---- | ---- |
| FR-01 | Must | Der Gast kann eine Reservierung für max. 2 Stunden und mind. 30 Minuten buchen. Die Reservierung muss mindestens 30 Minuten im Voraus erfolgen. | Behebt "keine Grenzen für die Dauer" (3.1.1 a). |
| FR-02 | Must | Das System bestätigt die Reservierung per E-Mail an den Gast und sendet den QR-Code als Bild im Anhang. | Konkretisiert "schicken" in 3.1.2. |
| FR-03 | Must | Vorbestellungen sind bis 60 Minuten vor dem Termin möglich. | Behebt fehlende Frist in 3.1.3, damit der Koch planen kann. |
| FR-04 | Must | Der Koch sieht alle Vorbestellungen für den Tag, sortiert nach Termin. | Koch hatte bisher kein Requirement. |
| FR-05 | Must | Der Koch kann eine Speise als "fertig" markieren, die Kellner:in sieht den Status. | Macht "Servieren" (3.1.5) prüfbar. |
| FR-06 | Must | Die Kellner:in kann eine Speise als "serviert" markieren. | Macht 3.1.5 testbar (wer, wann). |
| FR-07 | Must | Erscheint der Gast mehr als 15 Minuten nach Terminbeginn nicht zum Check-in, verfällt die Reservierung, und die Plätze werden freigegeben. | Fehlende Verspätungsregel (3.1.4 b, 3.2.6). |
| FR-08 | Must | Der Manager kann Speisen, Getränke und Plätze jeweils anlegen, ändern und deaktivieren. | Teilt das vage "verwalten" (3.1.8) in konkrete Funktionen auf. |
| FR-09 | Must | Der Gast sieht vor der Bestellung die Speisekarte mit Preisen und Allergenen. | Fehlt komplett, in der Gastronomie rechtlich relevant. |
| FR-10 | Must | Bei Storno wird die Vorbestellung automatisch mitstorniert, bereits online Bezahltes wird zurückerstattet (abzüglich einer eventuellen Gebühr). | Klärt offene Frage aus 3.1.7 a. |
| FR-11 | Should | Die Kellner:in kann Barzahlung im System als "bezahlt" verbuchen. | Cash wird bei Stakeholdern genannt, aber nirgends gefordert. |
| FR-12 | Should | Die Kellner:in sieht eine Übersicht aller Plätze mit Status (frei, reserviert, belegt, Pufferzeit). | Unterstützt Platzzuweisung und Bufferzeit. |
| FR-13 | Could | Der Manager sieht eine Auslastungsübersicht (Anzahl Reservierungen und Gäste pro Tag und Stunde). | Stakeholder-Ziel "gut ausgelastet" hat bisher keine Funktion. |
| FR-14 | Could | Der Gast sieht eine Liste seiner aktuellen und vergangenen Reservierungen. | Nötig zum Stornieren und Verschieben. |

### Neue nicht-funktionale Requirements

| ID | Kategorie | Requirement |
| ---- | ---- | ---- |
| NFR-01 | Performance | Die Verfügbarkeitsprüfung bei der Reservierung antwortet in höchstens 3 Sekunden. |
| NFR-02 | Usability | Ein Gast kann eine Reservierung in höchstens 5 Schritten und unter 2 Minuten abschließen. |
| NFR-03 | Sicherheit | Jeder QR-Code ist eindeutig, nicht erratbar und nach Besuchsende ungültig. |
| NFR-04 | Datenschutz | Personenbezogene Daten (Name, Telefon, E-Mail) werden DSGVO-konform gespeichert und nach 12 Monaten gelöscht. |
| NFR-05 | Zahlungssicherheit | Kreditkartendaten werden nicht im System gespeichert, die Abwicklung läuft über einen externen Zahlungsanbieter. |
| NFR-06 | Verfügbarkeit | Das System ist während der Öffnungszeiten (Mittagspause) zu mindestens 99 % verfügbar. |
| NFR-07 | Kompatibilität | Die App läuft auf aktuellen Versionen von Android und iOS (alternativ als Web-App in gängigen Browsern). |
| NFR-08 | Zuverlässigkeit | Zwei Gäste können nie denselben Platz im selben Zeitraum reservieren (keine Doppelbuchung, auch bei gleichzeitigen Anfragen). |

### Änderungsvorschläge für bestehende Requirements

- **3.2.7 Pufferzeit** von Maybe auf **Must** setzen (3.1.1 b hängt davon ab).
- **3.1.1 a** auf "einen Platz" einschränken, "mehrere Plätze" in 3.3.1 (Gruppenreservierung) belassen, damit der Scope-Widerspruch verschwindet.
- **3.2.1 Zahlung** in drei einzelne Requirements aufteilen (Online-Vorauszahlung, Barzahlung beim Check-in, Zahlung beim Beenden).
- **3.3.2** Zahlen und Zeitgrenzen festlegen (kein "z. B."), Tabelle der Gebühren erstellen.
- Eindeutige IDs für alle Requirements (z. B. `REQ-3.1.1-a`) und einheitliche Schreibweise (Kellner:in).
- Fehlendes Kapitel 4 ergänzen oder Nummerierung korrigieren.

### Neue User Stories

Format: *Als [Rolle] möchte ich [Funktion], damit [Nutzen].* Mit Akzeptanzkriterien (AK).
 
**US-01 [NEU] Reservieren** (zu 3.1.1, FR-01)
Als Gast möchte ich einen Platz für meine Mittagspause reservieren, damit ich nicht nach einem freien Tisch suchen muss.
- AK: Ich wähle Datum, Uhrzeit und Dauer (30 bis 120 Minuten).
- AK: Ist kein Platz frei, zeigt das System freie Alternativzeiten an.
- AK: Nach Abschluss erhalte ich eine Bestätigung mit QR-Code.

**US-02 [NEU] Vorbestellen** (zu 3.1.3, FR-03)
Als Gast möchte ich Essen und Getränke vorbestellen, damit ich sofort nach dem Hinsetzen essen kann.
- AK: Vorbestellen ist bis 60 Minuten vor dem Termin möglich.
- AK: Ich sehe Preise und Allergene und die Gesamtsumme.

**US-03 [NEU] Bestellungen sehen** (zu FR-04)
Als Koch möchte ich alle Vorbestellungen nach Uhrzeit sortiert sehen, damit ich die Zubereitung rechtzeitig planen kann.
- AK: Neue Bestellungen erscheinen sofort in der Liste.
- AK: Stornierte Bestellungen verschwinden aus der Liste.

**US-04 [NEU] Speise fertig melden** (zu FR-05)
Als Koch möchte ich eine Speise als fertig markieren, damit die Kellner:in sie sofort servieren kann.
- AK: Die Kellner:in sieht den Status "fertig" in der Übersicht.

**US-05 [NEU] Check-in** (zu 3.1.4)
Als Kellner:in möchte ich den QR-Code scannen und dem Gast einen Platz zuweisen, damit der Check-in in wenigen Sekunden erledigt ist.
- AK: Bei gültigem Code sehe ich Name, Platz und Bestellung.
- AK: Bei ungültigem, abgelaufenem oder bereits genutztem Code zeigt das System eine Fehlermeldung.

**US-06 [NEU] Servieren** (zu 3.1.5, FR-06)
Als Kellner:in möchte ich sehen, welche Bestellung an welchen Platz gehört, damit ich sie richtig serviere.
- AK: Ich kann jede Position als "serviert" markieren.

**US-07 [NEU] Besuch beenden** (zu 3.1.6)
Als Gast möchte ich meinen Besuch selbst beenden, damit ich nicht auf die Kellner:in warten muss.
- AK: Nach dem Beenden ist der Platz im System sofort als "Pufferzeit" markiert und nach 30 Minuten wieder frei.

**US-08 [NEU] Stornieren** (zu 3.1.7, FR-10)
Als Gast möchte ich meine Reservierung stornieren, falls sich meine Pläne ändern.
- AK: Bis 60 Minuten vor Termin ist Storno kostenlos, danach werden 10 € verrechnet. Das System zeigt mir die Gebühr vor der Bestätigung.
- AK: Meine Vorbestellung wird mitstorniert, bereits Bezahltes wird zurückerstattet.

**US-09 [NEU] Verwalten** (zu 3.1.8, FR-08)
Als Manager möchte ich Speisen, Getränke und Plätze anlegen, ändern und deaktivieren, damit die Speisekarte und die Platzliste immer aktuell sind.
- AK: Deaktivierte Speisen können nicht mehr bestellt werden, bestehende Bestellungen bleiben gültig.
- AK: Deaktivierte Plätze werden bei neuen Reservierungen nicht mehr vergeben.

**US-10 [NEU] Auslastung** (zu FR-13)
Als Manager möchte ich die Auslastung pro Tag und Stunde sehen, damit ich Personal und Einkauf besser planen kann.
 
**US-11 [NEU] Barzahlung** (zu 3.2.1, FR-11)
Als Kellner:in möchte ich eine Barzahlung im System verbuchen, damit der Gast als bezahlt gilt.
 
**US-12 [NEU] Nachbestellen** (zu 3.2.3)
Als Gast möchte ich während des Besuchs noch etwas nachbestellen, damit ich nicht aufstehen und anstehen muss.
- AK: Die Nachbestellung erscheint beim Koch und wird der Rechnung hinzugefügt.

**US-13 [NEU] Verschieben** (zu 3.2.4)
Als Gast möchte ich meinen Termin verschieben, wenn meine Pause länger oder kürzer wird, damit ich nicht stornieren muss.
- AK: Verschieben ist bis 2 Stunden vorher möglich, wenn im neuen Zeitraum ein Platz frei ist.

**US-14 [NEU] Timeout-Warnung** (zu 3.2.5)
Als Kellner:in möchte ich benachrichtigt werden, wenn ein Besuch die reservierte Dauer überschreitet, damit ich den Platz rechtzeitig für den nächsten Gast vorbereiten kann.
 
**US-15 [NEU] Feedback** (zu 3.2.2)
Als Gast möchte ich nach dem Besuch eine Bewertung von 1 bis 5 Sternen und optional einen Kommentar abgeben, damit das Restaurant sich verbessern kann.
 
---

### Ergebnis
 
Das Requirements-Dokument von Gruppe 1 ist inhaltlich gut und deckt den Grundablauf ab (**5 von 9**). Mit den genannten Änderungen (eindeutige IDs, atomare Requirements, genaue Zahlen, Koch-Sicht, nicht-funktionale Anforderungen und User Stories) wäre eine Bewertung von **7 bis 8 von 9** erreichbar.