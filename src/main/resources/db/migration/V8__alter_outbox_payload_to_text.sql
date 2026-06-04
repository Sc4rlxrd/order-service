ALTER TABLE outbox_events
ALTER
COLUMN payload TYPE TEXT
USING payload::text;