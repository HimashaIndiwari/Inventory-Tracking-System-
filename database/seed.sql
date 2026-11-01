-- =============================================================================
-- Inventory Tracking System — demo seed data
--
-- Seeds categories, suppliers and products ONLY. All data is fictional
-- (example.com addresses, 555 phone numbers).
--   * No users and no admin accounts: users are created when someone signs in
--     through Firebase.
--   * No stock transactions: every product starts at quantity 0, and stock is
--     added later through Stock IN by a real authenticated user.
--
-- Safe to run more than once. Each INSERT uses
--   ON DUPLICATE KEY UPDATE <key> = <key>
-- which is a no-op when the row already exists (matched by its unique name or
-- SKU), so re-running neither duplicates rows nor overwrites edits made since.
-- (INSERT IGNORE is avoided on purpose: it would also hide real errors such as
-- CHECK constraint violations.)
--
-- Run after schema.sql, for example:
--   mysql -u <user> -p --default-character-set=utf8mb4 inventory_db -e "source database/seed.sql"
-- Timestamps use UTC_TIMESTAMP(6) so they are UTC regardless of session time zone.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- Categories
-- -----------------------------------------------------------------------------
INSERT INTO categories (name, description, created_at, updated_at) VALUES
    ('Electronics',       'Computers, peripherals and accessories',      UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Office Supplies',   'Paper, writing instruments and desk items',   UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Furniture',         'Desks, chairs and storage',                   UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Networking',        'Cables, switches and wireless equipment',     UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Cleaning Supplies', 'Cleaning products for offices and warehouses', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE name = name;


-- -----------------------------------------------------------------------------
-- Suppliers
-- -----------------------------------------------------------------------------
INSERT INTO suppliers (name, contact_person, email, phone, address, created_at, updated_at) VALUES
    ('Northwind Electronics Ltd', 'Alex Morgan', 'sales@northwind-electronics.example.com', '+1-555-0101', '12 Harbor Road, Springfield',  UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Bluepeak Office Co',        'Jordan Lee',  'orders@bluepeak-office.example.com',      '+1-555-0102', '48 Maple Avenue, Riverton',    UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Oakline Furniture',         'Sam Patel',   'contact@oakline-furniture.example.com',   '+1-555-0103', '7 Industrial Park, Lakeside',  UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Clearwave Networks',        'Taylor Kim',  'support@clearwave-networks.example.com',  '+1-555-0104', '230 Signal Street, Fairview',  UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE name = name;


-- -----------------------------------------------------------------------------
-- Products (quantity is always 0; category/supplier looked up by unique name)
--   Supplier is optional: CLN-001 and CLN-002 have no supplier (LEFT JOIN -> NULL).
-- -----------------------------------------------------------------------------
INSERT INTO products
    (sku, name, description, category_id, supplier_id, price, quantity, reorder_level, active, created_at, updated_at)
SELECT p.sku, p.name, p.description, c.id, s.id, p.price, 0, p.reorder_level, TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM (
              SELECT 'ELEC-001' AS sku, 'Wireless Mouse'              AS name, '2.4 GHz wireless optical mouse'       AS description, 'Electronics'       AS category, 'Northwind Electronics Ltd' AS supplier,   24.99 AS price, 10 AS reorder_level
    UNION ALL SELECT 'ELEC-002', 'Mechanical Keyboard',         'Full-size keyboard with tactile switches', 'Electronics',       'Northwind Electronics Ltd',  79.50, 5
    UNION ALL SELECT 'ELEC-003', '27-inch Monitor',             '27-inch IPS display, 1440p',               'Electronics',       'Northwind Electronics Ltd', 289.00, 3
    UNION ALL SELECT 'OFF-001',  'A4 Copy Paper (500 sheets)',  '80 gsm white copy paper',                  'Office Supplies',   'Bluepeak Office Co',          6.75, 50
    UNION ALL SELECT 'OFF-002',  'Ballpoint Pens (Box of 12)',  'Blue ink, medium point',                   'Office Supplies',   'Bluepeak Office Co',          4.20, 20
    UNION ALL SELECT 'OFF-003',  'Stapler',                     'Desktop stapler, 20-sheet capacity',       'Office Supplies',   'Bluepeak Office Co',          9.99, 5
    UNION ALL SELECT 'FUR-001',  'Ergonomic Office Chair',      'Adjustable chair with lumbar support',     'Furniture',         'Oakline Furniture',         189.00, 2
    UNION ALL SELECT 'FUR-002',  'Standing Desk',               'Electric height-adjustable desk',          'Furniture',         'Oakline Furniture',         449.00, 1
    UNION ALL SELECT 'NET-001',  'Cat6 Ethernet Cable (3 m)',   'Shielded Cat6 patch cable',                'Networking',        'Clearwave Networks',          5.49, 25
    UNION ALL SELECT 'NET-002',  '8-Port Gigabit Switch',       'Unmanaged desktop switch',                 'Networking',        'Clearwave Networks',         39.90, 4
    UNION ALL SELECT 'CLN-001',  'Surface Disinfectant (1 L)',  'Multi-surface cleaning spray',             'Cleaning Supplies', NULL,                          7.25, 15
    UNION ALL SELECT 'CLN-002',  'Microfiber Cloths (Pack of 10)', 'Reusable cleaning cloths',              'Cleaning Supplies', NULL,                          8.40, 10
) AS p
JOIN categories c ON c.name = p.category
LEFT JOIN suppliers s ON s.name = p.supplier
ON DUPLICATE KEY UPDATE sku = products.sku;
