-- Dev/demo seed data: stock levels for the products seeded by
-- product-service's V2__seed_products.sql (same product ids).
INSERT INTO inventory_items (product_id, quantity_available) VALUES
    (1,  40),
    (2,  55),
    (3,  25),
    (4,  80),
    (5,  15),
    (6,  60),
    (7,  35),
    (8,  70),
    (9,  45),
    (10, 50),
    (11, 90),
    (12, 30);
