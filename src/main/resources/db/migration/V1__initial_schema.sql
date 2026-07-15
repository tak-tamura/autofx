CREATE TABLE candle_usd_jpy_1m (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    time DATETIME(3) NOT NULL,
    currency_pair VARCHAR(16) NOT NULL,
    open DECIMAL(6,3) NOT NULL,
    close DECIMAL(6,3) NOT NULL,
    high DECIMAL(6,3) NOT NULL,
    low DECIMAL(6,3) NOT NULL,
    UNIQUE KEY idx_candle_uj_1m_time (time)
);

CREATE TABLE candle_usd_jpy_15m (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    time DATETIME(3) NOT NULL,
    currency_pair VARCHAR(16) NOT NULL,
    open DECIMAL(6,3) NOT NULL,
    close DECIMAL(6,3) NOT NULL,
    high DECIMAL(6,3) NOT NULL,
    low DECIMAL(6,3) NOT NULL,
    UNIQUE KEY idx_candle_uj_15m_time (time)
);

CREATE TABLE candle_usd_jpy_1h (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    time DATETIME(3) NOT NULL,
    currency_pair VARCHAR(16) NOT NULL,
    open DECIMAL(6,3) NOT NULL,
    close DECIMAL(6,3) NOT NULL,
    high DECIMAL(6,3) NOT NULL,
    low DECIMAL(6,3) NOT NULL,
    UNIQUE KEY idx_candle_uj_1h_time (time)
);

CREATE TABLE candle_usd_jpy_4h (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    time DATETIME(3) NOT NULL,
    currency_pair VARCHAR(16) NOT NULL,
    open DECIMAL(6,3) NOT NULL,
    close DECIMAL(6,3) NOT NULL,
    high DECIMAL(6,3) NOT NULL,
    low DECIMAL(6,3) NOT NULL,
    UNIQUE KEY idx_candle_uj_4h_time (time)
);

CREATE TABLE candle_usd_jpy_1d (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    time DATETIME(3) NOT NULL,
    currency_pair VARCHAR(16) NOT NULL,
    open DECIMAL(6,3) NOT NULL,
    close DECIMAL(6,3) NOT NULL,
    high DECIMAL(6,3) NOT NULL,
    low DECIMAL(6,3) NOT NULL,
    UNIQUE KEY idx_candle_uj_1d_time (time)
);

CREATE TABLE candle_usd_jpy_1w (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    time DATETIME(3) NOT NULL,
    currency_pair VARCHAR(16) NOT NULL,
    open DECIMAL(6,3) NOT NULL,
    close DECIMAL(6,3) NOT NULL,
    high DECIMAL(6,3) NOT NULL,
    low DECIMAL(6,3) NOT NULL,
    UNIQUE KEY idx_candle_uj_1w_time (time)
);

CREATE TABLE config_parameter (
    `key` VARCHAR(64) PRIMARY KEY,
    `value` VARCHAR(256) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

CREATE TABLE economic_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    event_time_utc TIMESTAMP,
    window_before_min INT NOT NULL,
    window_after_min INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE `order` (
    order_id BIGINT PRIMARY KEY,
    currency_pair VARCHAR(16) NOT NULL,
    side VARCHAR(8) NOT NULL,
    `size` INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_datetime DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    fill_datetime DATETIME(3),
    fill_price DECIMAL(10,5),
    close_datetime DATETIME(3),
    close_price DECIMAL(10,5)
);

CREATE INDEX idx_order_1 ON `order` (created_datetime);
CREATE INDEX idx_order_2 ON `order` (status);
CREATE INDEX idx_order_3 ON `order` (currency_pair);

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(60) NOT NULL,
    roles VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY UK_users_username (username)
);
