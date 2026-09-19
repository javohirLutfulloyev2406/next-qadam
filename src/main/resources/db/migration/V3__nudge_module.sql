-- ============================================================
-- tasks: snooze va adaptive-shrink uchun holat
-- ============================================================
ALTER TABLE tasks
    ADD COLUMN consecutive_snooze_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE tasks
    ADD COLUMN snoozed_until TIMESTAMP NULL;

CREATE INDEX idx_tasks_status_snoozed_until ON tasks (status, snoozed_until);
