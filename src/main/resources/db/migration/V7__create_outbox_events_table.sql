CREATE TABLE outbox_events
(
    id             UUID PRIMARY KEY,
    event_id       UUID         NOT NULL UNIQUE,

    aggregate_id   UUID         NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,

    payload        JSONB        NOT NULL,

    status         VARCHAR(30)  NOT NULL,
    retry_count    INT          NOT NULL DEFAULT 0,

    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at   TIMESTAMP NULL,

    last_error     TEXT NULL
);

CREATE INDEX idx_outbox_events_status_created_at
    ON outbox_events (status,
                      created_at);

CREATE INDEX idx_outbox_events_aggregate_id
    ON outbox_events (aggregate_id);