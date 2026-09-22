-- ============================================================
-- users: interfeys tili (UZ/RU/EN) — default UZ
-- ============================================================
ALTER TABLE users
    ADD COLUMN language VARCHAR(5) NOT NULL DEFAULT 'UZ';