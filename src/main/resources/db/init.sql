-- Drop tables if they exist to ensure a clean slate (optional, for development)
DROP TABLE IF EXISTS cart_items CASCADE;
DROP TABLE IF EXISTS cart CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Users Table
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL, -- Store hashed passwords
    email VARCHAR(100) UNIQUE NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Products Table
CREATE TABLE products (
    product_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT DEFAULT 0,
    image_url VARCHAR(255) -- Placeholder for product images
    -- category_id INT REFERENCES categories(category_id) -- Optional: if you add categories later
);

-- Orders Table
CREATE TABLE orders (
    order_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id),
    order_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'Pending', -- e.g., Pending, Processing, Shipped, Delivered, Cancelled
    delivery_address TEXT, -- Simplified address storage for now
    delivery_type VARCHAR(50), -- Added: e.g., STANDARD, EXPRESS
    customer_notes TEXT,       -- Added: Optional notes from the customer
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, -- Added
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP  -- Added
    -- shipping_address_id INT REFERENCES delivery_addresses(address_id), -- For more structured addresses
);

-- Order Items Table (Junction table for Orders and Products)
CREATE TABLE order_items (
    order_item_id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(order_id),
    product_id INT NOT NULL REFERENCES products(product_id),
    quantity INT NOT NULL,
    price_at_purchase DECIMAL(10, 2) NOT NULL -- Price at the time of order
);

-- Cart Table
CREATE TABLE cart (
    cart_id SERIAL PRIMARY KEY,
    user_id INT UNIQUE NOT NULL REFERENCES users(user_id), -- Each user has one cart
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Cart Items Table
CREATE TABLE cart_items (
    cart_item_id SERIAL PRIMARY KEY,
    cart_id INT NOT NULL REFERENCES cart(cart_id),
    product_id INT NOT NULL REFERENCES products(product_id),
    quantity INT NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(cart_id, product_id) -- Ensure a product appears only once per cart, update quantity instead
);

-- Sample Data

-- Add a test user (password: "password123")
-- Generate a BCrypt hash for "password123" (you'd typically do this in Java or a tool)
-- Example hash for "password123": $2a$10$ZyVvF.1.q9K4T6s9G2fXj.O2q9J2qO9lJ.F8E.pS5.2jI6S7q8N.K
INSERT INTO users (username, password_hash, email, first_name, last_name) VALUES
('testuser', '$2a$10$ZyVvF.1.q9K4T6s9G2fXj.O2q9J2qO9lJ.F8E.pS5.2jI6S7q8N.K', 'test@example.com', 'Test', 'User');

-- Sample Products (reusing and expanding from previous hardcoded data)
INSERT INTO products (name, description, price, stock_quantity, image_url) VALUES
('Ноутбук Pro X', 'Мощный ноутбук для профессионалов', 120000.00, 10, 'images/laptop_pro_x.png'),
('Смартфон Galaxy S25', 'Флагманский смартфон с лучшей камерой', 80000.00, 25, 'images/galaxy_s25.png'),
('Наушники BeatZ Studio', 'Беспроводные наушники с шумоподавлением', 15000.00, 50, 'images/beatz_studio.png'),
('Планшет TabMaster 10', 'Легкий и производительный планшет для работы и развлечений', 45000.00, 15, 'images/tabmaster_10.png'),
('Умные часы WatchFit 3', 'Стильные умные часы с трекером активности', 22000.00, 30, 'images/watchfit_3.png'),
('Беспроводная мышь ErgoClick', 'Эргономичная мышь для комфортной работы', 2500.00, 100, 'images/mouse_ergoclick.png'),
('Механическая клавиатура KeyStorm', 'Надежная механическая клавиатура для геймеров и программистов', 7000.00, 40, 'images/keyboard_keystorm.png');

-- You can run this script against your PostgreSQL database using a tool like psql or pgAdmin.
-- Example psql command:
-- psql -U your_postgres_user -d your_database_name -f path/to/init.sql 