-- =====================================================================
-- БД "JujutsuKaisen" — АИС диспетчеризации автобусного парка (Самара)
-- Запускать в Query Tool, подключившись к пустой базе JujutsuKaisen.
-- Даты рейсов считаются от CURRENT_DATE, поэтому на дашбордах всегда
-- есть «сегодняшние» данные.
-- Пароли пользователей (BCrypt):
--   admin / admin123, dispatcher / disp123, medic / medic123,
--   mechanic / mech123, ivanov, petrov, sidorov, kozlov, morozov / driver123
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------- Справочники ----------
CREATE TABLE app_role (
    id   INTEGER PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE transport_type (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE fuel_type (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE entity_type (
    code VARCHAR(30) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE vehicle_model (
    id                SERIAL PRIMARY KEY,
    name              VARCHAR(100) NOT NULL UNIQUE,
    fuel_type_id      INTEGER NOT NULL REFERENCES fuel_type(id),
    transport_type_id INTEGER REFERENCES transport_type(id),
    default_capacity  INTEGER NOT NULL CHECK (default_capacity > 0)
);

-- ---------- Основные сущности ----------
CREATE TABLE vehicle (
    id         SERIAL PRIMARY KEY,
    reg_number VARCHAR(20) NOT NULL UNIQUE,
    model_id   INTEGER NOT NULL REFERENCES vehicle_model(id),
    capacity   INTEGER NOT NULL CHECK (capacity > 0),
    status     VARCHAR(20) NOT NULL DEFAULT 'active'
               CHECK (status IN ('active', 'maintenance', 'repair'))
);

CREATE TABLE driver (
    id               SERIAL PRIMARY KEY,
    full_name        VARCHAR(150) NOT NULL UNIQUE,
    experience_years INTEGER NOT NULL DEFAULT 0 CHECK (experience_years >= 0),
    license_category VARCHAR(10) NOT NULL,
    phone            VARCHAR(20) UNIQUE,
    passport_series  VARCHAR(4),
    passport_number  VARCHAR(6),
    passport_issued  VARCHAR(255),
    passport_date    DATE,
    license_number   VARCHAR(20) UNIQUE,
    license_date     DATE,
    photo_path       VARCHAR(255),
    UNIQUE (passport_series, passport_number)
);

CREATE TABLE app_user (
    id        SERIAL PRIMARY KEY,
    username  VARCHAR(50) NOT NULL UNIQUE,
    password  VARCHAR(100) NOT NULL,
    role_id   INTEGER NOT NULL REFERENCES app_role(id),
    driver_id INTEGER REFERENCES driver(id) ON DELETE SET NULL,
    enabled   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE stop (
    id        SERIAL PRIMARY KEY,
    name      VARCHAR(150) NOT NULL,
    latitude  DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    stop_type VARCHAR(20) NOT NULL DEFAULT 'intermediate'
              CHECK (stop_type IN ('initial', 'intermediate', 'final'))
);

CREATE TABLE route (
    id                SERIAL PRIMARY KEY,
    number            VARCHAR(10) NOT NULL UNIQUE,
    name              VARCHAR(200) NOT NULL,
    length_km         NUMERIC(6, 2) DEFAULT 0,
    duration_min      INTEGER DEFAULT 0,
    transport_type_id INTEGER NOT NULL REFERENCES transport_type(id)
);

CREATE TABLE route_stop_order (
    route_id            INTEGER NOT NULL REFERENCES route(id) ON DELETE CASCADE,
    order_num           INTEGER NOT NULL,
    stop_id             INTEGER NOT NULL REFERENCES stop(id),
    distance_to_next_km NUMERIC(5, 2) DEFAULT 0,
    time_to_next_min    INTEGER DEFAULT 0,
    PRIMARY KEY (route_id, stop_id)
);

CREATE TABLE schedule_template (
    id             SERIAL PRIMARY KEY,
    route_id       INTEGER NOT NULL REFERENCES route(id),
    day_of_week    INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    departure_time TIME NOT NULL,
    UNIQUE (route_id, day_of_week, departure_time)
);

CREATE TABLE trip_assignment (
    id                   SERIAL PRIMARY KEY,
    route_id             INTEGER NOT NULL REFERENCES route(id),
    vehicle_id           INTEGER REFERENCES vehicle(id),
    driver_id            INTEGER REFERENCES driver(id),
    schedule_template_id INTEGER REFERENCES schedule_template(id),
    assignment_date      DATE NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'scheduled'
                         CHECK (status IN ('scheduled', 'in_progress', 'completed', 'cancelled')),
    actual_departure     TIMESTAMP,
    actual_arrival       TIMESTAMP,
    details              TEXT,
    start_mileage        INTEGER,
    end_mileage          INTEGER
);

CREATE TABLE waybill (
    id             SERIAL PRIMARY KEY,
    trip_id        INTEGER NOT NULL UNIQUE REFERENCES trip_assignment(id) ON DELETE CASCADE,
    driver_id      INTEGER NOT NULL REFERENCES driver(id),
    vehicle_id     INTEGER NOT NULL REFERENCES vehicle(id),
    issue_date     DATE NOT NULL DEFAULT CURRENT_DATE,
    status         VARCHAR(20) NOT NULL DEFAULT 'issued',
    medical_ok     BOOLEAN NOT NULL DEFAULT FALSE,
    inspection_ok  BOOLEAN NOT NULL DEFAULT FALSE,
    ready          BOOLEAN NOT NULL DEFAULT FALSE,
    mileage_before INTEGER,
    mileage_after  INTEGER,
    fuel_before    NUMERIC(6, 2),
    fuel_after     NUMERIC(6, 2)
);

CREATE TABLE medical_check (
    id         SERIAL PRIMARY KEY,
    driver_id  INTEGER NOT NULL REFERENCES driver(id) ON DELETE CASCADE,
    check_type VARCHAR(20) NOT NULL CHECK (check_type IN ('pre_trip', 'post_trip')),
    result     VARCHAR(20) NOT NULL CHECK (result IN ('passed', 'failed')),
    notes      TEXT,
    checked_by VARCHAR(100),
    check_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE vehicle_inspection (
    id              SERIAL PRIMARY KEY,
    vehicle_id      INTEGER NOT NULL REFERENCES vehicle(id) ON DELETE CASCADE,
    driver_id       INTEGER REFERENCES driver(id) ON DELETE SET NULL,
    inspection_type VARCHAR(20) NOT NULL CHECK (inspection_type IN ('pre_trip', 'post_trip')),
    result          VARCHAR(20) NOT NULL CHECK (result IN ('passed', 'failed')),
    notes           TEXT,
    checked_by      VARCHAR(100),
    inspection_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE fuel_log (
    id         SERIAL PRIMARY KEY,
    vehicle_id INTEGER NOT NULL REFERENCES vehicle(id) ON DELETE CASCADE,
    driver_id  INTEGER REFERENCES driver(id) ON DELETE SET NULL,
    liters     NUMERIC(6, 2) NOT NULL CHECK (liters >= 0),
    mileage    INTEGER,
    fuel_date  DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE operation_log (
    id             SERIAL PRIMARY KEY,
    operation_type VARCHAR(30) NOT NULL,
    entity_type    VARCHAR(30) NOT NULL,
    entity_id      INTEGER,
    username       VARCHAR(50),
    details        TEXT,
    created_at     TIMESTAMP DEFAULT NOW()
);

CREATE TABLE dispatcher_notification (
    id                SERIAL PRIMARY KEY,
    notification_type VARCHAR(30) NOT NULL,
    title             VARCHAR(100) NOT NULL,
    message           VARCHAR(500) NOT NULL,
    trip_id           INTEGER,
    vehicle_id        INTEGER,
    driver_id         INTEGER,
    is_read           BOOLEAN DEFAULT FALSE,
    created_at        TIMESTAMP DEFAULT NOW()
);

CREATE TABLE vehicle_maintenance (
    vehicle_id            INTEGER PRIMARY KEY REFERENCES vehicle(id) ON DELETE CASCADE,
    last_service_mileage  INTEGER NOT NULL DEFAULT 0,
    service_interval_km   INTEGER NOT NULL DEFAULT 15000,
    last_notified_mileage INTEGER NOT NULL DEFAULT 0,
    updated_at            TIMESTAMP DEFAULT NOW()
);

-- ---------- Индексы ----------
CREATE INDEX idx_trip_date        ON trip_assignment (assignment_date);
CREATE INDEX idx_trip_driver_date ON trip_assignment (driver_id, assignment_date);
CREATE INDEX idx_trip_vehicle_date ON trip_assignment (vehicle_id, assignment_date);
CREATE INDEX idx_waybill_date     ON waybill (issue_date);
CREATE INDEX idx_medical_date     ON medical_check (check_date);
CREATE INDEX idx_inspection_date  ON vehicle_inspection (inspection_date);
CREATE INDEX idx_fuel_date        ON fuel_log (fuel_date);
CREATE INDEX idx_log_created      ON operation_log (created_at DESC);
CREATE INDEX idx_dispatcher_notification_created ON dispatcher_notification (created_at DESC);
CREATE INDEX idx_dispatcher_notification_unread  ON dispatcher_notification (is_read) WHERE is_read = FALSE;

-- =====================================================================
-- Триггер: пароли, добавленные открытым текстом, хешируются BCrypt
-- =====================================================================
CREATE OR REPLACE FUNCTION app_user_hash_password() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.password IS NOT NULL AND NEW.password !~ '^\$2[aby]\$' THEN
        NEW.password := crypt(NEW.password, gen_salt('bf', 10));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_app_user_hash_password
    BEFORE INSERT OR UPDATE OF password ON app_user
    FOR EACH ROW EXECUTE FUNCTION app_user_hash_password();

-- =====================================================================
-- Функции и процедуры, которые вызывает приложение
-- =====================================================================

-- Авторизация
CREATE OR REPLACE FUNCTION user_login(p_username VARCHAR)
RETURNS TABLE (out_id INTEGER, out_username VARCHAR, out_password_hash VARCHAR,
               out_role_name VARCHAR, out_driver_id INTEGER) AS $$
BEGIN
    RETURN QUERY
    SELECT u.id, u.username, u.password, r.name, u.driver_id
    FROM app_user u JOIN app_role r ON r.id = u.role_id
    WHERE u.username = p_username AND u.enabled;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Пользователь % не найден или заблокирован', p_username;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Рейсы водителя на дату
CREATE OR REPLACE FUNCTION driver_get_trips(p_driver_id INTEGER, p_date DATE)
RETURNS TABLE (id INTEGER, route_id INTEGER, vehicle_id INTEGER, driver_id INTEGER,
               assignment_date DATE, status VARCHAR) AS $$
    SELECT ta.id, ta.route_id, ta.vehicle_id, ta.driver_id, ta.assignment_date, ta.status
    FROM trip_assignment ta
    LEFT JOIN schedule_template st ON st.id = ta.schedule_template_id
    WHERE ta.driver_id = p_driver_id AND ta.assignment_date = p_date
    ORDER BY st.departure_time;
$$ LANGUAGE sql;

-- Расписание маршрута (день недели необязателен)
CREATE OR REPLACE FUNCTION schedule_get_by_route(p_route_id INTEGER, p_day_of_week INTEGER)
RETURNS TABLE (id INTEGER, route_id INTEGER, day_of_week INTEGER, departure_time TIME) AS $$
    SELECT st.id, st.route_id, st.day_of_week, st.departure_time
    FROM schedule_template st
    WHERE st.route_id = p_route_id
      AND (p_day_of_week IS NULL OR st.day_of_week = p_day_of_week)
    ORDER BY st.day_of_week, st.departure_time;
$$ LANGUAGE sql;

-- Сводное расписание на день
CREATE OR REPLACE FUNCTION trip_get_daily_schedule(p_date DATE)
RETURNS TABLE (trip_id INTEGER, route_number VARCHAR, departure_time TIME,
               vehicle_reg VARCHAR, driver_name VARCHAR, status VARCHAR) AS $$
    SELECT ta.id, r.number, st.departure_time, v.reg_number, d.full_name, ta.status
    FROM trip_assignment ta
    JOIN route r ON r.id = ta.route_id
    LEFT JOIN schedule_template st ON st.id = ta.schedule_template_id
    LEFT JOIN vehicle v ON v.id = ta.vehicle_id
    LEFT JOIN driver d ON d.id = ta.driver_id
    WHERE ta.assignment_date = p_date
    ORDER BY st.departure_time, r.number;
$$ LANGUAGE sql;

-- Добавление водителя
CREATE OR REPLACE PROCEDURE driver_add(
    p_full_name VARCHAR, p_experience_years INTEGER, p_license_category VARCHAR,
    p_phone VARCHAR, p_passport_series VARCHAR, p_passport_number VARCHAR,
    p_passport_issued VARCHAR, p_passport_date DATE, p_license_number VARCHAR,
    p_license_date DATE, p_photo_path VARCHAR)
AS $$
BEGIN
    IF p_license_date IS NOT NULL AND p_license_date > CURRENT_DATE THEN
        RAISE EXCEPTION 'Дата выдачи прав не может быть в будущем';
    END IF;
    INSERT INTO driver (full_name, experience_years, license_category, phone,
                        passport_series, passport_number, passport_issued, passport_date,
                        license_number, license_date, photo_path)
    VALUES (p_full_name, COALESCE(p_experience_years, 0), p_license_category, p_phone,
            p_passport_series, p_passport_number, p_passport_issued, p_passport_date,
            p_license_number, p_license_date, p_photo_path);
END;
$$ LANGUAGE plpgsql;

-- Добавление ТС: вместимость берётся из модели
CREATE OR REPLACE PROCEDURE vehicle_add(p_reg_number VARCHAR, p_model_id INTEGER, p_status VARCHAR)
AS $$
DECLARE v_capacity INTEGER;
BEGIN
    SELECT default_capacity INTO v_capacity FROM vehicle_model WHERE id = p_model_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Модель ТС с id % не найдена', p_model_id;
    END IF;
    IF EXISTS (SELECT 1 FROM vehicle WHERE reg_number = p_reg_number) THEN
        RAISE EXCEPTION 'ТС с госномером % уже существует', p_reg_number;
    END IF;
    INSERT INTO vehicle (reg_number, model_id, capacity, status)
    VALUES (p_reg_number, p_model_id, v_capacity, COALESCE(p_status, 'active'));
END;
$$ LANGUAGE plpgsql;

-- Изменение ТС (NULL = не менять поле)
CREATE OR REPLACE PROCEDURE vehicle_update(p_id INTEGER, p_reg_number VARCHAR,
                                           p_model_id INTEGER, p_status VARCHAR)
AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM vehicle WHERE id = p_id) THEN
        RAISE EXCEPTION 'ТС с id % не найдено', p_id;
    END IF;
    UPDATE vehicle SET
        reg_number = COALESCE(p_reg_number, reg_number),
        model_id   = COALESCE(p_model_id, model_id),
        capacity   = CASE WHEN p_model_id IS NOT NULL AND p_model_id <> model_id
                          THEN (SELECT default_capacity FROM vehicle_model WHERE id = p_model_id)
                          ELSE capacity END,
        status     = COALESCE(p_status, status)
    WHERE id = p_id;
END;
$$ LANGUAGE plpgsql;

-- Проверка пересечения рейсов по времени (вспомогательная)
CREATE OR REPLACE FUNCTION trip_overlaps(p_date DATE, p_template_id INTEGER, p_route_id INTEGER,
                                         p_vehicle_id INTEGER, p_driver_id INTEGER,
                                         p_exclude_trip INTEGER DEFAULT NULL)
RETURNS TABLE (vehicle_busy BOOLEAN, driver_busy BOOLEAN) AS $$
    WITH new_trip AS (
        SELECT st.departure_time AS t_start,
               st.departure_time + make_interval(mins => COALESCE(r.duration_min, 0)) AS t_end
        FROM schedule_template st JOIN route r ON r.id = p_route_id
        WHERE st.id = p_template_id
    ), other AS (
        SELECT ta.vehicle_id, ta.driver_id, st.departure_time AS t_start,
               st.departure_time + make_interval(mins => COALESCE(r.duration_min, 0)) AS t_end
        FROM trip_assignment ta
        JOIN schedule_template st ON st.id = ta.schedule_template_id
        JOIN route r ON r.id = ta.route_id
        WHERE ta.assignment_date = p_date
          AND ta.status IN ('scheduled', 'in_progress')
          AND (p_exclude_trip IS NULL OR ta.id <> p_exclude_trip)
    )
    SELECT
        EXISTS (SELECT 1 FROM other o, new_trip n
                WHERE o.vehicle_id = p_vehicle_id AND o.t_start < n.t_end AND n.t_start < o.t_end),
        EXISTS (SELECT 1 FROM other o, new_trip n
                WHERE o.driver_id = p_driver_id AND o.t_start < n.t_end AND n.t_start < o.t_end);
$$ LANGUAGE sql;

-- Безопасное назначение рейса
CREATE OR REPLACE PROCEDURE trip_assign_safe(p_route_id INTEGER, p_vehicle_id INTEGER,
                                             p_driver_id INTEGER, p_schedule_template_id INTEGER,
                                             p_assignment_date DATE, p_status VARCHAR)
AS $$
DECLARE v_busy BOOLEAN; d_busy BOOLEAN; v_status VARCHAR;
BEGIN
    SELECT status INTO v_status FROM vehicle WHERE id = p_vehicle_id;
    IF v_status IS NULL THEN
        RAISE EXCEPTION 'Машина с id % не найдена', p_vehicle_id;
    ELSIF v_status <> 'active' THEN
        RAISE EXCEPTION 'Машина % сейчас недоступна (статус: %)', p_vehicle_id, v_status;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM driver WHERE id = p_driver_id) THEN
        RAISE EXCEPTION 'Водитель с id % не найден', p_driver_id;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM schedule_template
                   WHERE id = p_schedule_template_id AND route_id = p_route_id) THEN
        RAISE EXCEPTION 'Шаблон расписания % не относится к маршруту %', p_schedule_template_id, p_route_id;
    END IF;

    SELECT o.vehicle_busy, o.driver_busy INTO v_busy, d_busy
    FROM trip_overlaps(p_assignment_date, p_schedule_template_id, p_route_id,
                       p_vehicle_id, p_driver_id) o;
    IF v_busy THEN
        RAISE EXCEPTION 'Машина уже занята в это время';
    END IF;
    IF d_busy THEN
        RAISE EXCEPTION 'Водитель уже занят в это время';
    END IF;

    INSERT INTO trip_assignment (route_id, vehicle_id, driver_id, schedule_template_id,
                                 assignment_date, status)
    VALUES (p_route_id, p_vehicle_id, p_driver_id, p_schedule_template_id,
            p_assignment_date, COALESCE(p_status, 'scheduled'));
END;
$$ LANGUAGE plpgsql;

-- Переназначение ТС/водителя на рейс
CREATE OR REPLACE PROCEDURE trip_reassign(p_trip_id INTEGER, p_vehicle_id INTEGER, p_driver_id INTEGER)
AS $$
DECLARE t trip_assignment%ROWTYPE; v_busy BOOLEAN; d_busy BOOLEAN;
BEGIN
    SELECT * INTO t FROM trip_assignment WHERE id = p_trip_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Рейс % не найден', p_trip_id;
    END IF;
    IF t.status IN ('completed', 'cancelled') THEN
        RAISE EXCEPTION 'Рейс % уже завершён или отменён', p_trip_id;
    END IF;
    IF p_vehicle_id IS NOT NULL AND
       (SELECT status FROM vehicle WHERE id = p_vehicle_id) IS DISTINCT FROM 'active' THEN
        RAISE EXCEPTION 'Машина % недоступна', p_vehicle_id;
    END IF;

    SELECT o.vehicle_busy, o.driver_busy INTO v_busy, d_busy
    FROM trip_overlaps(t.assignment_date, t.schedule_template_id, t.route_id,
                       COALESCE(p_vehicle_id, t.vehicle_id), COALESCE(p_driver_id, t.driver_id),
                       p_trip_id) o;
    IF p_vehicle_id IS NOT NULL AND v_busy THEN
        RAISE EXCEPTION 'Машина уже занята в это время';
    END IF;
    IF p_driver_id IS NOT NULL AND d_busy THEN
        RAISE EXCEPTION 'Водитель уже занят в это время';
    END IF;

    UPDATE trip_assignment
    SET vehicle_id = COALESCE(p_vehicle_id, vehicle_id),
        driver_id  = COALESCE(p_driver_id, driver_id)
    WHERE id = p_trip_id;

    UPDATE waybill
    SET vehicle_id = COALESCE(p_vehicle_id, vehicle_id),
        driver_id  = COALESCE(p_driver_id, driver_id)
    WHERE trip_id = p_trip_id;
END;
$$ LANGUAGE plpgsql;

-- =====================================================================
-- Наполнение данными
-- =====================================================================
INSERT INTO app_role (id, name) VALUES
(1, 'DRIVER'), (2, 'DISPATCHER'), (3, 'ADMIN'), (4, 'MEDIC'), (5, 'MECHANIC');

INSERT INTO transport_type (name) VALUES ('Автобус'), ('Троллейбус'), ('Трамвай');
INSERT INTO fuel_type (name) VALUES ('Дизель'), ('Метан (КПГ)'), ('Электричество');

INSERT INTO entity_type (code, name) VALUES
('VEHICLE', 'Транспортное средство'), ('DRIVER', 'Водитель'), ('ROUTE', 'Маршрут'),
('STOP', 'Остановка'), ('TRIP', 'Рейс'), ('USER', 'Пользователь'),
('SCHEDULE_TEMPLATE', 'Шаблон расписания'), ('MEDICAL_CHECK', 'Медосмотр'),
('VEHICLE_INSPECTION', 'Техосмотр'), ('FUEL_LOG', 'Заправка'), ('WAYBILL', 'Путевой лист');

INSERT INTO vehicle_model (name, fuel_type_id, transport_type_id, default_capacity) VALUES
('НефАЗ-5299-40-57', 2, 1, 102),
('ЛиАЗ-5292.67',     2, 1, 108),
('ПАЗ-320435-04',    1, 1, 52),
('ГАЗ ГАЗель City',  1, 1, 40),
('КАМАЗ-6282 (электробус)', 3, 1, 85);

INSERT INTO vehicle (reg_number, model_id, capacity, status) VALUES
('ВА 101 63', 1, 102, 'active'),
('ВА 102 63', 1, 102, 'active'),
('ВА 215 63', 2, 108, 'active'),
('ВА 216 63', 2, 108, 'active'),
('ВК 340 63', 3, 52,  'active'),
('ВК 341 63', 3, 52,  'active'),
('ВМ 507 63', 4, 40,  'active'),
('ВМ 508 63', 4, 40,  'maintenance'),
('ВЕ 777 63', 5, 85,  'active'),
('ВЕ 778 63', 5, 85,  'repair');

INSERT INTO driver (full_name, experience_years, license_category, phone, passport_series,
                    passport_number, passport_issued, passport_date, license_number, license_date) VALUES
('Иванов Сергей Петрович',     14, 'D',  '+7 917 101-11-11', '3611', '100001', 'ГУ МВД России по Самарской обл.', '2015-04-12', '6301 100001', '2012-06-01'),
('Петров Алексей Викторович',   9, 'D',  '+7 917 202-22-22', '3612', '100002', 'ГУ МВД России по Самарской обл.', '2016-09-03', '6301 100002', '2017-03-15'),
('Сидоров Николай Андреевич',  21, 'DE', '+7 927 303-33-33', '3608', '100003', 'ОУФМС России по Самарской обл.',  '2009-11-20', '6301 100003', '2005-08-10'),
('Козлов Дмитрий Игоревич',     6, 'D',  '+7 927 404-44-44', '3619', '100004', 'ГУ МВД России по Самарской обл.', '2019-02-14', '6301 100004', '2020-05-22'),
('Морозов Андрей Олегович',    11, 'D',  '+7 937 505-55-55', '3614', '100005', 'ГУ МВД России по Самарской обл.', '2014-07-30', '6301 100005', '2015-01-19'),
('Волков Павел Сергеевич',     17, 'D',  '+7 937 606-66-66', '3610', '100006', 'ОУФМС России по Самарской обл.',  '2010-03-25', '6301 100006', '2009-10-02'),
('Лебедев Игорь Николаевич',    4, 'D',  '+7 917 707-77-77', '3621', '100007', 'ГУ МВД России по Самарской обл.', '2021-06-17', '6301 100007', '2022-02-11'),
('Новиков Артём Евгеньевич',    8, 'C',  '+7 927 808-88-88', '3617', '100008', 'ГУ МВД России по Самарской обл.', '2017-12-05', '6301 100008', '2018-04-28');

-- Пароли хешируются триггером
INSERT INTO app_user (username, password, role_id, driver_id) VALUES
('admin',      'admin123',  3, NULL),
('dispatcher', 'disp123',   2, NULL),
('medic',      'medic123',  4, NULL),
('mechanic',   'mech123',   5, NULL),
('ivanov',     'driver123', 1, 1),
('petrov',     'driver123', 1, 2),
('sidorov',    'driver123', 1, 3),
('kozlov',     'driver123', 1, 4),
('morozov',    'driver123', 1, 5);

INSERT INTO stop (name, latitude, longitude, stop_type) VALUES
('Железнодорожный вокзал',  53.18680, 50.12230, 'initial'),    -- 1
('Площадь Революции',       53.19520, 50.10090, 'intermediate'),-- 2
('Площадь Куйбышева',       53.19710, 50.11380, 'intermediate'),-- 3
('Ул. Ново-Садовая',        53.21170, 50.14600, 'intermediate'),-- 4
('Губернский рынок',        53.21430, 50.17010, 'intermediate'),-- 5
('Московское шоссе',        53.22100, 50.18950, 'intermediate'),-- 6
('Метро «Российская»',      53.21310, 50.16230, 'intermediate'),-- 7
('Метро «Безымянка»',       53.21360, 50.24010, 'intermediate'),-- 8
('ТЦ «Мега»',               53.26530, 50.25360, 'final'),       -- 9
('СамГТУ (ул. Молодогвардейская)', 53.18630, 50.10560, 'intermediate'), -- 10
('Барбошина поляна',        53.26600, 50.18150, 'final'),       -- 11
('Аэропорт Курумоч',        53.50490, 50.16430, 'final'),       -- 12
('Автостанция «Аврора»',    53.19410, 50.16640, 'initial'),     -- 13
('Ул. Стара-Загора',        53.22510, 50.21390, 'intermediate'),-- 14
('Кировский рынок',         53.22600, 50.24260, 'final');       -- 15

INSERT INTO route (number, name, length_km, duration_min, transport_type_id) VALUES
('1',   'Ж/д вокзал — Барбошина поляна',          0, 0, 1),
('41',  'Ж/д вокзал — ТЦ «Мега»',                 0, 0, 1),
('50',  'Площадь Революции — Кировский рынок',    0, 0, 1),
('67',  'Автостанция «Аврора» — ТЦ «Мега»',       0, 0, 1),
('78',  'Ж/д вокзал — Аэропорт Курумоч',          0, 0, 1);

INSERT INTO route_stop_order (route_id, order_num, stop_id, distance_to_next_km, time_to_next_min) VALUES
(1, 1, 1, 1.6, 6), (1, 2, 10, 1.9, 7), (1, 3, 4, 2.4, 8), (1, 4, 7, 3.1, 9), (1, 5, 6, 5.2, 12), (1, 6, 11, 0, 0),
(2, 1, 1, 1.8, 6), (2, 2, 3, 3.6, 11), (2, 3, 5, 2.1, 7), (2, 4, 6, 4.6, 12), (2, 5, 8, 6.3, 15), (2, 6, 9, 0, 0),
(3, 1, 2, 1.0, 4), (3, 2, 3, 3.9, 12), (3, 3, 7, 4.2, 11), (3, 4, 14, 2.3, 7), (3, 5, 15, 0, 0),
(4, 1, 13, 1.4, 5), (4, 2, 5, 2.1, 7), (4, 3, 14, 3.0, 9), (4, 4, 8, 6.1, 14), (4, 5, 9, 0, 0),
(5, 1, 1, 2.7, 9), (5, 2, 4, 3.2, 9), (5, 3, 6, 6.8, 14), (5, 4, 9, 27.5, 32), (5, 5, 12, 0, 0);

UPDATE route r SET
    length_km    = (SELECT COALESCE(SUM(distance_to_next_km), 0) FROM route_stop_order WHERE route_id = r.id),
    duration_min = (SELECT COALESCE(SUM(time_to_next_min), 0) FROM route_stop_order WHERE route_id = r.id);

-- Расписание: будни — больше рейсов, выходные — меньше
INSERT INTO schedule_template (route_id, day_of_week, departure_time)
SELECT r.id, d, t::TIME
FROM route r
CROSS JOIN generate_series(1, 7) d
CROSS JOIN LATERAL unnest(
    CASE
        WHEN r.number = '78' THEN ARRAY['06:00', '12:00', '18:00']
        WHEN d <= 5         THEN ARRAY['06:30', '09:00', '13:00', '17:30']
        ELSE                     ARRAY['08:00', '14:00']
    END) t;

-- Рейсы: 3 прошедших дня, сегодня и завтра. Водители и ТС распределяются по кругу.
WITH days AS (
    SELECT (CURRENT_DATE + g)::DATE AS dt FROM generate_series(-3, 1) g
), slots AS (
    SELECT d.dt, st.id AS template_id, st.route_id, st.departure_time,
           ROW_NUMBER() OVER (PARTITION BY d.dt ORDER BY st.departure_time, st.route_id) AS rn
    FROM days d
    JOIN schedule_template st ON st.day_of_week = EXTRACT(ISODOW FROM d.dt)
), drv AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS k, COUNT(*) OVER () AS n FROM driver
    WHERE license_category IN ('D', 'DE')
), veh AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS k, COUNT(*) OVER () AS n FROM vehicle WHERE status = 'active'
)
INSERT INTO trip_assignment (route_id, vehicle_id, driver_id, schedule_template_id, assignment_date, status)
SELECT s.route_id, v.id, dr.id, s.template_id, s.dt,
       CASE WHEN s.dt < CURRENT_DATE THEN 'completed' ELSE 'scheduled' END
FROM slots s
JOIN drv dr ON dr.k = ((s.rn - 1) % dr.n) + 1
JOIN veh v  ON v.k  = ((s.rn - 1) % v.n) + 1;

-- Прошедшие рейсы: фактическое время, пробег; один рейс отменён
UPDATE trip_assignment ta SET
    actual_departure = ta.assignment_date + st.departure_time + make_interval(mins => (ta.id % 4)),
    actual_arrival   = ta.assignment_date + st.departure_time
                       + make_interval(mins => r.duration_min + (ta.id % 9) - 3),
    start_mileage    = 40000 + ta.vehicle_id * 3700 + ta.id * 35,
    end_mileage      = 40000 + ta.vehicle_id * 3700 + ta.id * 35 + CEIL(r.length_km)::INT,
    details          = 'Рейс выполнен'
FROM schedule_template st, route r
WHERE st.id = ta.schedule_template_id AND r.id = ta.route_id AND ta.status = 'completed';

UPDATE trip_assignment SET status = 'cancelled', actual_departure = NULL, actual_arrival = NULL,
       start_mileage = NULL, end_mileage = NULL, details = 'Отменён: неисправность ТС'
WHERE id = (SELECT MIN(id) FROM trip_assignment WHERE assignment_date = CURRENT_DATE - 2);

-- Сегодня: утренние рейсы выполнены/в пути
UPDATE trip_assignment ta SET
    status = CASE WHEN st.departure_time < '07:00' THEN 'completed' ELSE 'in_progress' END,
    actual_departure = CURRENT_DATE + st.departure_time,
    actual_arrival = CASE WHEN st.departure_time < '07:00'
                          THEN CURRENT_DATE + st.departure_time + make_interval(mins => r.duration_min) END,
    start_mileage = 40000 + ta.vehicle_id * 3700 + ta.id * 35,
    end_mileage = CASE WHEN st.departure_time < '07:00'
                       THEN 40000 + ta.vehicle_id * 3700 + ta.id * 35 + CEIL(r.length_km)::INT END
FROM schedule_template st, route r
WHERE st.id = ta.schedule_template_id AND r.id = ta.route_id
  AND ta.assignment_date = CURRENT_DATE AND st.departure_time <= '08:00';

-- Путевые листы на выполненные и сегодняшние рейсы
INSERT INTO waybill (trip_id, driver_id, vehicle_id, issue_date, status, medical_ok, inspection_ok, ready,
                     mileage_before, mileage_after, fuel_before, fuel_after)
SELECT ta.id, ta.driver_id, ta.vehicle_id, ta.assignment_date,
       CASE WHEN ta.status = 'completed' THEN 'closed' ELSE 'issued' END,
       TRUE, TRUE, TRUE,
       ta.start_mileage, ta.end_mileage,
       CASE WHEN ta.start_mileage IS NOT NULL THEN 180 - (ta.id % 5) * 10 END,
       CASE WHEN ta.end_mileage IS NOT NULL THEN 160 - (ta.id % 5) * 10 END
FROM trip_assignment ta
WHERE ta.status IN ('completed', 'in_progress');

-- Медосмотры и техосмотры по путевым листам
INSERT INTO medical_check (driver_id, check_type, result, notes, checked_by, check_date, created_at)
SELECT DISTINCT ON (w.driver_id, w.issue_date)
       w.driver_id, 'pre_trip', 'passed', 'Давление и пульс в норме', 'medic', w.issue_date,
       w.issue_date + TIME '05:40'
FROM waybill w ORDER BY w.driver_id, w.issue_date;

INSERT INTO medical_check (driver_id, check_type, result, notes, checked_by, check_date, created_at) VALUES
(8, 'pre_trip', 'failed', 'Повышенное давление, к рейсу не допущен', 'medic', CURRENT_DATE - 1, CURRENT_DATE - 1 + TIME '05:50');

INSERT INTO vehicle_inspection (vehicle_id, driver_id, inspection_type, result, notes, checked_by, inspection_date)
SELECT DISTINCT ON (w.vehicle_id, w.issue_date)
       w.vehicle_id, w.driver_id, 'pre_trip', 'passed', 'Замечаний нет', 'mechanic', w.issue_date
FROM waybill w ORDER BY w.vehicle_id, w.issue_date;

INSERT INTO vehicle_inspection (vehicle_id, driver_id, inspection_type, result, notes, checked_by, inspection_date) VALUES
(8, NULL, 'pre_trip', 'failed', 'Износ тормозных колодок, отправлен на ТО', 'mechanic', CURRENT_DATE - 2),
(10, NULL, 'pre_trip', 'failed', 'Неисправность тягового аккумулятора', 'mechanic', CURRENT_DATE - 3);

INSERT INTO fuel_log (vehicle_id, driver_id, liters, mileage, fuel_date)
SELECT w.vehicle_id, w.driver_id, 60 + (w.id % 4) * 10, w.mileage_after, w.issue_date
FROM waybill w WHERE w.status = 'closed' AND w.id % 3 = 0;

-- Техобслуживание: у одного автобуса подошёл срок ТО
INSERT INTO vehicle_maintenance (vehicle_id, last_service_mileage, service_interval_km)
SELECT id, 30000, 15000 FROM vehicle;
UPDATE vehicle_maintenance SET last_service_mileage = 20000 WHERE vehicle_id = 1;

INSERT INTO dispatcher_notification (notification_type, title, message, vehicle_id, created_at) VALUES
('MAINTENANCE_DUE', 'Требуется ТО', 'ВА 101 63: пробег превысил интервал обслуживания', 1, NOW() - INTERVAL '3 hours'),
('VEHICLE_FAULT', 'ТС снято с линии', 'ВЕ 778 63: неисправность тягового аккумулятора', 10, NOW() - INTERVAL '2 days');
INSERT INTO dispatcher_notification (notification_type, title, message, driver_id, is_read, created_at) VALUES
('MEDICAL_FAILED', 'Водитель не допущен', 'Новиков А. Е. не прошёл предрейсовый медосмотр', 8, TRUE, NOW() - INTERVAL '1 day');

INSERT INTO operation_log (operation_type, entity_type, entity_id, username, details, created_at) VALUES
('LOGIN',  'USER',    NULL, 'admin',      'role=ADMIN',                       NOW() - INTERVAL '2 days'),
('CREATE', 'ROUTE',   5,    'admin',      'Добавлен маршрут 78',              NOW() - INTERVAL '2 days'),
('UPDATE', 'VEHICLE', 8,    'mechanic',   'Статус изменён на maintenance',    NOW() - INTERVAL '2 days'),
('UPDATE', 'VEHICLE', 10,   'mechanic',   'Статус изменён на repair',         NOW() - INTERVAL '3 days'),
('CREATE', 'DRIVER',  8,    'admin',      'Добавлен водитель Новиков А. Е.',  NOW() - INTERVAL '1 day'),
('LOGIN',  'USER',    NULL, 'dispatcher', 'role=DISPATCHER',                  NOW() - INTERVAL '4 hours'),
('CREATE', 'WAYBILL', 1,    'dispatcher', 'Выданы путевые листы на сегодня',  NOW() - INTERVAL '3 hours');
