CREATE TABLE orders (
    id                 BIGSERIAL PRIMARY KEY,
    customer_username  VARCHAR(255) NOT NULL,
    customer_email     VARCHAR(255),
    status             VARCHAR(255) NOT NULL,
    total_amount       NUMERIC(19, 2) NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE order_items (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity   INTEGER NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    order_id   BIGINT,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);
