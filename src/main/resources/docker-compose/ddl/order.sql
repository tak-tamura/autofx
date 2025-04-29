CREATE TABLE IF NOT EXISTS `order` (
    order_id BIGINT PRIMARY KEY,
    currency_pair VARCHAR(16) NOT NULL,
    side VARCHAR(8) NOT NULL, -- 'BUY' or 'SELL'
    `size` INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL, -- 'FILLED', 'CANCELLED', 'CLOSED'
    created_datetime DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    fill_datetime DATETIME(3),
    fill_price DECIMAL(10,5),
    close_datetime DATETIME(3),
    close_price DECIMAL(10,5)
);

CREATE INDEX idx_order_1 ON `order` (created_datetime);
CREATE INDEX idx_order_2 ON `order` (status);
CREATE INDEX idx_order_3 ON `order` (currency_pair);