CREATE TABLE common_data (
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
