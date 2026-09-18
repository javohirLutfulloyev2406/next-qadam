-- ============================================================
-- tasks: bugungi ustuvorlik belgisi
-- ============================================================
ALTER TABLE tasks
    ADD COLUMN is_today_priority BOOLEAN NOT NULL DEFAULT FALSE;

-- tasks jadvalida to'g'ridan-to'g'ri user_id yo'q (goal_id orqali users'ga bog'lanadi, goals.user_id
-- allaqachon idx_goals_user_id bilan indekslangan) — shu sababli goal_id orqali join qilinganda
-- is_today_priority/due_date bo'yicha saralashni tezlashtiruvchi composite index qo'shamiz.
CREATE INDEX idx_tasks_priority_lookup ON tasks (goal_id, status, is_today_priority, due_date);

-- ============================================================
-- reminders: Brain Dump orqali kelgan eslatma matni
-- ============================================================
ALTER TABLE reminders
    ADD COLUMN content TEXT;

-- ============================================================
-- ideas
-- ============================================================
CREATE TABLE ideas
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    deleted    BOOLEAN     NOT NULL DEFAULT FALSE,

    user_id    UUID        REFERENCES users (id),
    content    TEXT        NOT NULL
);

CREATE INDEX idx_ideas_user_id ON ideas (user_id);
