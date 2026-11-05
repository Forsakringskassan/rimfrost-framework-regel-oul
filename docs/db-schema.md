# Databasschema — rimfrost-framework-regel-oul

Ramverket tillhandahåller inga egna Flyway-migrationer. Den konsumerande regelservicen ansvarar
för att skapa tabellerna nedan via sin egen Flyway-migreringskedja.

Tabellnamnen styrs av `regel.persistence.table-prefix` (konfigurerat av konsumenten). I exemplen
nedan används prefixet `{prefix}` som platshållare.

---

## {prefix}_cloud_event_data

Lagrar CloudEvent-attributen från den inkommande regelförfrågan för korrelation.

```sql
CREATE TABLE {prefix}_cloud_event_data (
    handlaggning_id        UUID         NOT NULL,
    event_id               UUID         NOT NULL,
    kogitorootprociid      UUID         NOT NULL,
    kogitoparentprociid    UUID         NOT NULL,
    kogitoprocinstanceid   UUID         NOT NULL,
    kogitorootprocid       VARCHAR(255) NOT NULL,
    kogitoprocid           VARCHAR(255) NOT NULL,
    kogitoprocist          VARCHAR(255),
    kogitoprocversion      VARCHAR(255),
    type                   VARCHAR(255) NOT NULL,
    source                 VARCHAR(255) NOT NULL,
    version                BIGINT       NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (handlaggning_id)
);
```

---

## {prefix}_common_data

Lagrar uppgifts-ID och OUL:s uppgifts-ID efter att en OUL-uppgift skapats.

```sql
CREATE TABLE {prefix}_common_data (
    handlaggning_id               UUID         NOT NULL,
    uppgift_id                    UUID         NOT NULL,
    uppgift_version               INTEGER      NOT NULL,
    uppgift_skapad_ts             TIMESTAMPTZ  NOT NULL,
    uppgift_utford_ts             TIMESTAMPTZ,
    uppgift_planerad_ts           TIMESTAMPTZ,
    uppgift_utforar_id_typ_id     VARCHAR(255),
    uppgift_utforar_id_varde      VARCHAR(255),
    uppgift_status                VARCHAR(255) NOT NULL,
    uppgift_aktivitet_id          UUID         NOT NULL,
    uppgift_fssa_information      VARCHAR(255) NOT NULL,
    uppgift_specifikation_id      UUID         NOT NULL,
    uppgift_specifikation_version INTEGER      NOT NULL,
    oul_uppgift_id                UUID,
    version                       BIGINT       NOT NULL DEFAULT 0,
    created_at                    TIMESTAMPTZ  NOT NULL,
    updated_at                    TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (handlaggning_id)
);
```

---

## Flyway-konfiguration (konsumenten)

Konsumenten konfigurerar Flyway och tabellprefixet i sin `application.properties`:

```properties
quarkus.flyway.default-schema=<schema>
quarkus.flyway.migrate-at-start=true
quarkus.flyway.create-schemas=true
quarkus.flyway.schemas=${quarkus.flyway.default-schema}
quarkus.hibernate-orm.physical-naming-strategy=se.fk.rimfrost.framework.regel.oul.storage.internal.RegelPhysicalNamingStrategy
regel.persistence.table-prefix=<prefix>
```
