-- Ocean View Resort System Database Schema
-- MySQL Database Schema

-- Drop tables if they exist (for clean installation)
DROP TABLE IF EXISTS bills;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS guests;
DROP TABLE IF EXISTS users;

-- Users Table (with UUID as a primary key and BCrypt hashed passwords)
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,  -- UUID format
    name VARCHAR(100) NOT NULL,  -- Full name
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(60) NOT NULL,  -- BCrypt hash is 60 characters
    role VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Guests Table
CREATE TABLE guests (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    contact_number VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_contact (contact_number),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Reservations Table
CREATE TABLE reservations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    guest_id INT NOT NULL,
    room_type VARCHAR(50) NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'CONFIRMED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE CASCADE,
    INDEX idx_guest_id (guest_id),
    INDEX idx_status (status),
    INDEX idx_check_in (check_in),
    INDEX idx_check_out (check_out)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bills Table
CREATE TABLE bills (
    id INT PRIMARY KEY AUTO_INCREMENT,
    reservation_id INT NOT NULL,
    nights INT NOT NULL,
    rate_per_night DECIMAL(10, 2) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    generated_date DATE NOT NULL,
    is_paid BOOLEAN DEFAULT FALSE,
    payment_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
    INDEX idx_reservation_id (reservation_id),
    INDEX idx_generated_date (generated_date),
    INDEX idx_is_paid (is_paid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default admin user (password: admin123)
-- BCrypt hashed password for 'admin123' with work factor 12
INSERT INTO users (id, name, username, password, role, is_active)
VALUES ('550e8400-e29b-41d4-a716-446655440000',
        'System Administrator',
        'admin',
        'TEMP_HASH_WILL_BE_REPLACED',
        'ADMIN',
        TRUE);

-- Insert default staff user (password: staff123)
-- BCrypt hashed password for 'staff123' with work factor 12
INSERT INTO users (id, name, username, password, role, is_active)
VALUES ('550e8400-e29b-41d4-a716-446655440001',
        'Staff User',
        'staff',
        'TEMP_HASH_WILL_BE_REPLACED',
        'STAFF',
        TRUE);

-- Insert sample guests for testing
INSERT INTO guests (name, address, contact_number, email) VALUES
('John Doe', '123 Main St, New York', '+1-555-0101', 'john.doe@email.com'),
('Jane Smith', '456 Oak Ave, Los Angeles', '+1-555-0102', 'jane.smith@email.com'),
('Robert Johnson', '789 Pine Rd, Chicago', '+1-555-0103', 'robert.j@email.com');

-- Insert sample reservations for testing
INSERT INTO reservations (guest_id, room_type, check_in, check_out, status) VALUES
(1, 'DELUXE', '2026-03-01', '2026-03-05', 'CONFIRMED'),
(2, 'SUITE', '2026-03-10', '2026-03-15', 'CONFIRMED'),
(3, 'STANDARD', '2026-03-20', '2026-03-23', 'CONFIRMED');

-- Insert sample bills for testing
INSERT INTO bills (reservation_id, nights, rate_per_night, total_amount, generated_date, is_paid) VALUES
(1, 4, 150.00, 600.00, '2026-03-05', FALSE),
(2, 5, 250.00, 1250.00, '2026-03-15', FALSE),
(3, 3, 100.00, 300.00, '2026-03-23', FALSE);

-- Display a success message
SELECT 'Database schema created successfully!' as Message;
