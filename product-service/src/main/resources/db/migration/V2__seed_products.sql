-- Dev/demo seed data. Explicit ids so inventory-service's seed migration can
-- reference the same product ids; the sequence is advanced afterwards so
-- runtime inserts don't collide.
INSERT INTO products (id, name, description, sku, price, category, active) VALUES
    (1,  'Aurora Wireless Headphones', 'Over-ear noise-cancelling headphones with 30-hour battery life and plush memory-foam ear cups.', 'SKU-AURORA-HP', 129.99, 'Audio', TRUE),
    (2,  'Echo Bluetooth Speaker', 'Portable waterproof speaker with punchy 360-degree sound and 12 hours of playtime.', 'SKU-ECHO-SPK', 64.99, 'Audio', TRUE),
    (3,  'Volt Mechanical Keyboard', 'Hot-swappable RGB mechanical keyboard with tactile switches and aluminium frame.', 'SKU-VOLT-KB', 89.99, 'Electronics', TRUE),
    (4,  'Drift Wireless Mouse', 'Silent-click ergonomic mouse with adjustable DPI and a 6-month rechargeable battery.', 'SKU-DRIFT-MS', 39.99, 'Electronics', TRUE),
    (5,  'Nimbus 27" 4K Monitor', 'Ultra-thin IPS display with HDR10, 99% sRGB coverage and a height-adjustable stand.', 'SKU-NIMBUS-MON', 349.99, 'Electronics', TRUE),
    (6,  'Flux USB-C Charging Hub', '8-in-1 hub with 100W pass-through charging, dual HDMI and gigabit ethernet.', 'SKU-FLUX-HUB', 54.99, 'Electronics', TRUE),
    (7,  'Trailblazer Backpack 25L', 'Weather-resistant commuter backpack with padded laptop sleeve and hidden pockets.', 'SKU-TRAIL-BP', 59.99, 'Accessories', TRUE),
    (8,  'Meridian Leather Wallet', 'Slim RFID-blocking wallet in full-grain leather that holds 8 cards plus cash.', 'SKU-MERIDIAN-WL', 34.99, 'Accessories', TRUE),
    (9,  'Everbrew Pour-Over Coffee Kit', 'Borosilicate glass carafe, stainless dripper and 100 filters for cafe-grade coffee at home.', 'SKU-EVERBREW-KIT', 44.50, 'Home & Kitchen', TRUE),
    (10, 'Lumen Smart Desk Lamp', 'App-controlled lamp with adaptive colour temperature and a built-in wireless charger.', 'SKU-LUMEN-LAMP', 42.99, 'Home & Kitchen', TRUE),
    (11, 'Apex Yoga Mat', 'Non-slip 6mm mat in natural rubber with alignment guides and carry strap.', 'SKU-APEX-MAT', 29.99, 'Fitness', TRUE),
    (12, 'Pulse Fitness Tracker', 'Heart-rate, sleep and workout tracking with a 10-day battery and AMOLED display.', 'SKU-PULSE-FIT', 79.99, 'Fitness', TRUE);

SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
