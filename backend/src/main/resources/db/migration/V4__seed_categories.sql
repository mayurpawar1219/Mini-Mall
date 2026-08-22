-- V4__seed_categories.sql
INSERT IGNORE INTO categories (name, description, created_at, updated_at) VALUES
('Fruits & Vegetables', 'Fresh fruits and vegetables', NOW(), NOW()),
('Dairy & Milk', 'Milk, cheese, butter, and other dairy products', NOW(), NOW()),
('Bakery', 'Breads, buns, and baked goods', NOW(), NOW()),
('Eggs', 'Fresh eggs', NOW(), NOW()),
('Meat & Seafood', 'Fresh meat, poultry, and seafood', NOW(), NOW()),
('Beverages', 'Soft drinks, juices, tea, and coffee', NOW(), NOW()),
('Snacks', 'Chips, namkeen, and other snacks', NOW(), NOW()),
('Biscuits', 'Cookies and biscuits', NOW(), NOW()),
('Staples', 'Dal, pulses, rice, atta, and other staples', NOW(), NOW()),
('Personal Care', 'Soaps, shampoos, and personal hygiene', NOW(), NOW()),
('Household', 'Cleaning supplies and household items', NOW(), NOW()),
('Frozen Foods', 'Frozen peas, snacks, and meals', NOW(), NOW());
