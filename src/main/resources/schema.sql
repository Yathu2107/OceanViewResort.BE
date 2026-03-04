-- Ocean View Resort System Database Schema
-- MySQL Database Schema

-- Drop tables if they exist (for clean installation)
DROP TABLE IF EXISTS bills;
DROP TABLE IF EXISTS reservation_rooms;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS rooms;
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

-- Rooms Table (Manager Only - Add/Update)
CREATE TABLE rooms (
    id INT PRIMARY KEY AUTO_INCREMENT,
    room_number VARCHAR(10) UNIQUE NOT NULL,
    room_type ENUM(
        'STANDARD SINGLE', 'STANDARD DOUBLE', 'STANDARD TWIN',
        'DELUXE SINGLE', 'DELUXE DOUBLE', 'DELUXE TWIN', 'DELUXE FAMILY',
        'SUPERIOR DOUBLE', 'SUPERIOR TWIN', 'SUPERIOR FAMILY',
        'SUITE SEA VIEW', 'SUITE CITY VIEW', 'EXECUTIVE SUITE',
        'FAMILY SUITE', 'PRESIDENTIAL SUITE'
    ) NOT NULL,
    capacity INT NOT NULL,
    price_per_night DECIMAL(10, 2) NOT NULL,
    status ENUM('AVAILABLE','MAINTENANCE') DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_room_number (room_number),
    INDEX idx_room_type (room_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Reservations Table (now without room_id - rooms linked via junction table)
CREATE TABLE reservations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    guest_id INT NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    status ENUM('OCCUPIED', 'COMPLETED', 'CANCELLED') DEFAULT 'OCCUPIED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE CASCADE,
    INDEX idx_guest_id (guest_id),
    INDEX idx_status (status),
    INDEX idx_check_in (check_in),
    INDEX idx_check_out (check_out)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Reservation Rooms Junction Table (Many-to-Many relationship)
-- One reservation can have multiple rooms, one room can be in multiple reservations
CREATE TABLE reservation_rooms (
    reservation_id INT NOT NULL,
    room_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (reservation_id, room_id),
    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    INDEX idx_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bills Table
CREATE TABLE bills (
    id INT PRIMARY KEY AUTO_INCREMENT,
    reservation_id INT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    generated_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
    INDEX idx_reservation_id (reservation_id),
    INDEX idx_generated_date (generated_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Display a success message
SELECT 'Database schema created successfully!' as Message;
