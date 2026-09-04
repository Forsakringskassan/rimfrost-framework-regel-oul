CREATE TABLE cloud_event_data (
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
