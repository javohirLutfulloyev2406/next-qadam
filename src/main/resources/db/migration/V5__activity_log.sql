-- ============================================================
-- user_activity_logs: admin panelida har bir foydalanuvchining so'nggi
-- faolligini (buyruq/callback/erkin matn) ko'rish uchun
-- ============================================================
CREATE TABLE user_activity_logs
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,

    user_id       UUID         NOT NULL REFERENCES users (id),
    action_type   VARCHAR(20)  NOT NULL,
    action_detail VARCHAR(255)
);

-- Admin panelida bitta foydalanuvchining so'nggi N harakatini tez olish uchun.
CREATE INDEX idx_user_activity_logs_user_id_created_at ON user_activity_logs (user_id, created_at DESC);

-- ActivityLogCleanupScheduler'ning "N kundan eski yozuvlarni o'chirish" so'rovi uchun.
CREATE INDEX idx_user_activity_logs_created_at ON user_activity_logs (created_at);
