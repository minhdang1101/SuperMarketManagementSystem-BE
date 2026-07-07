-- =============================================
-- Script thêm dữ liệu đăng nhập cho SMMS
-- Chạy sau khi ứng dụng đã khởi động (ddl-auto=update)
-- Database: SQL Server - SMMS
--
-- Cách chạy:
--   1. Mở SQL Server Management Studio, kết nối tới localhost
--   2. Chọn database SMMS
--   3. Mở file này và Execute (F5)
--
-- Tạo BCrypt hash mới (nếu cần):
--   Chạy: mvn exec:java -Dexec.mainClass="fu.se.smms.util.BcryptHashGen"
--   Hoặc dùng online: https://bcrypt-generator.com/
-- =============================================

-- 1. Thêm Role (nếu chưa tồn tại)
IF NOT EXISTS (SELECT 1 FROM role WHERE role_name = 'ADMIN')
    INSERT INTO role (role_name) VALUES ('ADMIN');

IF NOT EXISTS (SELECT 1 FROM role WHERE role_name = 'MANAGER')
    INSERT INTO role (role_name) VALUES ('MANAGER');

IF NOT EXISTS (SELECT 1 FROM role WHERE role_name = 'CASHIER')
    INSERT INTO role (role_name) VALUES ('CASHIER');

-- 2. Thêm User đăng nhập
-- Password: 123456 (BCrypt hash)
-- Password: admin123 (BCrypt hash) 
IF NOT EXISTS (SELECT 1 FROM [user] WHERE username = 'admin')
BEGIN
    INSERT INTO [user] (username, password, name, email, phone, role_id, status, created_at, updated_at)
    SELECT 
        'admin',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- password: "password"
        N'Quản trị viên',
        'admin@smms.local',
        '0901234567',
        (SELECT role_id FROM role WHERE role_name = 'ADMIN'),
        1,
        GETDATE(),
        GETDATE();
END

IF NOT EXISTS (SELECT 1 FROM [user] WHERE username = 'manager')
BEGIN
    INSERT INTO [user] (username, password, name, email, phone, role_id, status, created_at, updated_at)
    SELECT 
        'manager',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- password: "password"
        N'Quản lý',
        'manager@smms.local',
        '0902345678',
        (SELECT role_id FROM role WHERE role_name = 'MANAGER'),
        1,
        GETDATE(),
        GETDATE();
END

IF NOT EXISTS (SELECT 1 FROM [user] WHERE username = 'cashier')
BEGIN
    INSERT INTO [user] (username, password, name, email, phone, role_id, status, created_at, updated_at)
    SELECT 
        'cashier',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- password: "password"
        N'Nhân viên thu ngân',
        'cashier@smms.local',
        '0903456789',
        (SELECT role_id FROM role WHERE role_name = 'CASHIER'),
        1,
        GETDATE(),
        GETDATE();
END

-- =============================================
-- Thông tin đăng nhập mặc định:
-- Username: admin    | Password: password | Role: ADMIN
-- Username: manager  | Password: password | Role: MANAGER  
-- Username: cashier  | Password: password | Role: CASHIER
-- =============================================
