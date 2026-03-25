CREATE TABLE orders
(
    id           UUID PRIMARY KEY,
    client_id    UUID           NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    status       VARCHAR(50)    NOT NULL,
    created_at   TIMESTAMP      NOT NULL
);