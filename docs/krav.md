# Krav — rimfrost-framework-regel-oul

Detta ramverk tillhandahåller OUL-uppgiftshantering, prenumeration på OUL-statusnotifieringar
samt persistens av den korrelation som krävs för att avsluta en regelkörning i ett separat
anrop från det som startade den.

Kraven här beskriver endast det som är implementerat i detta ramverk. Ärvda krav från
underliggande ramverk upprepas inte.

## 1. Funktionella krav

### FROUL-FR-01 — OUL-uppgiftshantering

- **FROUL-FR-01.1** Ramverket ska tillhandahålla en operation för att skapa en OUL-uppgift via
  OUL-tjänsten (`createOperativUppgift`).
- **FROUL-FR-01.2** Den skapade OUL-uppgiften ska innehålla regelns namn, beskrivning,
  affärslogiktyp och roll.
- **FROUL-FR-01.3** Den skapade OUL-uppgiften ska innehålla URL till regelns REST-gränssnitt för
  användning av handläggarportalen.
- **FROUL-FR-01.4** Den skapade OUL-uppgiften ska innehålla CloudEvent-attributen från den
  inkommande regelförfrågan, för att möjliggöra korrelation.
- **FROUL-FR-01.5** Den skapade OUL-uppgiften ska ange ett reply-subtopic som OUL använder för
  statusnotifieringar till ramverket.
- **FROUL-FR-01.6** Ramverket ska efter skapandet lagra uppgiftens metadata (uppgifts-ID, OUL:s
  uppgifts-ID) persistent via `RegelCommonData`.
- **FROUL-FR-01.7** Ramverket ska efter skapandet uppdatera handläggningsärendet med
  uppgiftsreferens och uppgiftsspecifikation.
- **FROUL-FR-01.8** Ramverket ska inte inkludera individer i OUL-skapandeförfrågan. OUL hämtar
  individinformation internt vid behov via det `handlaggningId` som uppgiften är knuten till.
- **FROUL-FR-01.9** Ramverket ska tillhandahålla en operation för att avsluta en OUL-uppgift
  (`tryEndOperativUppgift`) med angiven orsak.

### FROUL-FR-02 — Hantering av OUL-statusnotifieringar

- **FROUL-FR-02.1** Ramverket ska prenumerera på OUL:s statusnotifieringar via Kafka.
- **FROUL-FR-02.2** Vid statusnotifiering ska ramverket uppdatera uppgiftens version, status,
  utförar-ID och planerad tidsstämpel i den lagrade uppgiften.
- **FROUL-FR-02.3** Ramverket ska synkronisera uppdaterad uppgiftsstatus till handläggningstjänsten.
- **FROUL-FR-02.4** Statusuppdatering ska ske utan att handläggningsärendets egen version
  inkrementeras.
- **FROUL-FR-02.5** Om en OUL-statusnotifiering tas emot för en handläggning utan lagrad
  `RegelCommonData` ska ramverket ignorera notifieringen utan att avsluta uppgiften eller
  skicka ett felmeddelande.

### FROUL-FR-03 — Persistens av korrelationstillstånd

- **FROUL-FR-03.1** Ramverket ska lagra CloudEvent-attributen från den inkommande regelförfrågan
  persistent för korrelation under hela regelkörningens livscykel.
- **FROUL-FR-03.2** Ramverket ska lagra `replyTo` från den inkommande regelförfrågan persistent
  för återkoppling vid regelkörningens avslut.
- **FROUL-FR-03.3** Ramverket ska lagra `ProcessTopicInfo` (routing till reply-subtopic för
  OUL-statusnotifieringar) persistent per handläggning.
- **FROUL-FR-03.4** Ramverket ska lagra `RegelCommonData` (uppgifts-ID och OUL:s uppgifts-ID)
  persistent per handläggning.
- **FROUL-FR-03.5** Ramverket ska tillhandahålla en operation för att rensa samtliga lagrade
  korrelationsdata, processroutingdata och uppgiftsmetadata efter avslutad regelkörning.
- **FROUL-FR-03.6** Rensningsoperationerna ska genomföras med bästa möjliga ansträngning —
  fel i enskilda rensningar ska inte hindra övriga rensningar.

---

## 2. Persistenskrav

### FROUL-PR-01 — Tabeller och namngivning

- **FROUL-PR-01.1** Ramverket ska skapa tre tabeller i databasen med konfigurerbart prefix:
  `{prefix}_common_data`, `{prefix}_cloud_event_data` och `{prefix}_process_topic_info`.
- **FROUL-PR-01.2** Tabellprefixet ska vara konfigurerbart och unikt per regelimplementation
  för att möjliggöra deployment av flera regler i samma databas.
- **FROUL-PR-01.3** Ramverket ska rejecta uppstart om tabellprefixet inte är konfigurerat.
- **FROUL-PR-01.4** Databasmigrationer ska hanteras via Flyway och köras automatiskt vid uppstart.

---

## 3. Icke-funktionella krav

### FROUL-NFR-01 — Tillförlitlighet

- **FROUL-NFR-01.1** Optimistisk låsning ska förhindra att samtida anrop mot samma
  handläggningsärende skriver över varandra.

### FROUL-NFR-02 — Observerbarhet

- **FROUL-NFR-02.1** Alla fel i integrationer mot OUL och handläggningstjänsten ska loggas med
  tillräcklig information för felsökning.
- **FROUL-NFR-02.2** Misslyckade rensningsoperationer ska loggas samlat utan att hindra andra
  operationer.
