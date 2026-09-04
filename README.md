# rimfrost-framework-regel-oul

Ramverkskomponent som tillhandahåller den OUL-integration (Operativt Uppgiftslager)
och den korrelationslagring som krävs för att en regelkörning ska kunna avslutas i ett
separat anrop från det som startade den — typiskt vid manuell handläggning där en
handläggare markerar en OUL-uppgift som klar via handläggarportalen.

Baseras på [rimfrost-framework-regel](https://github.com/Forsakringskassan/rimfrost-framework-regel)
och konsumeras av regelramverk som behöver OUL-uppgifter, till exempel
`rimfrost-framework-regel-manuell` och `rimfrost-framework-regel-komplettering`.

> Se [`docs/krav.md`](docs/krav.md) för fullständig kravdefinition.

## Aktörer

| Aktör                             | Roll                                                                        |
|-----------------------------------|-----------------------------------------------------------------------------|
| Konsumerande regelramverk         | Skapar och avslutar OUL-uppgifter via detta ramverk                         |
| OUL (Operativt Uppgiftslager)     | Tar emot uppgifter, hanterar tilldelning och publicerar statusnotifieringar |
| Handläggningstjänsten             | Uppdateras med uppgiftsreferens och statusinformation                       |

## Ansvarsområden

- **OUL-uppgiftshantering** — skapar och avslutar OUL-uppgifter (`createOperativUppgift`,
  `tryEndOperativUppgift`) och synkroniserar uppgiftsdata till handläggningstjänsten.
- **Statusnotifieringar** — prenumererar på OUL:s statusuppdateringar via Kafka och
  synkroniserar aktuell uppgiftsstatus till handläggningstjänsten.
- **Korrelationslagring** — persisterar CloudEvent-attribut, `replyTo`, `ProcessTopicInfo`
  och `RegelCommonData` per handläggning så att regelkörningen kan avslutas långt efter
  att den startades.

## Användning

Inject `OulUppgiftService` och anropa `createOulUppgift` med ett `OulUppgiftSpec`:

```java
@Inject
OulUppgiftService oulUppgiftService;

OperativUppgift uppgift = oulUppgiftService.createOulUppgift(
    ImmutableOulUppgiftSpec.builder()
        .handlaggningId(handlaggningId)
        .handlaggning(handlaggning)
        .replyTo(replyTo)
        .cloudEventData(cloudEventData)
        .cloudEventAttributes(cloudEventAttributes)
        .regel("min-regel")
        .beskrivning("Beskriving av uppgiften")
        .verksamhetslogik("min-verksamhet")
        .roll("handlaggare")
        .url("/min-regel/uppgift")
        .erbjudande(erbjudande)
        .aktivitetId(aktivitetId)
        .uppgiftSpecifikationId(uppgiftSpecifikationId)
        .uppgiftSpecifikationVersion(1)
        .build());
```

OUL-statusnotifieringar hanteras automatiskt av ramverket via `OulHandlerInterface` —
konsumenten behöver inte implementera något för status callbacks.

## Konfiguration

| Egenskap                          | Beskrivning                                                                  |
|-----------------------------------|------------------------------------------------------------------------------|
| `regel.persistence.table-prefix`  | Unikt prefix för ramverkets databastabeller, t.ex. `rtf_manuell`             |
| `kafka.subtopic`                  | Reply-subtopic som OUL använder för att skicka statusnotifieringar tillbaka  |
| `quarkus.datasource.jdbc.url`     | JDBC-URL till databasen (PostgreSQL)                                         |
| `quarkus.flyway.default-schema`   | Databasschema som Flyway migrerar och som ramverkets tabeller skapas i       |

## Persistens

Ramverket skapar tre tabeller per regelimplementation:

| Tabell                          | Innehåll                                                    |
|---------------------------------|-------------------------------------------------------------|
| `{prefix}_common_data`          | OUL-uppgifts-ID och tillhörande uppgiftsmetadata            |
| `{prefix}_cloud_event_data`     | CloudEvent-attribut från regelförfrågan för korrelation vid avslut |
| `{prefix}_process_topic_info`   | `replyTo` och routing till reply-subtopic för OUL-statusnotifieringar |

Prefixet konfigureras via `regel.persistence.table-prefix` och måste vara unikt per
regelimplementation. Migrationer hanteras av Flyway.
