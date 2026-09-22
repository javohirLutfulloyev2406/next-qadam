CREATE OR REPLACE VIEW v_user_activity_logs AS
SELECT
    ual.id,
    u.id AS user_id,
    u.name AS user_name,
    u.telegram_id,
    u.language,
    ual.action_type,
    ual.action_detail,
    ual.created_at,
    ual.updated_at
FROM user_activity_logs ual
JOIN users u ON u.id = ual.user_id
WHERE ual.deleted = false AND u.deleted = false
ORDER BY ual.created_at DESC;
