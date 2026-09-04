-- Test-side framework tables for regel-oul.
--
-- These mirror the main migrations under src/main/resources/db/migration/, but
-- with the regel_oul_test_ prefix baked in — matching what
-- RegelPhysicalNamingStrategy will produce at runtime when
-- regel.persistence.table-prefix=regel_oul_test.
--
-- The main migrations are intentionally unprefixed because they belong to the
-- consuming service, not this framework. In production consumers apply their
-- own prefix (e.g. rtf_barnbidrag_) and provide their own migrations.

CREATE TABLE regel_oul_test_cloud_event_data (
    handlaggning_id      UUID         NOT NULL PRIMARY KEY,
    event_id             UUID         NOT NULL,
    kogitorootprociid    UUID,
    kogitoparentprociid  UUID,
    kogitoprocinstanceid UUID,
    kogitorootprocid     VARCHAR(255),
    kogitoprocid         VARCHAR(255),
    kogitoprocist        VARCHAR(255),
    kogitoprocversion    VARCHAR(255),
    type                 VARCHAR(255),
    source               VARCHAR(255),
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL
);

CREATE TABLE regel_oul_test_common_data (
    handlaggning_id                UUID         NOT NULL PRIMARY KEY,
    uppgift_id                     UUID,
    uppgift_version                INTEGER      NOT NULL DEFAULT 0,
    uppgift_skapad_ts              TIMESTAMPTZ,
    uppgift_utford_ts              TIMESTAMPTZ,
    uppgift_planerad_ts            TIMESTAMPTZ,
    uppgift_utforar_id_typ_id      VARCHAR(255),
    uppgift_utforar_id_varde       VARCHAR(255),
    uppgift_status                 VARCHAR(255),
    uppgift_aktivitet_id           UUID,
    uppgift_fssa_information       VARCHAR(255),
    uppgift_specifikation_id       UUID,
    uppgift_specifikation_version  INTEGER,
    oul_uppgift_id                 UUID,
    version                        BIGINT       NOT NULL DEFAULT 0,
    created_at                     TIMESTAMPTZ  NOT NULL,
    updated_at                     TIMESTAMPTZ  NOT NULL
);

CREATE TABLE regel_oul_test_process_topic_info (
    handlaggning_id  UUID         NOT NULL PRIMARY KEY,
    reply_topic      VARCHAR(255) NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);
