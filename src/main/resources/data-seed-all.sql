/*
    SMMS - FULL DEMO DATA FOR SQL SERVER
    Database configured by application.properties: supermarket

    Usage:
      1. Start the Spring Boot application once so Hibernate creates/updates the schema.
      2. Open this file in SSMS and select database [supermarket].
      3. Execute the whole script.

    The script is idempotent: business keys are checked before inserting.
    Demo login (all accounts): password
*/

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    /* ============================================================
       1. ROLES AND USERS
       ============================================================ */
    IF NOT EXISTS (SELECT 1 FROM role WHERE role_name = 'ADMIN')
        INSERT INTO role (role_name) VALUES ('ADMIN');
    IF NOT EXISTS (SELECT 1 FROM role WHERE role_name = 'MANAGER')
        INSERT INTO role (role_name) VALUES ('MANAGER');
    IF NOT EXISTS (SELECT 1 FROM role WHERE role_name = 'CASHIER')
        INSERT INTO role (role_name) VALUES ('CASHIER');

    DECLARE @passwordHash NVARCHAR(255) =
        '$2a$10$4EI69xA12fh40ZeYtf2p9uIVw9EfLtDL9mQKSeZp0Gw7hUo0Sxria';

    IF NOT EXISTS (SELECT 1 FROM [user] WHERE username = 'admin')
        INSERT INTO [user]
            (username, password, name, email, phone, role_id, status, created_at, updated_at)
        SELECT 'admin', @passwordHash, N'Nguyễn Minh Admin', 'admin@smms.local', '0901000001',
               role_id, 1, DATEADD(day, -180, GETDATE()), GETDATE()
        FROM role WHERE role_name = 'ADMIN';

    IF NOT EXISTS (SELECT 1 FROM [user] WHERE username = 'manager')
        INSERT INTO [user]
            (username, password, name, email, phone, role_id, status, created_at, updated_at)
        SELECT 'manager', @passwordHash, N'Trần Thu Hà', 'minhdang110105@gmail.com', '0901000002',
               role_id, 1, DATEADD(day, -150, GETDATE()), GETDATE()
        FROM role WHERE role_name = 'MANAGER';

    IF NOT EXISTS (SELECT 1 FROM [user] WHERE username = 'cashier')
        INSERT INTO [user]
            (username, password, name, email, phone, role_id, status, created_at, updated_at)
        SELECT 'cashier', @passwordHash, N'Lê Hoàng Nam', 'cashier@smms.local', '0901000003',
               role_id, 1, DATEADD(day, -120, GETDATE()), GETDATE()
        FROM role WHERE role_name = 'CASHIER';

    IF NOT EXISTS (SELECT 1 FROM [user] WHERE username = 'cashier2')
        INSERT INTO [user]
            (username, password, name, email, phone, role_id, status, created_at, updated_at)
        SELECT 'cashier2', @passwordHash, N'Phạm Thảo Vy', 'cashier2@smms.local', '0901000004',
               role_id, 1, DATEADD(day, -90, GETDATE()), GETDATE()
        FROM role WHERE role_name = 'CASHIER';

    IF NOT EXISTS (SELECT 1 FROM [user] WHERE username = 'warehouse')
        INSERT INTO [user]
            (username, password, name, email, phone, role_id, status, created_at, updated_at)
        SELECT 'warehouse', @passwordHash, N'Võ Quốc Bảo', 'warehouse@smms.local', '0901000005',
               role_id, 1, DATEADD(day, -75, GETDATE()), GETDATE()
        FROM role WHERE role_name = 'MANAGER';

    IF NOT EXISTS (SELECT 1 FROM [user] WHERE username = 'inactive.staff')
        INSERT INTO [user]
            (username, password, name, email, phone, role_id, status, created_at, updated_at)
        SELECT 'inactive.staff', @passwordHash, N'Nhân viên đã nghỉ', 'inactive@smms.local', '0901000006',
               role_id, 0, DATEADD(day, -200, GETDATE()), DATEADD(day, -30, GETDATE())
        FROM role WHERE role_name = 'CASHIER';

    -- Keep all documented demo accounts usable even if they came from an older seed.
    UPDATE [user]
    SET password = @passwordHash, updated_at = GETDATE()
    WHERE username IN ('admin', 'manager', 'cashier', 'cashier2', 'warehouse', 'inactive.staff');

    /* ============================================================
       2. CATEGORIES
       ============================================================ */
    IF NOT EXISTS (SELECT 1 FROM category WHERE name = N'Đồ uống')
        INSERT INTO category (name, description, status)
        VALUES (N'Đồ uống', N'Nước ngọt, nước suối, trà và cà phê', 1);
    IF NOT EXISTS (SELECT 1 FROM category WHERE name = N'Sữa và sản phẩm từ sữa')
        INSERT INTO category (name, description, status)
        VALUES (N'Sữa và sản phẩm từ sữa', N'Sữa tươi, sữa chua và phô mai', 1);
    IF NOT EXISTS (SELECT 1 FROM category WHERE name = N'Thực phẩm đóng gói')
        INSERT INTO category (name, description, status)
        VALUES (N'Thực phẩm đóng gói', N'Mì, bánh, snack và đồ hộp', 1);
    IF NOT EXISTS (SELECT 1 FROM category WHERE name = N'Rau củ quả')
        INSERT INTO category (name, description, status)
        VALUES (N'Rau củ quả', N'Rau, củ và trái cây tươi', 1);
    IF NOT EXISTS (SELECT 1 FROM category WHERE name = N'Gia vị và dầu ăn')
        INSERT INTO category (name, description, status)
        VALUES (N'Gia vị và dầu ăn', N'Nước mắm, đường, muối và dầu ăn', 1);
    IF NOT EXISTS (SELECT 1 FROM category WHERE name = N'Chăm sóc cá nhân')
        INSERT INTO category (name, description, status)
        VALUES (N'Chăm sóc cá nhân', N'Dầu gội, sữa tắm và kem đánh răng', 1);
    IF NOT EXISTS (SELECT 1 FROM category WHERE name = N'Đồ gia dụng')
        INSERT INTO category (name, description, status)
        VALUES (N'Đồ gia dụng', N'Nước rửa chén, giấy và vật dụng gia đình', 1);
    IF NOT EXISTS (SELECT 1 FROM category WHERE name = N'Ngành hàng ngừng kinh doanh')
        INSERT INTO category (name, description, status)
        VALUES (N'Ngành hàng ngừng kinh doanh', N'Dữ liệu mẫu cho trạng thái ngừng hoạt động', 0);

    /* ============================================================
       3. SUPPLIERS
       ============================================================ */
    IF NOT EXISTS (SELECT 1 FROM supplier WHERE email = 'sales@coca-cola.demo')
        INSERT INTO supplier (name, contact_person, phone, email, address, status)
        VALUES (N'Coca-Cola Việt Nam', N'Nguyễn Văn Cường', '02838123456',
                'sales@coca-cola.demo', N'Thủ Đức, TP. Hồ Chí Minh', 1);
    IF NOT EXISTS (SELECT 1 FROM supplier WHERE email = 'sales@vinamilk.demo')
        INSERT INTO supplier (name, contact_person, phone, email, address, status)
        VALUES (N'Vinamilk', N'Trần Thị Mai', '02838234567',
                'sales@vinamilk.demo', N'Quận 7, TP. Hồ Chí Minh', 1);
    IF NOT EXISTS (SELECT 1 FROM supplier WHERE email = 'sales@acecook.demo')
        INSERT INTO supplier (name, contact_person, phone, email, address, status)
        VALUES (N'Acecook Việt Nam', N'Lê Quốc Hưng', '02838345678',
                'sales@acecook.demo', N'Tân Bình, TP. Hồ Chí Minh', 1);
    IF NOT EXISTS (SELECT 1 FROM supplier WHERE email = 'sales@unilever.demo')
        INSERT INTO supplier (name, contact_person, phone, email, address, status)
        VALUES (N'Unilever Việt Nam', N'Phạm Gia Linh', '02838456789',
                'sales@unilever.demo', N'Quận 1, TP. Hồ Chí Minh', 1);
    IF NOT EXISTS (SELECT 1 FROM supplier WHERE email = 'contact@freshfarm.demo')
        INSERT INTO supplier (name, contact_person, phone, email, address, status)
        VALUES (N'Nông trại Fresh Farm', N'Võ Thanh Sơn', '0909000011',
                'contact@freshfarm.demo', N'Đức Trọng, Lâm Đồng', 1);
    IF NOT EXISTS (SELECT 1 FROM supplier WHERE email = 'old@supplier.demo')
        INSERT INTO supplier (name, contact_person, phone, email, address, status)
        VALUES (N'Nhà cung cấp cũ', N'Đại diện cũ', '0909000099',
                'old@supplier.demo', N'Kho cũ', 0);

    /* ============================================================
       4. CUSTOMERS / MEMBERS
       ============================================================ */
    IF NOT EXISTS (SELECT 1 FROM customer WHERE member_card_id = 'CARD000001')
        INSERT INTO customer (member_card_id, name, phone, email, points, rank, status, created_at)
        VALUES ('CARD000001', N'Nguyễn An', '0912000001', 'an.customer@demo.local',
                120, 'Bronze', 1, DATEADD(day, -160, GETDATE()));
    IF NOT EXISTS (SELECT 1 FROM customer WHERE member_card_id = 'CARD000002')
        INSERT INTO customer (member_card_id, name, phone, email, points, rank, status, created_at)
        VALUES ('CARD000002', N'Trần Bình', '0912000002', 'binh.customer@demo.local',
                650, 'Silver', 1, DATEADD(day, -140, GETDATE()));
    IF NOT EXISTS (SELECT 1 FROM customer WHERE member_card_id = 'CARD000003')
        INSERT INTO customer (member_card_id, name, phone, email, points, rank, status, created_at)
        VALUES ('CARD000003', N'Lê Chi', '0912000003', 'chi.customer@demo.local',
                1750, 'Gold', 1, DATEADD(day, -120, GETDATE()));
    IF NOT EXISTS (SELECT 1 FROM customer WHERE member_card_id = 'CARD000004')
        INSERT INTO customer (member_card_id, name, phone, email, points, rank, status, created_at)
        VALUES ('CARD000004', N'Phạm Dũng', '0912000004', 'dung.customer@demo.local',
                5600, 'Platinum', 1, DATEADD(day, -100, GETDATE()));
    IF NOT EXISTS (SELECT 1 FROM customer WHERE member_card_id = 'CARD000005')
        INSERT INTO customer (member_card_id, name, phone, email, points, rank, status, created_at)
        VALUES ('CARD000005', N'Võ Hạnh', '0912000005', NULL,
                20, 'Bronze', 1, DATEADD(day, -45, GETDATE()));
    IF NOT EXISTS (SELECT 1 FROM customer WHERE member_card_id = 'CARD000006')
        INSERT INTO customer (member_card_id, name, phone, email, points, rank, status, created_at)
        VALUES ('CARD000006', N'Đặng Minh', '0912000006', 'minh.customer@demo.local',
                980, 'Silver', 1, DATEADD(day, -30, GETDATE()));
    IF NOT EXISTS (SELECT 1 FROM customer WHERE member_card_id = 'CARD000007')
        INSERT INTO customer (member_card_id, name, phone, email, points, rank, status, created_at)
        VALUES ('CARD000007', N'Khách hàng đã khóa', '0912000007', NULL,
                0, 'Bronze', 0, DATEADD(day, -200, GETDATE()));

    /* ============================================================
       5. PRODUCTS AND IMAGES
       Includes normal, low-stock, out-of-stock and inactive samples.
       ============================================================ */
    DECLARE @catDrink INT = (SELECT category_id FROM category WHERE name = N'Đồ uống');
    DECLARE @catDairy INT = (SELECT category_id FROM category WHERE name = N'Sữa và sản phẩm từ sữa');
    DECLARE @catPackaged INT = (SELECT category_id FROM category WHERE name = N'Thực phẩm đóng gói');
    DECLARE @catFresh INT = (SELECT category_id FROM category WHERE name = N'Rau củ quả');
    DECLARE @catSpice INT = (SELECT category_id FROM category WHERE name = N'Gia vị và dầu ăn');
    DECLARE @catPersonal INT = (SELECT category_id FROM category WHERE name = N'Chăm sóc cá nhân');
    DECLARE @catHome INT = (SELECT category_id FROM category WHERE name = N'Đồ gia dụng');

    DECLARE @supCoke INT = (SELECT supplier_id FROM supplier WHERE email = 'sales@coca-cola.demo');
    DECLARE @supMilk INT = (SELECT supplier_id FROM supplier WHERE email = 'sales@vinamilk.demo');
    DECLARE @supAce INT = (SELECT supplier_id FROM supplier WHERE email = 'sales@acecook.demo');
    DECLARE @supUni INT = (SELECT supplier_id FROM supplier WHERE email = 'sales@unilever.demo');
    DECLARE @supFarm INT = (SELECT supplier_id FROM supplier WHERE email = 'contact@freshfarm.demo');

    DECLARE @Products TABLE (
        barcode NVARCHAR(50), name NVARCHAR(255), description NVARCHAR(500), unit NVARCHAR(50),
        cost_price DECIMAL(19,4), selling_price DECIMAL(19,4), stock_level INT,
        min_stock_level INT, max_stock_level INT, status BIT, category_id INT, supplier_id INT
    );

    INSERT INTO @Products VALUES
        ('8930000000001', N'Coca-Cola 330ml', N'Nước ngọt có ga lon 330ml', N'Lon', 6500, 10000, 125, 30, 250, 1, @catDrink, @supCoke),
        ('8930000000002', N'Coca-Cola Zero 330ml', N'Nước ngọt không đường', N'Lon', 6800, 11000, 8, 20, 180, 1, @catDrink, @supCoke),
        ('8930000000003', N'Fanta Cam 330ml', N'Nước ngọt vị cam', N'Lon', 6500, 10000, 0, 20, 180, 1, @catDrink, @supCoke),
        ('8930000000004', N'Nước suối Dasani 500ml', N'Nước uống tinh khiết', N'Chai', 3500, 6000, 180, 40, 300, 1, @catDrink, @supCoke),
        ('8930000000005', N'Sữa tươi Vinamilk 1L', N'Sữa tươi tiệt trùng có đường', N'Hộp', 27000, 35000, 45, 15, 100, 1, @catDairy, @supMilk),
        ('8930000000006', N'Sữa chua Vinamilk', N'Lốc 4 hộp sữa chua có đường', N'Lốc', 22000, 29000, 12, 15, 100, 1, @catDairy, @supMilk),
        ('8930000000007', N'Sữa đặc Ông Thọ', N'Sữa đặc có đường 380g', N'Hộp', 19000, 26000, 65, 15, 120, 1, @catDairy, @supMilk),
        ('8930000000008', N'Mì Hảo Hảo tôm chua cay', N'Mì ăn liền gói 75g', N'Gói', 3200, 4500, 260, 80, 500, 1, @catPackaged, @supAce),
        ('8930000000009', N'Mì Đệ Nhất thịt bằm', N'Mì ăn liền gói 82g', N'Gói', 3800, 5500, 95, 40, 300, 1, @catPackaged, @supAce),
        ('8930000000010', N'Bánh quy Cosy Marie', N'Bánh quy sữa 144g', N'Gói', 13000, 19000, 38, 15, 100, 1, @catPackaged, @supAce),
        ('8930000000011', N'Khoai tây Đà Lạt', N'Khoai tây tươi loại 1', N'Kg', 18000, 28000, 32, 10, 80, 1, @catFresh, @supFarm),
        ('8930000000012', N'Cà chua beef', N'Cà chua tươi VietGAP', N'Kg', 16000, 26000, 6, 10, 60, 1, @catFresh, @supFarm),
        ('8930000000013', N'Táo Gala nhập khẩu', N'Táo Gala size vừa', N'Kg', 52000, 75000, 24, 8, 50, 1, @catFresh, @supFarm),
        ('8930000000014', N'Dầu ăn Simply 1L', N'Dầu đậu nành nguyên chất', N'Chai', 43000, 56000, 40, 12, 90, 1, @catSpice, @supUni),
        ('8930000000015', N'Nước mắm Nam Ngư 500ml', N'Nước mắm cá cơm', N'Chai', 29000, 39000, 35, 10, 80, 1, @catSpice, @supUni),
        ('8930000000016', N'Kem đánh răng P/S 180g', N'Bảo vệ răng chắc khỏe', N'Tuýp', 26000, 38000, 28, 10, 80, 1, @catPersonal, @supUni),
        ('8930000000017', N'Dầu gội Sunsilk 650g', N'Dầu gội mềm mượt diệu kỳ', N'Chai', 105000, 139000, 16, 8, 50, 1, @catPersonal, @supUni),
        ('8930000000018', N'Nước rửa chén Sunlight 750g', N'Hương chanh', N'Chai', 24000, 34000, 42, 12, 100, 1, @catHome, @supUni),
        ('8930000000019', N'Giấy vệ sinh Pulppy 10 cuộn', N'Giấy vệ sinh hai lớp', N'Bịch', 52000, 69000, 7, 10, 60, 1, @catHome, @supUni),
        ('8930000000020', N'Sản phẩm ngừng bán', N'Dữ liệu mẫu sản phẩm không hoạt động', N'Cái', 10000, 15000, 0, 5, 20, 0, @catHome, @supUni);

    INSERT INTO product
        (name, barcode, description, unit, cost_price, selling_price, stock_level,
         min_stock_level, max_stock_level, version, status, category_id, supplier_id,
         created_at, updated_at)
    SELECT p.name, p.barcode, p.description, p.unit, p.cost_price, p.selling_price,
           p.stock_level, p.min_stock_level, p.max_stock_level, 0, p.status,
           p.category_id, p.supplier_id, DATEADD(day, -60, GETDATE()), GETDATE()
    FROM @Products p
    WHERE NOT EXISTS (SELECT 1 FROM product x WHERE x.barcode = p.barcode);

    INSERT INTO product_image (product_id, image_url, image_order)
    SELECT p.product_id,
           'https://placehold.co/600x600?text=' + REPLACE(p.barcode, '89300000000', 'SP-'),
           0
    FROM product p
    WHERE p.barcode LIKE '89300000000%'
      AND NOT EXISTS (SELECT 1 FROM product_image pi WHERE pi.product_id = p.product_id);

    /* ============================================================
       6. PURCHASE ORDERS AND DETAILS
       ============================================================ */
    DECLARE @adminId INT = (SELECT user_id FROM [user] WHERE username = 'admin');
    DECLARE @managerId INT = (SELECT user_id FROM [user] WHERE username = 'manager');
    DECLARE @warehouseId INT = (SELECT user_id FROM [user] WHERE username = 'warehouse');
    DECLARE @cashierId INT = (SELECT user_id FROM [user] WHERE username = 'cashier');
    DECLARE @cashier2Id INT = (SELECT user_id FROM [user] WHERE username = 'cashier2');
    DECLARE @poId INT;

    IF NOT EXISTS (SELECT 1 FROM purchase_order WHERE note = N'[DEMO] Đơn nháp Acecook')
    BEGIN
        INSERT INTO purchase_order
            (order_date, status, total_amount, note, expected_delivery_date, created_by, supplier_id)
        VALUES (DATEADD(day, -1, GETDATE()), 'DRAFT', 0, N'[DEMO] Đơn nháp Acecook',
                DATEADD(day, 3, GETDATE()), @managerId, @supAce);
        SET @poId = SCOPE_IDENTITY();
        INSERT INTO purchase_order_detail (po_id, product_id, quantity, unit_price, total_price)
        SELECT @poId, product_id, 200, cost_price, 200 * cost_price
        FROM product WHERE barcode = '8930000000008';
        INSERT INTO purchase_order_detail (po_id, product_id, quantity, unit_price, total_price)
        SELECT @poId, product_id, 100, cost_price, 100 * cost_price
        FROM product WHERE barcode = '8930000000009';
        UPDATE purchase_order
        SET total_amount = (SELECT SUM(total_price) FROM purchase_order_detail WHERE po_id = @poId)
        WHERE po_id = @poId;
    END;

    IF NOT EXISTS (SELECT 1 FROM purchase_order WHERE note = N'[DEMO] Đơn đang chờ Vinamilk')
    BEGIN
        INSERT INTO purchase_order
            (order_date, status, total_amount, note, expected_delivery_date, created_by, supplier_id)
        VALUES (DATEADD(day, -2, GETDATE()), 'SENT', 0, N'[DEMO] Đơn đang chờ Vinamilk',
                DATEADD(day, 1, GETDATE()), @managerId, @supMilk);
        SET @poId = SCOPE_IDENTITY();
        INSERT INTO purchase_order_detail (po_id, product_id, quantity, unit_price, total_price)
        SELECT @poId, product_id, 50, cost_price, 50 * cost_price
        FROM product WHERE barcode IN ('8930000000005', '8930000000006', '8930000000007');
        UPDATE purchase_order
        SET total_amount = (SELECT SUM(total_price) FROM purchase_order_detail WHERE po_id = @poId)
        WHERE po_id = @poId;
    END;

    IF NOT EXISTS (SELECT 1 FROM purchase_order WHERE note = N'[DEMO] Đơn đã nhập Coca-Cola')
    BEGIN
        INSERT INTO purchase_order
            (order_date, status, total_amount, note, expected_delivery_date, created_by, supplier_id)
        VALUES (DATEADD(day, -12, GETDATE()), 'COMPLETED', 0, N'[DEMO] Đơn đã nhập Coca-Cola',
                DATEADD(day, -9, GETDATE()), @adminId, @supCoke);
        SET @poId = SCOPE_IDENTITY();
        INSERT INTO purchase_order_detail (po_id, product_id, quantity, unit_price, total_price)
        SELECT @poId, product_id, 100, cost_price, 100 * cost_price
        FROM product WHERE barcode IN ('8930000000001', '8930000000002', '8930000000003', '8930000000004');
        UPDATE purchase_order
        SET total_amount = (SELECT SUM(total_price) FROM purchase_order_detail WHERE po_id = @poId)
        WHERE po_id = @poId;
    END;

    IF NOT EXISTS (SELECT 1 FROM purchase_order WHERE note = N'[DEMO] Đơn đã hủy Fresh Farm')
        INSERT INTO purchase_order
            (order_date, status, total_amount, note, expected_delivery_date, created_by, supplier_id)
        VALUES (DATEADD(day, -7, GETDATE()), 'CANCELLED', 1450000,
                N'[DEMO] Đơn đã hủy Fresh Farm', DATEADD(day, -5, GETDATE()), @managerId, @supFarm);

    /* ============================================================
       7. GOODS RECEIPTS: completed and pending
       ============================================================ */
    DECLARE @completedPoId INT =
        (SELECT po_id FROM purchase_order WHERE note = N'[DEMO] Đơn đã nhập Coca-Cola');
    DECLARE @sentPoId INT =
        (SELECT po_id FROM purchase_order WHERE note = N'[DEMO] Đơn đang chờ Vinamilk');
    DECLARE @receiptId INT;

    IF @completedPoId IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM goods_receipt WHERE po_id = @completedPoId)
    BEGIN
        INSERT INTO goods_receipt (po_id, received_by, received_date, status, note)
        VALUES (@completedPoId, @warehouseId, DATEADD(day, -9, GETDATE()), 'COMPLETED',
                N'[DEMO] Đã kiểm đếm và nhập đủ hàng');
        SET @receiptId = SCOPE_IDENTITY();

        INSERT INTO goods_receipt_detail
            (receipt_id, product_id, ordered_quantity, received_quantity, expiry_date, batch_number)
        SELECT @receiptId, pod.product_id, pod.quantity, pod.quantity,
               CASE
                   WHEN p.barcode = '8930000000001' THEN DATEADD(day, 5, CAST(GETDATE() AS date))
                   WHEN p.barcode = '8930000000002' THEN DATEADD(day, 20, CAST(GETDATE() AS date))
                   ELSE DATEADD(month, 6, CAST(GETDATE() AS date))
               END,
               'COCA-' + RIGHT(p.barcode, 4)
        FROM purchase_order_detail pod
        JOIN product p ON p.product_id = pod.product_id
        WHERE pod.po_id = @completedPoId;
    END;

    IF @sentPoId IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM goods_receipt WHERE po_id = @sentPoId)
        INSERT INTO goods_receipt (po_id, received_by, received_date, status, note)
        VALUES (@sentPoId, @warehouseId, GETDATE(), 'PENDING',
                N'[DEMO] Phiếu chờ nhận hàng từ nhà cung cấp');

    /* ============================================================
       8. PROMOTIONS
       ============================================================ */
    IF NOT EXISTS (SELECT 1 FROM promotion WHERE promo_code = 'WELCOME10')
        INSERT INTO promotion
            (promo_code, name, description, discount_type, discount_value, apply_target,
             min_order_amount, start_date, end_date, active, created_by, created_at)
        VALUES ('WELCOME10', N'Giảm 10% đơn từ 100.000đ', N'Khuyến mãi toàn đơn mẫu',
                'PERCENTAGE', 10, NULL, 100000, DATEADD(day, -15, CAST(GETDATE() AS date)),
                DATEADD(day, 30, CAST(GETDATE() AS date)), 1, 'admin', GETDATE());

    IF NOT EXISTS (SELECT 1 FROM promotion WHERE promo_code = 'DRINK20')
        INSERT INTO promotion
            (promo_code, name, description, discount_type, discount_value, apply_target,
             category_id, min_order_amount, start_date, end_date, active, created_by, created_at)
        VALUES ('DRINK20', N'Giảm 20% ngành đồ uống', N'Áp dụng cho các sản phẩm đồ uống',
                'PERCENTAGE', 20, 'CATEGORY', @catDrink, 50000,
                DATEADD(day, -7, CAST(GETDATE() AS date)),
                DATEADD(day, 14, CAST(GETDATE() AS date)), 1, 'manager', GETDATE());

    IF NOT EXISTS (SELECT 1 FROM promotion WHERE promo_code = 'HOUSE30K')
        INSERT INTO promotion
            (promo_code, name, description, discount_type, discount_value, apply_target,
             min_order_amount, start_date, end_date, active, created_by, created_at)
        VALUES ('HOUSE30K', N'Giảm 30.000đ đồ gia dụng', N'Mã giảm cố định cho sản phẩm chọn lọc',
                'FIXED_AMOUNT', 30000, 'PRODUCT', 200000,
                DATEADD(day, -3, CAST(GETDATE() AS date)),
                DATEADD(day, 20, CAST(GETDATE() AS date)), 1, 'manager', GETDATE());

    IF NOT EXISTS (SELECT 1 FROM promotion WHERE promo_code = 'COKE2GET1')
        INSERT INTO promotion
            (promo_code, name, description, discount_type, discount_value, apply_target,
             buy_quantity, get_quantity, get_product_id, min_order_amount,
             start_date, end_date, active, created_by, created_at)
        SELECT 'COKE2GET1', N'Mua 2 Coca tặng 1', N'Khuyến mãi mua X tặng Y',
               'BUY_X_GET_Y', 0, 'PRODUCT', 2, 1, product_id, 0,
               DATEADD(day, -5, CAST(GETDATE() AS date)),
               DATEADD(day, 25, CAST(GETDATE() AS date)), 1, 'admin', GETDATE()
        FROM product WHERE barcode = '8930000000001';

    IF NOT EXISTS (SELECT 1 FROM promotion WHERE promo_code = 'EXPIRED5')
        INSERT INTO promotion
            (promo_code, name, description, discount_type, discount_value, apply_target,
             min_order_amount, start_date, end_date, active, created_by, created_at)
        VALUES ('EXPIRED5', N'Chương trình đã kết thúc', N'Dữ liệu mẫu khuyến mãi hết hạn',
                'PERCENTAGE', 5, NULL, 0, DATEADD(day, -60, CAST(GETDATE() AS date)),
                DATEADD(day, -30, CAST(GETDATE() AS date)), 0, 'admin', DATEADD(day, -60, GETDATE()));

    DECLARE @promoDrink INT = (SELECT promotion_id FROM promotion WHERE promo_code = 'DRINK20');
    DECLARE @promoHouse INT = (SELECT promotion_id FROM promotion WHERE promo_code = 'HOUSE30K');
    DECLARE @promoCoke INT = (SELECT promotion_id FROM promotion WHERE promo_code = 'COKE2GET1');

    INSERT INTO promotion_product (promotion_id, product_id)
    SELECT @promoDrink, p.product_id
    FROM product p
    WHERE p.barcode IN ('8930000000001', '8930000000002', '8930000000003', '8930000000004')
      AND NOT EXISTS (
          SELECT 1 FROM promotion_product pp
          WHERE pp.promotion_id = @promoDrink AND pp.product_id = p.product_id
      );

    INSERT INTO promotion_product (promotion_id, product_id)
    SELECT @promoHouse, p.product_id
    FROM product p
    WHERE p.barcode IN ('8930000000018', '8930000000019')
      AND NOT EXISTS (
          SELECT 1 FROM promotion_product pp
          WHERE pp.promotion_id = @promoHouse AND pp.product_id = p.product_id
      );

    INSERT INTO promotion_product (promotion_id, product_id)
    SELECT @promoCoke, p.product_id
    FROM product p
    WHERE p.barcode = '8930000000001'
      AND NOT EXISTS (
          SELECT 1 FROM promotion_product pp
          WHERE pp.promotion_id = @promoCoke AND pp.product_id = p.product_id
      );

    /* ============================================================
       9. SALES ORDERS
       Creates sales over the last 35 days so dashboard/report charts
       have data for today, week, month and previous month.
       ============================================================ */
    DECLARE @dayOffset INT = 0;
    DECLARE @invoice NVARCHAR(50);
    DECLARE @orderDate DATETIME2;
    DECLARE @salesOrderId INT;
    DECLARE @customerId INT;
    DECLARE @subtotal DECIMAL(19,4);
    DECLARE @discount DECIMAL(19,4);
    DECLARE @paymentMethod NVARCHAR(20);
    DECLARE @paymentStatus NVARCHAR(20);
    DECLARE @line1 INT;
    DECLARE @line2 INT;
    DECLARE @qty1 INT;
    DECLARE @qty2 INT;

    WHILE @dayOffset <= 35
    BEGIN
        SET @invoice = 'INV-DEMO-' + CONVERT(CHAR(8), DATEADD(day, -@dayOffset, GETDATE()), 112) + '-01';
        SET @orderDate = DATEADD(hour, 8 + (@dayOffset % 12),
                         CAST(CAST(DATEADD(day, -@dayOffset, GETDATE()) AS date) AS datetime2));

        IF NOT EXISTS (SELECT 1 FROM sales_order WHERE invoice_number = @invoice)
        BEGIN
            SET @line1 = (SELECT product_id FROM product
                          WHERE barcode = CASE @dayOffset % 5
                              WHEN 0 THEN '8930000000001'
                              WHEN 1 THEN '8930000000008'
                              WHEN 2 THEN '8930000000005'
                              WHEN 3 THEN '8930000000014'
                              ELSE '8930000000018' END);
            SET @line2 = (SELECT product_id FROM product
                          WHERE barcode = CASE @dayOffset % 4
                              WHEN 0 THEN '8930000000004'
                              WHEN 1 THEN '8930000000010'
                              WHEN 2 THEN '8930000000013'
                              ELSE '8930000000016' END);
            SET @qty1 = 1 + (@dayOffset % 4);
            SET @qty2 = 1 + (@dayOffset % 2);
            SET @customerId = CASE WHEN @dayOffset % 3 = 0 THEN NULL ELSE
                (SELECT customer_id FROM customer
                 WHERE member_card_id = 'CARD' + RIGHT('000000' + CAST((@dayOffset % 6) + 1 AS VARCHAR(6)), 6))
                END;
            SET @paymentMethod = CASE @dayOffset % 3
                WHEN 0 THEN 'CASH' WHEN 1 THEN 'CARD' ELSE 'VNPAY' END;
            SET @paymentStatus = CASE WHEN @dayOffset = 4 THEN 'FAILED' ELSE 'PAID' END;
            SET @subtotal =
                (SELECT selling_price * @qty1 FROM product WHERE product_id = @line1) +
                (SELECT selling_price * @qty2 FROM product WHERE product_id = @line2);
            SET @discount = CASE WHEN @subtotal >= 100000 THEN ROUND(@subtotal * 0.10, 0) ELSE 0 END;

            INSERT INTO sales_order
                (invoice_number, order_date, cashier_id, customer_id, promotion_id,
                 subtotal, discount_amount, tax_amount, total_amount, payment_method,
                 received_amount, change_amount, payment_status, vnpay_transaction_no)
            VALUES
                (@invoice, @orderDate,
                 CASE WHEN @dayOffset % 2 = 0 THEN @cashierId ELSE @cashier2Id END,
                 @customerId,
                 CASE WHEN @discount > 0 THEN
                     (SELECT promotion_id FROM promotion WHERE promo_code = 'WELCOME10')
                     ELSE NULL END,
                 @subtotal, @discount, 0, @subtotal - @discount, @paymentMethod,
                 CASE WHEN @paymentMethod = 'CASH' THEN CEILING((@subtotal - @discount) / 10000) * 10000
                      ELSE @subtotal - @discount END,
                 CASE WHEN @paymentMethod = 'CASH'
                      THEN CEILING((@subtotal - @discount) / 10000) * 10000 - (@subtotal - @discount)
                      ELSE 0 END,
                 @paymentStatus,
                 CASE WHEN @paymentMethod = 'VNPAY' THEN 'VNP-DEMO-' + CONVERT(VARCHAR(3), @dayOffset)
                      ELSE NULL END);
            SET @salesOrderId = SCOPE_IDENTITY();

            INSERT INTO sales_order_detail
                (sales_order_id, product_id, quantity, unit_price, cost_price, discount_amount, total_price)
            SELECT @salesOrderId, product_id, @qty1, selling_price, cost_price, 0,
                   selling_price * @qty1
            FROM product WHERE product_id = @line1;

            INSERT INTO sales_order_detail
                (sales_order_id, product_id, quantity, unit_price, cost_price, discount_amount, total_price)
            SELECT @salesOrderId, product_id, @qty2, selling_price, cost_price, 0,
                   selling_price * @qty2
            FROM product WHERE product_id = @line2;
        END;

        SET @dayOffset = @dayOffset + 1;
    END;

    /* Extra transactions today for recent-transactions and hourly charts. */
    DECLARE @todayIndex INT = 2;
    WHILE @todayIndex <= 6
    BEGIN
        SET @invoice = 'INV-DEMO-' + CONVERT(CHAR(8), GETDATE(), 112)
                     + '-0' + CAST(@todayIndex AS VARCHAR(1));
        IF NOT EXISTS (SELECT 1 FROM sales_order WHERE invoice_number = @invoice)
        BEGIN
            SET @line1 = (SELECT product_id FROM product
                          WHERE barcode = '89300000000' + RIGHT('0' + CAST(@todayIndex AS VARCHAR(2)), 2));
            IF @line1 IS NULL
                SET @line1 = (SELECT product_id FROM product WHERE barcode = '8930000000001');
            SET @subtotal = (SELECT selling_price * @todayIndex FROM product WHERE product_id = @line1);

            INSERT INTO sales_order
                (invoice_number, order_date, cashier_id, customer_id, promotion_id,
                 subtotal, discount_amount, tax_amount, total_amount, payment_method,
                 received_amount, change_amount, payment_status)
            VALUES
                (@invoice, DATEADD(hour, 7 + (@todayIndex * 2), CAST(CAST(GETDATE() AS date) AS datetime2)),
                 CASE WHEN @todayIndex % 2 = 0 THEN @cashierId ELSE @cashier2Id END,
                 (SELECT customer_id FROM customer
                  WHERE member_card_id = 'CARD' + RIGHT('000000' + CAST(@todayIndex AS VARCHAR(6)), 6)),
                 NULL, @subtotal, 0, 0, @subtotal,
                 CASE WHEN @todayIndex % 2 = 0 THEN 'CASH' ELSE 'CARD' END,
                 @subtotal, 0, 'PAID');
            SET @salesOrderId = SCOPE_IDENTITY();

            INSERT INTO sales_order_detail
                (sales_order_id, product_id, quantity, unit_price, cost_price, discount_amount, total_price)
            SELECT @salesOrderId, product_id, @todayIndex, selling_price, cost_price, 0,
                   selling_price * @todayIndex
            FROM product WHERE product_id = @line1;
        END;
        SET @todayIndex = @todayIndex + 1;
    END;

    /* ============================================================
       10. STOCK ADJUSTMENTS
       Quantity is positive; reason/note describe the stock movement.
       ============================================================ */
    IF NOT EXISTS (SELECT 1 FROM stock_adjustment WHERE note = N'[DEMO] Hàng bị móp bao bì')
        INSERT INTO stock_adjustment (product_id, adjusted_by, quantity, reason, note, adjusted_at)
        SELECT product_id, @warehouseId, 3, 'DAMAGED', N'[DEMO] Hàng bị móp bao bì',
               DATEADD(day, -2, GETDATE())
        FROM product WHERE barcode = '8930000000008';

    IF NOT EXISTS (SELECT 1 FROM stock_adjustment WHERE note = N'[DEMO] Hàng hết hạn sử dụng')
        INSERT INTO stock_adjustment (product_id, adjusted_by, quantity, reason, note, adjusted_at)
        SELECT product_id, @warehouseId, 2, 'EXPIRED', N'[DEMO] Hàng hết hạn sử dụng',
               DATEADD(day, -5, GETDATE())
        FROM product WHERE barcode = '8930000000006';

    IF NOT EXISTS (SELECT 1 FROM stock_adjustment WHERE note = N'[DEMO] Thất thoát khi vận chuyển')
        INSERT INTO stock_adjustment (product_id, adjusted_by, quantity, reason, note, adjusted_at)
        SELECT product_id, @managerId, 1, 'LOST', N'[DEMO] Thất thoát khi vận chuyển',
               DATEADD(day, -8, GETDATE())
        FROM product WHERE barcode = '8930000000014';

    /* ============================================================
       11. INVENTORY CHECKS
       ============================================================ */
    DECLARE @checkId INT;
    IF NOT EXISTS (SELECT 1 FROM inventory_check WHERE note = N'[DEMO] Kiểm kê đồ uống hoàn tất')
    BEGIN
        INSERT INTO inventory_check (checked_by, category_id, check_date, status, note)
        VALUES (@warehouseId, @catDrink, DATEADD(day, -3, GETDATE()), 'COMPLETED',
                N'[DEMO] Kiểm kê đồ uống hoàn tất');
        SET @checkId = SCOPE_IDENTITY();

        INSERT INTO inventory_check_detail
            (check_id, product_id, system_quantity, actual_quantity, difference, note)
        SELECT @checkId, product_id, stock_level,
               CASE WHEN barcode = '8930000000002' THEN stock_level - 1 ELSE stock_level END,
               CASE WHEN barcode = '8930000000002' THEN -1 ELSE 0 END,
               CASE WHEN barcode = '8930000000002' THEN N'Lệch 1 sản phẩm' ELSE N'Khớp tồn kho' END
        FROM product WHERE category_id = @catDrink AND status = 1;
    END;

    IF NOT EXISTS (SELECT 1 FROM inventory_check WHERE note = N'[DEMO] Kiểm kê thực phẩm đang thực hiện')
    BEGIN
        INSERT INTO inventory_check (checked_by, category_id, check_date, status, note)
        VALUES (@warehouseId, @catPackaged, GETDATE(), 'IN_PROGRESS',
                N'[DEMO] Kiểm kê thực phẩm đang thực hiện');
        SET @checkId = SCOPE_IDENTITY();

        INSERT INTO inventory_check_detail
            (check_id, product_id, system_quantity, actual_quantity, difference, note)
        SELECT @checkId, product_id, stock_level, stock_level, 0, N'Đã đếm sơ bộ'
        FROM product WHERE category_id = @catPackaged AND status = 1;
    END;

    /* ============================================================
       12. SHIFTS
       ============================================================ */
    DECLARE @shiftDay INT = 0;
    WHILE @shiftDay <= 6
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM shift
            WHERE user_id = @cashierId
              AND shift_date = CAST(DATEADD(day, -@shiftDay, GETDATE()) AS date)
              AND shift_type = 'MORNING'
        )
            INSERT INTO shift (user_id, shift_date, shift_type, start_time, end_time, note)
            VALUES (@cashierId, CAST(DATEADD(day, -@shiftDay, GETDATE()) AS date),
                    'MORNING', '06:00', '14:00', N'[DEMO] Ca sáng');

        IF NOT EXISTS (
            SELECT 1 FROM shift
            WHERE user_id = @cashier2Id
              AND shift_date = CAST(DATEADD(day, -@shiftDay, GETDATE()) AS date)
              AND shift_type = 'AFTERNOON'
        )
            INSERT INTO shift (user_id, shift_date, shift_type, start_time, end_time, note)
            VALUES (@cashier2Id, CAST(DATEADD(day, -@shiftDay, GETDATE()) AS date),
                    'AFTERNOON', '14:00', '22:00', N'[DEMO] Ca chiều');

        SET @shiftDay = @shiftDay + 1;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM shift
        WHERE user_id = @managerId
          AND shift_date = CAST(GETDATE() AS date)
          AND shift_type = 'NIGHT'
    )
        INSERT INTO shift (user_id, shift_date, shift_type, start_time, end_time, note)
        VALUES (@managerId, CAST(GETDATE() AS date), 'NIGHT', '22:00', '23:59',
                N'[DEMO] Ca trực quản lý');

    /* ============================================================
       13. SYSTEM SETTINGS
       Keys exactly match SystemSettingServiceImpl.
       ============================================================ */
    DECLARE @Settings TABLE (
        setting_key NVARCHAR(100), setting_value NVARCHAR(1000), description NVARCHAR(500)
    );
    INSERT INTO @Settings VALUES
        ('STORE_NAME', N'SMMS Supermarket Demo', N'Tên siêu thị'),
        ('STORE_ADDRESS', N'Lô E2a-7, Đường D1, TP. Hồ Chí Minh', N'Địa chỉ siêu thị'),
        ('STORE_PHONE', N'028 7300 5588', N'Số điện thoại liên hệ'),
        ('STORE_EMAIL', N'contact@smms.local', N'Email liên hệ'),
        ('VAT_RATE', N'10.0', N'Thuế giá trị gia tăng (%)'),
        ('CURRENCY', N'Vietnamese Dong (VND)', N'Đơn vị tiền tệ'),
        ('LOW_STOCK_THRESHOLD', N'10', N'Ngưỡng cảnh báo tồn kho thấp');

    INSERT INTO system_setting (setting_key, setting_value, description)
    SELECT s.setting_key, s.setting_value, s.description
    FROM @Settings s
    WHERE NOT EXISTS (
        SELECT 1 FROM system_setting x WHERE x.setting_key = s.setting_key
    );

    COMMIT TRANSACTION;
    PRINT N'Hoàn tất seed dữ liệu mẫu cho toàn bộ dự án SMMS.';
    PRINT N'Tài khoản: admin, manager, cashier, cashier2, warehouse - mật khẩu: password';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
