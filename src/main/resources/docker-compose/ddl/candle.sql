CREATE TABLE IF NOT EXISTS candle_usd_jpy_1m (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    time DATETIME(3)  NOT NULL,
    currency_pair VARCHAR(16) NOT NULL,
    open DECIMAL(6,3) NOT NULL,
    close DECIMAL(6,3) NOT NULL,
    high DECIMAL(6,3) NOT NULL,
    low DECIMAL(6,3) NOT NULL,
    UNIQUE KEY idx_candle_uj_1m_time (time)
);

CREATE TABLE IF NOT EXISTS candle_usd_jpy_15m (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    time DATETIME(3)  NOT NULL,
    currency_pair VARCHAR(16) NOT NULL,
    open DECIMAL(6,3) NOT NULL,
    close DECIMAL(6,3) NOT NULL,
    high DECIMAL(6,3) NOT NULL,
    low DECIMAL(6,3) NOT NULL,
    UNIQUE KEY idx_candle_uj_15m_time (time)
);

CREATE TABLE IF NOT EXISTS candle_usd_jpy_1h (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    time DATETIME(3)  NOT NULL,
    currency_pair VARCHAR(16) NOT NULL,
    open DECIMAL(6,3) NOT NULL,
    close DECIMAL(6,3) NOT NULL,
    high DECIMAL(6,3) NOT NULL,
    low DECIMAL(6,3) NOT NULL,
    UNIQUE KEY idx_candle_uj_1h_time (time)
);

CREATE TABLE IF NOT EXISTS candle_usd_jpy_4h (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    time DATETIME(3)  NOT NULL,
    currency_pair VARCHAR(16) NOT NULL,
    open DECIMAL(6,3) NOT NULL,
    close DECIMAL(6,3) NOT NULL,
    high DECIMAL(6,3) NOT NULL,
    low DECIMAL(6,3) NOT NULL,
    UNIQUE KEY idx_candle_uj_4h_time (time)
);

CREATE TABLE IF NOT EXISTS candle_usd_jpy_1d (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    time DATETIME(3)  NOT NULL,
    currency_pair VARCHAR(16) NOT NULL,
    open DECIMAL(6,3) NOT NULL,
    close DECIMAL(6,3) NOT NULL,
    high DECIMAL(6,3) NOT NULL,
    low DECIMAL(6,3) NOT NULL,
    UNIQUE KEY idx_candle_uj_1d_time (time)
);

CREATE TABLE IF NOT EXISTS candle_usd_jpy_1w (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    time DATETIME(3)  NOT NULL,
    currency_pair VARCHAR(16) NOT NULL,
    open DECIMAL(6,3) NOT NULL,
    close DECIMAL(6,3) NOT NULL,
    high DECIMAL(6,3) NOT NULL,
    low DECIMAL(6,3) NOT NULL,
    UNIQUE KEY idx_candle_uj_1w_time (time)
);