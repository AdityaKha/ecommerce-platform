CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    sku         VARCHAR(255) NOT NULL,
    price       NUMERIC(19, 2) NOT NULL,
    category    VARCHAR(255),
    active      BOOLEAN NOT NULL,
    CONSTRAINT uk_products_sku UNIQUE (sku)
);
