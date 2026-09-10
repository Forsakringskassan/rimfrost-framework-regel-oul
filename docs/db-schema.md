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
    handlaggning_id         UUID         NOT NULL,
    event_id                UUID         NOT NULL,
    kogito_process_id       VARCHAR(255),
    kogito_process_instance_id UUID,
    kogito_root_process_instance_id UUID,
    kogito_root_process_id  VARCHAR(255),
    kogito_business_key     VARCHAR(255),
    reply_to                VARCHAR(255),
    version                 BIGINT       NOT NULL DEFAULT 0,
    created_at              TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT now(),
    PRIMARY KEY (handlaggning_id)
);
```

---

## {prefix}_common_data

Lagrar uppgifts-ID och OUL:s uppgifts-ID efter att en OUL-uppgift skapats.

```sql
CREATE TABLE {prefix}_common_data (
    handlaggning_id UUID         NOT NULL,
    uppgift_id      UUID,
    uppgift_version BIGINT,
    uppgift_status  VARCHAR(255),
    uppgift_utforare_id VARCHAR(255),
    uppgift_planerad_tidsstampel TIMESTAMP,
    oul_uppgift_id  UUID,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    PRIMARY KEY (handlaggning_id)
);
```

---

## {prefix}_process_topic_info

Lagrar routing till reply-subtopic för OUL-statusnotifieringar.

```sql
CREATE TABLE {prefix}_process_topic_info (
    handlaggning_id UUID         NOT NULL,
    reply_topic     VARCHAR(255),
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
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
