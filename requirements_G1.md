# REAP – Requirements-Dokument

## 1. Einleitung

### 1.1 Problemstellung und Ziel

An der HTL Leoben ist die Mittagspause kurz. Wenn man essen gehen will, geht viel Zeit für Platzsuche, Anstehen, Bestellen und Bezahlen drauf. Das Restaurant weiß außerdem vorher nicht, wie viele Gäste kommen.

Mit unserer App REAP kann man vorher einen Platz reservieren und gleich Essen vorbestellen. Man bekommt einen QR-Code und checkt damit im Restaurant ein. So spart man Zeit und das Restaurant kann besser planen.

### 1.2 Scope

Die App deckt den Ablauf vom Reservieren bis zum Beenden des Besuchs ab. Gruppenreservierungen kommen erst später.

## 2. Stakeholder

### 2.1 Gast / Kundin

Schüler:innen und Lehrpersonen, die in der Mittagspause essen gehen. Sie wollen Zeit sparen. Sie reservieren einen Platz, bestellen vor, zahlen und beenden den Besuch mit dem QR-Code.

### 2.2 Kellner:in

Scannt den QR-Code beim Check-in und weist den Platz zu. Sie serviert das vorbestellte Essen. Wenn der Gast fertig ist, gibt sie den Platz wieder frei. Sie kann auch Cash kassieren.

### 2.3 Koch

Bereitet die Speisen zu, die vorbestellt wurden. Er braucht die Bestellungen rechtzeitig, damit er planen kann.

### 2.4 Restaurant Manager:in

Verwaltet Speisekarte, Getränke und Plätze im System. Sie will, dass das Restaurant gut ausgelastet ist und der Ablauf funktioniert.

## 3. Anforderungen nach Priorität

### 3.1 Must-have

#### 3.1.1 Reservieren
- Der Gast kann einen oder mehrere Plätze für einen bestimmten Zeitpunkt und eine bestimmte Dauer reservieren.
- Das System prüft, ob im gewünschten Zeitraum genug Plätze frei sind.
- Der Gast gibt Name, Telefonnummer, E-Mail und Zahlungsart (Kreditkarte, PayPal oder Cash) an.

#### 3.1.2 QR-Code
- Wenn die Reservierung klappt, erstellt das System einen QR-Code und schickt ihn dem Gast.

#### 3.1.3 Vorbestellen
- Der Gast kann mit dem QR-Code Speisen und Getränke vorbestellen.

#### 3.1.4 Check-in und Platzzuweisung
- Die Kellner:in scannt den QR-Code beim Check-in.
- Der Gast bekommt seinen reservierten Platz zugewiesen.

#### 3.1.5 Servieren
- Die vorbestellten Speisen und Getränke werden serviert.

#### 3.1.6 Besuch beenden, Platz freigeben
- Der Gast kann seinen Besuch selbst beenden (Kontextmenü im QR-Code). Danach ist der Platz wieder frei.
- Der Kellner kann den Besuch beenden und den Platz freigeben, wenn sie merkt, dass der Gast gegangen ist.

#### 3.1.7 Stornieren
- Der Gast kann seine Reservierung stornieren.
- Bis 1 Stunde vor dem Termin ist das kostenlos, danach kostet es 10 €.

#### 3.1.8 Verwaltung durch Manager:in
- Der Manager kann Speisen, Getränke und Plätze im System verwalten.

### 3.2 Maybe

#### 3.2.1 Zahlung 

- Der Gast kann im Voraus online bezahlen (Kreditkarte oder PayPal). Die Bezahlung wird im QR-Code bestätigt.
- Der Gast kann beim Check-in bar bezahlen.
- Beim Beenden des Besuchs kann der Gast online bezahlen oder bar beim Kellner.
#### 3.2.2 Feedback 

- Nach dem Besuch kann der Gast online Feedback geben.

#### 3.2.3 Nachbuchung 

- Der Gast kann bis zum Ende des Besuchs noch Speisen und Getränke nachbestellen, z. B. einen Salat oder ein weiteres Getränk.

#### 3.2.4 Terminverschiebung 

- Der Gast kann den Termin bis 2 Stunden vorher verschieben, wenn dann noch genug Plätze frei sind.
- Sind keine Plätze frei, kann er nur stornieren.

#### 3.2.5 Timeout-Warnung

- Wenn ein Besuch nach 2 Stunden nicht beendet ist, wird der Kellner vom System darauf aufmerksam gemacht.

#### 3.2.6 Absence-Gebühr 

- Wenn der Gast nicht erscheint (kein Check-in), bekommt er eine Gebühr von 10 € verrechnet.

#### 3.2.7 Pufferzeit 

- Zwischen zwei Reservierungen gibt es eine Pufferzeit von 30 Minuten, damit der Platz geräumt und gesäubert werden kann.

### 3.3 Not necessary

#### 3.3.1 Gruppenreservierung
- Eine Person kann für eine Gruppe reservieren, z. B. für 10 Personen an einem Tisch.
- Die Person bekommt einen QR-Code und kann über das Kontextmenü die anderen Gäste benachrichtigen.
- Jeder weitere Gast bekommt seinen eigenen QR-Code.
- Jeder Gast kann mit seinem Code selbst Speisen und Getränke vorbestellen und bezahlen.

#### 3.3.2 Gruppen-Gebühren und Teilstorno
- Wird eine Gruppenreservierung zu spät storniert, kostet das 20 €.
- Kommt ein Gast der Gruppe nicht zum Check-in, bekommt er eine Absence-Gebühr von 10 €. Das gilt auch, wenn die ganze Gruppe nicht kommt.
- Jeder Gast der Gruppe kann bis 1 Stunde vorher kostenlos stornieren.
- Danach, bis kurz vor dem Termin (z. B. 30 Minuten), kostet es 10 € pro Person.
- In den letzten 15 Minuten und danach fällt eine Gebühr von z. B. 75 % des Preises an.


## 5. Quellen

- SYP Skriptum: 03 Requirements Process
- SYP Skriptum: 04 Requirements Elicitation
- SYP Skriptum: 05 Requirements Analysis
- SWEBOK v4