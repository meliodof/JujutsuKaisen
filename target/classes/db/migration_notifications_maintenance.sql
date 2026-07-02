-- Выполнить один раз в PostgreSQL (обязательно для уведомлений и ТО)

CREATE TABLE IF NOT EXISTS dispatcher_notification (
    id SERIAL PRIMARY KEY,
    notification_type VARCHAR(30) NOT NULL,
    title VARCHAR(100) NOT NULL,
    message VARCHAR(500) NOT NULL,
    trip_id INTEGER,
    vehicle_id INTEGER,
    driver_id INTEGER,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dispatcher_notification_created
    ON dispatcher_notification (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_dispatcher_notification_unread
    ON dispatcher_notification (is_read) WHERE is_read = FALSE;

CREATE TABLE IF NOT EXISTS vehicle_maintenance (
    vehicle_id INTEGER PRIMARY KEY REFERENCES vehicle(id) ON DELETE CASCADE,
    last_service_mileage INTEGER NOT NULL DEFAULT 0,
    service_interval_km INTEGER NOT NULL DEFAULT 15000,
    last_notified_mileage INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT NOW()
);
