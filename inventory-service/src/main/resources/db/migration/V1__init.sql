CREATE TABLE inventory_items (
    id                  BIGSERIAL PRIMARY KEY,
    product_id          BIGINT NOT NULL,
    quantity_available  INTEGER NOT NULL,
    CONSTRAINT uk_inventory_items_product_id UNIQUE (product_id)
);

CREATE TABLE processed_order_events (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT NOT NULL,
    processed_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_processed_order_events_order_id UNIQUE (order_id)
);
