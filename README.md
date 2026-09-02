# rimfrost-framework-regel-oul

Ramverkskomponent som tillhandahåller den OUL-integration (Operativt Uppgiftslager)
och den korrelationslagring som krävs för att en regelkörning ska kunna avslutas i ett
separat anrop från det som startade den — typiskt vid manuell handläggning där en
handläggare markerar en OUL-uppgift som klar via handläggarportalen.

Baseras på [rimfrost-framework-regel](https://github.com/Forsakringskassan/rimfrost-framework-regel)
och konsumeras av regelramverk som behöver OUL-uppgifter, till exempel
`rimfrost-framework-regel-manuell` och `rimfrost-framework-regel-komplettering`.

> Ramverket är under initial extraktion från `rimfrost-framework-regel-manuell`.
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

## Persistens

Ramverket skapar tre tabeller per regelimplementation:

| Tabell                          | Innehåll                                                    |
|---------------------------------|-------------------------------------------------------------|
| `{prefix}_common_data`          | OUL-uppgifts-ID och tillhörande uppgiftsmetadata            |
| `{prefix}_cloud_event_data`     | CloudEvent-attribut och `replyTo` för korrelation vid avslut |
| `{prefix}_process_topic_info`   | Routing till reply-subtopic för OUL-statusnotifieringar     |

Prefixet konfigureras via `regel.persistence.table-prefix` och måste vara unikt per
regelimplementation. Migrationer hanteras av Flyway.
