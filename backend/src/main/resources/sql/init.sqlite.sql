-- =====================================================
-- 翱翔无人机租赁系统 - SQLite 数据库初始化脚本
-- 数据库: SQLite 3
-- 特点：幂等，可重复执行，不会清空已有业务数据
-- =====================================================

-- =====================================================
-- 1. 用户表 (user)
-- =====================================================
CREATE TABLE IF NOT EXISTS user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    password TEXT NOT NULL,
    nickname TEXT,
    phone TEXT,
    email TEXT,
    avatar TEXT,
    address TEXT,
    role INTEGER NOT NULL DEFAULT 0,
    status INTEGER NOT NULL DEFAULT 1,
    credit_status INTEGER NOT NULL DEFAULT 1,
    balance DECIMAL(10,2) DEFAULT 0.00,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_username ON user(username);
CREATE INDEX IF NOT EXISTS idx_user_phone ON user(phone);
CREATE INDEX IF NOT EXISTS idx_user_status ON user(status);

-- =====================================================
-- 2. 用户飞行资质表 (user_qualification)
-- =====================================================
CREATE TABLE IF NOT EXISTS user_qualification (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    certificate_no TEXT NOT NULL,
    certificate_type TEXT,
    certificate_image TEXT,
    valid_start_date TEXT,
    valid_end_date TEXT,
    audit_status INTEGER NOT NULL DEFAULT 0,
    audit_remark TEXT,
    audit_time TEXT,
    auditor_id INTEGER,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_uq_user_id ON user_qualification(user_id);
CREATE INDEX IF NOT EXISTS idx_uq_audit_status ON user_qualification(audit_status);
CREATE INDEX IF NOT EXISTS idx_uq_certificate_no ON user_qualification(certificate_no);

-- =====================================================
-- 3. 无人机表 (drone)
-- =====================================================
CREATE TABLE IF NOT EXISTS drone (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    model TEXT NOT NULL,
    brand TEXT,
    type TEXT,
    description TEXT,
    image TEXT,
    price_per_day DECIMAL(10,2) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    flight_time INTEGER,
    max_payload DECIMAL(10,2),
    max_speed DECIMAL(10,2),
    max_range DECIMAL(10,2),
    status INTEGER NOT NULL DEFAULT 1,
    on_shelf INTEGER NOT NULL DEFAULT 1,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_drone_model ON drone(model);
CREATE INDEX IF NOT EXISTS idx_drone_status ON drone(status);
CREATE INDEX IF NOT EXISTS idx_drone_on_shelf ON drone(on_shelf);

-- =====================================================
-- 4. 无人机库存日志表 (drone_stock_log)
-- =====================================================
CREATE TABLE IF NOT EXISTS drone_stock_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    drone_id INTEGER NOT NULL,
    change_type INTEGER NOT NULL,
    change_amount INTEGER NOT NULL,
    before_stock INTEGER NOT NULL,
    after_stock INTEGER NOT NULL,
    related_order_id INTEGER,
    remark TEXT,
    operator_id INTEGER,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_dsl_drone_id ON drone_stock_log(drone_id);
CREATE INDEX IF NOT EXISTS idx_dsl_change_type ON drone_stock_log(change_type);
CREATE INDEX IF NOT EXISTS idx_dsl_created_time ON drone_stock_log(created_time);

-- =====================================================
-- 5. 空域备案表 (airspace_record)
-- =====================================================
CREATE TABLE IF NOT EXISTS airspace_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    region_name TEXT NOT NULL,
    region_address TEXT,
    longitude DECIMAL(10,6),
    latitude DECIMAL(10,6),
    radius INTEGER,
    max_altitude INTEGER,
    planned_start_time TEXT,
    planned_end_time TEXT,
    purpose TEXT,
    audit_status INTEGER NOT NULL DEFAULT 0,
    audit_remark TEXT,
    audit_time TEXT,
    auditor_id INTEGER,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ar_user_id ON airspace_record(user_id);
CREATE INDEX IF NOT EXISTS idx_ar_audit_status ON airspace_record(audit_status);
CREATE INDEX IF NOT EXISTS idx_ar_region_name ON airspace_record(region_name);

-- =====================================================
-- 6. 租赁订单表 (rental_order)
-- =====================================================
CREATE TABLE IF NOT EXISTS rental_order (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    drone_id INTEGER NOT NULL,
    airspace_record_id INTEGER,
    rental_start_time TEXT NOT NULL,
    rental_end_time TEXT NOT NULL,
    rental_days INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    deposit_amount DECIMAL(10,2) DEFAULT 0.00,
    delivery_address TEXT,
    order_status INTEGER NOT NULL DEFAULT 0,
    remark TEXT,
    cancel_reason TEXT,
    cancel_time TEXT,
    payment_method INTEGER,
    pay_time TEXT,
    ship_time TEXT,
    receive_time TEXT,
    return_time TEXT,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ro_order_no ON rental_order(order_no);
CREATE INDEX IF NOT EXISTS idx_ro_user_id ON rental_order(user_id);
CREATE INDEX IF NOT EXISTS idx_ro_drone_id ON rental_order(drone_id);
CREATE INDEX IF NOT EXISTS idx_ro_order_status ON rental_order(order_status);
CREATE INDEX IF NOT EXISTS idx_ro_created_time ON rental_order(created_time);

-- =====================================================
-- 7. 支付记录表 (payment)
-- =====================================================
CREATE TABLE IF NOT EXISTS payment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    payment_no TEXT NOT NULL,
    order_id INTEGER NOT NULL,
    order_no TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_type INTEGER NOT NULL DEFAULT 1,
    payment_method TEXT DEFAULT 'SIMULATED',
    payment_status INTEGER NOT NULL DEFAULT 0,
    payment_time TEXT,
    refund_time TEXT,
    refund_amount DECIMAL(10,2),
    refund_reason TEXT,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_no ON payment(payment_no);
CREATE INDEX IF NOT EXISTS idx_payment_order_id ON payment(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_user_id ON payment(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payment(payment_status);

-- =====================================================
-- 8. 评论表 (comment)
-- =====================================================
CREATE TABLE IF NOT EXISTS comment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    drone_id INTEGER NOT NULL,
    order_id INTEGER,
    content TEXT NOT NULL,
    rating INTEGER DEFAULT 5,
    images TEXT,
    status INTEGER NOT NULL DEFAULT 1,
    parent_id INTEGER,
    reply_content TEXT,
    reply_time TEXT,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_comment_user_id ON comment(user_id);
CREATE INDEX IF NOT EXISTS idx_comment_drone_id ON comment(drone_id);
CREATE INDEX IF NOT EXISTS idx_comment_order_id ON comment(order_id);
CREATE INDEX IF NOT EXISTS idx_comment_status ON comment(status);

-- =====================================================
-- 9. 故障上报表 (fault_report)
-- =====================================================
CREATE TABLE IF NOT EXISTS fault_report (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    report_no TEXT NOT NULL,
    user_id INTEGER NOT NULL,
    drone_id INTEGER NOT NULL,
    order_id INTEGER,
    fault_type TEXT,
    fault_description TEXT NOT NULL,
    fault_images TEXT,
    fault_time TEXT,
    audit_status INTEGER NOT NULL DEFAULT 0,
    audit_remark TEXT,
    audit_time TEXT,
    auditor_id INTEGER,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_fr_report_no ON fault_report(report_no);
CREATE INDEX IF NOT EXISTS idx_fr_user_id ON fault_report(user_id);
CREATE INDEX IF NOT EXISTS idx_fr_drone_id ON fault_report(drone_id);
CREATE INDEX IF NOT EXISTS idx_fr_order_id ON fault_report(order_id);
CREATE INDEX IF NOT EXISTS idx_fr_audit_status ON fault_report(audit_status);

-- =====================================================
-- 10. 维修工单表 (maintenance_ticket)
-- =====================================================
CREATE TABLE IF NOT EXISTS maintenance_ticket (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ticket_no TEXT NOT NULL,
    fault_report_id INTEGER NOT NULL,
    drone_id INTEGER NOT NULL,
    user_id INTEGER,
    maintenance_type TEXT,
    maintenance_description TEXT,
    status INTEGER NOT NULL DEFAULT 0,
    estimated_cost DECIMAL(10,2),
    actual_cost DECIMAL(10,2),
    estimated_days INTEGER,
    actual_days INTEGER,
    start_time TEXT,
    complete_time TEXT,
    progress_notes TEXT,
    assignee_name TEXT,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mt_ticket_no ON maintenance_ticket(ticket_no);
CREATE INDEX IF NOT EXISTS idx_mt_fault_report_id ON maintenance_ticket(fault_report_id);
CREATE INDEX IF NOT EXISTS idx_mt_drone_id ON maintenance_ticket(drone_id);
CREATE INDEX IF NOT EXISTS idx_mt_status ON maintenance_ticket(status);

-- =====================================================
-- 11. 诚信记录表 (credit_record)
-- =====================================================
CREATE TABLE IF NOT EXISTS credit_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    change_type INTEGER NOT NULL,
    before_status INTEGER NOT NULL,
    after_status INTEGER NOT NULL,
    reason TEXT,
    order_id INTEGER,
    operator_id INTEGER,
    operator_name TEXT,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark TEXT
);
CREATE INDEX IF NOT EXISTS idx_cr_user_id ON credit_record(user_id);
CREATE INDEX IF NOT EXISTS idx_cr_change_type ON credit_record(change_type);
CREATE INDEX IF NOT EXISTS idx_cr_created_time ON credit_record(created_time);

-- =====================================================
-- 12. 消息通知表 (notification)
-- =====================================================
CREATE TABLE IF NOT EXISTS notification (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    type INTEGER NOT NULL,
    title TEXT NOT NULL,
    content TEXT,
    business_id INTEGER,
    read_status INTEGER NOT NULL DEFAULT 0,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_nf_user_id ON notification(user_id);
CREATE INDEX IF NOT EXISTS idx_nf_type ON notification(type);
CREATE INDEX IF NOT EXISTS idx_nf_read_status ON notification(read_status);
CREATE INDEX IF NOT EXISTS idx_nf_created_time ON notification(created_time);

-- =====================================================
-- 13. AI 配置表 (ai_config)
-- =====================================================
CREATE TABLE IF NOT EXISTS ai_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key TEXT NOT NULL,
    config_value TEXT,
    description TEXT,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_config_key ON ai_config(config_key);

-- =====================================================
-- 14. AI 聊天消息表 (ai_chat_message)
-- =====================================================
CREATE TABLE IF NOT EXISTS ai_chat_message (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    session_id TEXT,
    role TEXT NOT NULL,
    content TEXT,
    token_count INTEGER DEFAULT 0,
    model TEXT,
    created_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_acm_user_id ON ai_chat_message(user_id);
CREATE INDEX IF NOT EXISTS idx_acm_session_id ON ai_chat_message(session_id);
CREATE INDEX IF NOT EXISTS idx_acm_created_time ON ai_chat_message(created_time);

-- =====================================================
-- 初始数据（仅当表为空时插入，避免重复插入报错）
-- =====================================================

-- 插入默认管理员账号 (密码: admin123)
INSERT INTO user (username, password, nickname, phone, email, address, role, status, credit_status, balance)
SELECT 'admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', '13800000000', 'admin@example.com', '北京市海淀区', 1, 1, 1, 0.00
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'admin');

-- 插入测试用户 (密码: 123456)
INSERT INTO user (username, password, nickname, phone, email, address, role, status, credit_status, balance)
SELECT 'testuser', 'e10adc3949ba59abbe56e057f20f883e', '测试用户', '13900000001', 'test@example.com', '北京市朝阳区', 0, 1, 1, 1000.00
WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'testuser');

-- 插入示例无人机数据
INSERT INTO drone (model, brand, type, description, image, price_per_day, stock, flight_time, max_payload, max_speed, max_range, status, on_shelf)
SELECT * FROM (VALUES
('DJI Mavic 3', 'DJI大疆', '航拍', '专业航拍无人机，4/3 CMOS哈苏相机，46分钟续航', '/uploads/mavic3_drone.png', 299.00, 10, 46, 0.9, 75.0, 30.0, 1, 1),
('DJI Mini 3 Pro', 'DJI大疆', '航拍', '轻便型航拍无人机，249g起飞重量，适合新手', '/uploads/mini3pro_drone.png', 149.00, 15, 34, 0.25, 58.0, 18.0, 1, 1),
('DJI Air 2S', 'DJI大疆', '航拍', '一英寸传感器，5.4K视频，智能跟随', '/uploads/air2s_drone.png', 199.00, 8, 31, 0.6, 68.0, 18.5, 1, 1),
('DJI Inspire 2', 'DJI大疆', '航拍', '专业影视航拍平台，可换镜头，双操控', '/uploads/inspire2_drone.png', 599.00, 5, 27, 1.4, 94.0, 15.0, 1, 1),
('DJI Phantom 4 Pro V2.0', 'DJI大疆', '测绘', '经典航拍无人机，一英寸传感器', '/uploads/phantom4pro_drone.png', 249.00, 6, 30, 0.5, 72.0, 15.0, 1, 1),
('DJI Agras T40', 'DJI大疆', '农业', '农业植保无人机，40kg喷洒载荷', '/uploads/agrast40_drone.png', 899.00, 3, 25, 50.0, 45.0, 10.0, 1, 1)
) WHERE NOT EXISTS (SELECT 1 FROM drone);

-- 插入示例资质数据
INSERT INTO user_qualification (user_id, certificate_no, certificate_type, certificate_image, valid_start_date, valid_end_date, audit_status, audit_remark, auditor_id)
SELECT 2, 'UAV-2024-001', '轻小型民用无人机驾驶员', '/uploads/cert_1.png', '2024-01-01', '2026-12-31', 1, '资质审核通过', 1
WHERE NOT EXISTS (SELECT 1 FROM user_qualification);

-- 插入示例空域备案数据
INSERT INTO airspace_record (user_id, region_name, region_address, longitude, latitude, radius, max_altitude, purpose, audit_status)
SELECT * FROM (VALUES
(2, '北京奥林匹克公园', '北京市朝阳区奥林匹克公园', 116.391244, 39.992552, 500, 120, '航拍摄影', 1),
(2, '上海外滩', '上海市黄浦区外滩', 121.490317, 31.240018, 300, 100, '城市风光拍摄', 0)
) WHERE NOT EXISTS (SELECT 1 FROM airspace_record);

-- 插入示例订单
INSERT INTO rental_order (order_no, user_id, drone_id, rental_start_time, rental_end_time, rental_days, unit_price, total_amount, deposit_amount, delivery_address, order_status, payment_method, pay_time, ship_time, receive_time)
SELECT 'ORDER202406010001', 2, 1, '2024-06-10 09:00:00', '2024-06-12 09:00:00', 2, 299.00, 598.00, 2000.00, '北京市朝阳区XX路XX号', 3, 1, '2024-06-01 10:00:00', '2024-06-10 09:30:00', '2024-06-10 14:00:00'
WHERE NOT EXISTS (SELECT 1 FROM rental_order);

-- 插入示例评论
INSERT INTO comment (user_id, drone_id, order_id, content, rating, status)
SELECT * FROM (VALUES
(2, 1, 1, '设备很新，续航表现符合描述，客服态度也很好！', 5, 1),
(2, 2, NULL, '小巧便携，画质不错，适合家庭旅行使用。', 4, 1)
) WHERE NOT EXISTS (SELECT 1 FROM comment);

-- 插入示例通知
INSERT INTO notification (user_id, type, title, content, business_id, read_status)
SELECT * FROM (VALUES
(2, 3, '欢迎使用无人机租赁系统', '感谢您注册使用我们的服务，如有问题请联系客服。', NULL, 1),
(2, 1, '订单状态更新', '您的订单已发货，请注意查收。', 1, 0)
) WHERE NOT EXISTS (SELECT 1 FROM notification);

-- 插入示例AI配置
INSERT INTO ai_config (config_key, config_value, description)
SELECT * FROM (VALUES
('ai.enabled', 'true', '是否启用AI功能'),
('ai.model', 'qwen-turbo', '使用的AI模型')
) WHERE NOT EXISTS (SELECT 1 FROM ai_config);
