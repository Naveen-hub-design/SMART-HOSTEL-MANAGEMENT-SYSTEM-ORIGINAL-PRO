CREATE DATABASE IF NOT EXISTS hostel_db;
USE hostel_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'WARDEN', 'STUDENT') NOT NULL,
    phone VARCHAR(20),
    profile_image_url VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hostel_blocks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    address VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_no VARCHAR(20) NOT NULL,
    block_id BIGINT NOT NULL,
    floor INT DEFAULT 1,
    capacity INT DEFAULT 2,
    occupants INT DEFAULT 0,
    status ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE') DEFAULT 'AVAILABLE',
    rent DOUBLE DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (block_id) REFERENCES hostel_blocks(id) ON DELETE CASCADE,
    UNIQUE KEY uk_room_block (room_no, block_id),
    INDEX idx_rooms_status (status),
    INDEX idx_rooms_block (block_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    room_id BIGINT,
    enrollment_no VARCHAR(50) NOT NULL UNIQUE,
    parent_contact VARCHAR(20),
    address VARCHAR(500),
    date_of_birth VARCHAR(20),
    gender VARCHAR(10),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL,
    INDEX idx_students_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    department VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wardens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    block_id BIGINT,
    qualification VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (block_id) REFERENCES hostel_blocks(id) ON DELETE SET NULL,
    UNIQUE KEY uk_wardens_block (block_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    from_date VARCHAR(20) NOT NULL,
    to_date VARCHAR(20) NOT NULL,
    reason TEXT,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    applied_at VARCHAR(50) DEFAULT CURRENT_TIMESTAMP,
    resolved_at VARCHAR(50),
    approved_by VARCHAR(100),
    remarks TEXT,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_leaves_student (student_id),
    INDEX idx_leaves_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS complaints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    status ENUM('PENDING', 'IN_PROGRESS', 'RESOLVED', 'REJECTED') DEFAULT 'PENDING',
    created_at VARCHAR(50) DEFAULT CURRENT_TIMESTAMP,
    resolved_at VARCHAR(50),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_complaints_student (student_id),
    INDEX idx_complaints_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    posted_by VARCHAR(100),
    posted_at VARCHAR(50) DEFAULT CURRENT_TIMESTAMP,
    expires_at VARCHAR(50),
    target_role VARCHAR(50) DEFAULT 'ALL',
    INDEX idx_notices_role (target_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mess_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    date VARCHAR(20),
    food_quality_rating INT,
    taste_rating INT,
    cleanliness_rating INT,
    comments TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_feedback_student (student_id),
    INDEX idx_feedback_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lost_and_found (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    status ENUM('LOST', 'FOUND', 'RESOLVED') DEFAULT 'LOST',
    category VARCHAR(100),
    location VARCHAR(200),
    contact_info VARCHAR(200),
    reported_by_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reported_by_id) REFERENCES students(id) ON DELETE SET NULL,
    INDEX idx_lf_status (status),
    INDEX idx_lf_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS marketplace_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    price DOUBLE NOT NULL,
    image_url VARCHAR(500),
    category VARCHAR(100),
    seller_id BIGINT NOT NULL,
    status ENUM('AVAILABLE', 'SOLD') DEFAULT 'AVAILABLE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES students(id) ON DELETE CASCADE,
    INDEX idx_market_status (status),
    INDEX idx_market_category (category),
    INDEX idx_market_seller (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO users (name, email, password, role, phone)
VALUES ('System Admin', 'admin@hostel.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', '9876543210');

INSERT INTO admins (user_id, department) VALUES (1, 'Administration');

INSERT INTO hostel_blocks (name, code, address) VALUES
('A Wing - Boys Hostel', 'A-BLOCK', 'Main Campus, North Side'),
('B Wing - Boys Hostel', 'B-BLOCK', 'Main Campus, North Side'),
('C Wing - Girls Hostel', 'C-BLOCK', 'Main Campus, South Side');

INSERT INTO rooms (room_no, block_id, floor, capacity, status, rent) VALUES
('A-101', 1, 1, 2, 'AVAILABLE', 5000),
('A-102', 1, 1, 2, 'AVAILABLE', 5000),
('A-103', 1, 1, 3, 'AVAILABLE', 6000),
('A-104', 1, 1, 2, 'AVAILABLE', 5000),
('A-201', 1, 2, 2, 'AVAILABLE', 5500),
('A-202', 1, 2, 2, 'AVAILABLE', 5500),
('A-203', 1, 2, 3, 'AVAILABLE', 6500),
('B-101', 2, 1, 2, 'AVAILABLE', 5000),
('B-102', 2, 1, 2, 'AVAILABLE', 5000),
('B-103', 2, 1, 2, 'AVAILABLE', 5000),
('B-201', 2, 2, 3, 'AVAILABLE', 6500),
('C-101', 3, 1, 2, 'AVAILABLE', 5000),
('C-102', 3, 1, 2, 'AVAILABLE', 5000),
('C-103', 3, 1, 2, 'AVAILABLE', 5000),
('C-201', 3, 2, 2, 'AVAILABLE', 5500);

INSERT INTO users (name, email, password, role, phone)
VALUES ('Mr. Sharma', 'warden@hostel.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'WARDEN', '9876543211');

INSERT INTO wardens (user_id, block_id, qualification) VALUES (2, 1, 'M.Sc. Hostel Management');

INSERT INTO users (name, email, password, role, phone)
VALUES ('Rahul Kumar', 'student@hostel.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STUDENT', '9876543212');

INSERT INTO students (user_id, room_id, enrollment_no, parent_contact, address, date_of_birth, gender)
VALUES (3, 1, 'ENR2024001', '9876543213', '123, Main Street, Delhi', '2000-01-15', 'MALE');

INSERT INTO leave_requests (student_id, from_date, to_date, reason, status)
VALUES (1, '2025-12-20', '2025-12-25', 'Going home for holidays', 'PENDING');

INSERT INTO complaints (student_id, title, description, status)
VALUES (1, 'Broken Fan', 'The ceiling fan in room A-101 is not working properly and making noise.', 'PENDING');

INSERT INTO notices (title, content, posted_by, target_role)
VALUES ('Hostel Fee Deadline', 'All students are requested to pay their hostel fees by 15th of every month.', 'System Admin', 'ALL');

INSERT INTO mess_feedback (student_id, date, food_quality_rating, taste_rating, cleanliness_rating, comments)
VALUES (1, '2025-12-01', 4, 3, 5, 'Good food overall, but could improve taste variety.');

INSERT INTO lost_and_found (title, description, status, category, location, contact_info, reported_by_id)
VALUES ('Blue Water Bottle', 'Milton blue water bottle lost in the mess hall.', 'LOST', 'BOTTLE', 'Main Mess Hall', 'Contact: student@hostel.com', 1);

INSERT INTO marketplace_items (title, description, price, category, seller_id, status)
VALUES ('Used Textbooks - Computer Science', 'Set of 5 computer science textbooks in good condition. Includes DSA, OS, DBMS, Networks, and SE.', 1500, 'BOOKS', 1, 'AVAILABLE');
