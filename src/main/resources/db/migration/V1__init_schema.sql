CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- users
-- ============================================================
CREATE TABLE users
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,

    telegram_id     BIGINT       NOT NULL,
    name            VARCHAR(255),
    tone_preference VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    timezone        VARCHAR(64),

    CONSTRAINT uq_users_telegram_id UNIQUE (telegram_id)
);

-- ============================================================
-- goals
-- ============================================================
CREATE TABLE goals
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,

    user_id     UUID        REFERENCES users (id),
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(20),
    target_date TIMESTAMPTZ
);

CREATE INDEX idx_goals_user_id ON goals (user_id);

-- ============================================================
-- milestones
-- ============================================================
CREATE TABLE milestones
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    deleted    BOOLEAN     NOT NULL DEFAULT FALSE,

    goal_id    UUID        REFERENCES goals (id),
    title      VARCHAR(255),
    period     VARCHAR(20),
    status     VARCHAR(20)
);

CREATE INDEX idx_milestones_goal_id ON milestones (goal_id);

-- ============================================================
-- tasks
-- ============================================================
CREATE TABLE tasks
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at        TIMESTAMP   NOT NULL,
    updated_at        TIMESTAMP   NOT NULL,
    version           BIGINT      NOT NULL DEFAULT 0,
    deleted           BOOLEAN     NOT NULL DEFAULT FALSE,

    goal_id           UUID        NOT NULL REFERENCES goals (id),
    milestone_id      UUID        REFERENCES milestones (id),
    title             VARCHAR(255),
    due_date          TIMESTAMPTZ,
    status            VARCHAR(20),
    estimated_minutes INTEGER
);

CREATE INDEX idx_task_status_due_date ON tasks (status, due_date);
CREATE INDEX idx_tasks_goal_id ON tasks (goal_id);
CREATE INDEX idx_tasks_milestone_id ON tasks (milestone_id);

-- ============================================================
-- check_ins
-- ============================================================
CREATE TABLE check_ins
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    deleted         BOOLEAN     NOT NULL DEFAULT FALSE,

    user_id         UUID        REFERENCES users (id),
    date            DATE,
    type            VARCHAR(20),
    raw_text        TEXT,
    parsed_summary  TEXT,

    CONSTRAINT uq_check_in_user_date_type UNIQUE (user_id, date, type)
);

CREATE INDEX idx_check_in_user_date ON check_ins (user_id, date);

-- ============================================================
-- journal_entries
-- ============================================================
CREATE TABLE journal_entries
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,

    user_id     UUID        REFERENCES users (id),
    content     TEXT,
    source_type VARCHAR(20)
);

CREATE INDEX idx_journal_entries_user_id ON journal_entries (user_id);

-- ============================================================
-- reminders
-- ============================================================
CREATE TABLE reminders
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL,
    version      BIGINT      NOT NULL DEFAULT 0,
    deleted      BOOLEAN     NOT NULL DEFAULT FALSE,

    user_id      UUID        REFERENCES users (id),
    task_id      UUID        REFERENCES tasks (id),
    scheduled_at TIMESTAMPTZ,
    tone         VARCHAR(20),
    status       VARCHAR(20)
);

CREATE INDEX idx_reminder_status_scheduled_at ON reminders (status, scheduled_at);
CREATE INDEX idx_reminders_user_id ON reminders (user_id);

-- ============================================================
-- memory_items
-- ============================================================
CREATE TABLE memory_items
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    deleted    BOOLEAN     NOT NULL DEFAULT FALSE,

    user_id    UUID        REFERENCES users (id),
    key        VARCHAR(255),
    value      JSONB,
    importance INTEGER     NOT NULL DEFAULT 0,

    CONSTRAINT uq_memory_item_user_key UNIQUE (user_id, key)
);

-- ============================================================
-- error_logs
-- ============================================================
CREATE TABLE error_logs
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP   NOT NULL,
    version        BIGINT      NOT NULL DEFAULT 0,
    deleted        BOOLEAN     NOT NULL DEFAULT FALSE,

    source_module  VARCHAR(255),
    exception_type VARCHAR(255),
    message        TEXT,
    stack_trace    TEXT,
    log_id         VARCHAR(64),
    notified       BOOLEAN     NOT NULL DEFAULT FALSE
);