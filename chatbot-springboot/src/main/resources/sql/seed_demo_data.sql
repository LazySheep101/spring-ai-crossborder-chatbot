-- 小智AI 演示/测试数据（幂等：按唯一键冲突跳过）

-- 后台账号（明文，应用启动时会升级为 BCrypt）
INSERT INTO t_admin_user (username, password, display_name, role, create_time)
VALUES
    ('admin', '123456', '管理员', 'supervisor', CURRENT_TIMESTAMP),
    ('agent', 'agent123', '小智AI 客服', 'agent', CURRENT_TIMESTAMP),
    ('supervisor', 'super123', '客服主管', 'supervisor', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 客户
INSERT INTO t_customer (name, email, phone, create_time, update_time)
VALUES
    ('张三', 'zhangsan@example.com', '13800001111', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('李四', 'lisi@example.com', '13900002222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('王五', 'wangwu@example.com', '13700003333', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Alice Chen', 'alice@example.com', '13600004444', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 商品
INSERT INTO t_product (sku, name, description, price, stock, category, create_time, update_time)
VALUES
    ('XZ-SOCK-01', '小智AI 联名袜', '透气棉袜，最终售出商品不支持无理由退货', 39.90, 200, '服饰', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('XZ-HOODIE-02', '小智AI 连帽卫衣', '加绒卫衣，支持签收后 7 天无理由退货', 199.00, 80, '服饰', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('XZ-CUP-03', '小智AI 保温杯', '不锈钢保温杯 500ml', 89.00, 120, '周边', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('XZ-BAG-04', '小智AI 托特包', '大容量帆布包，跨境直邮', 129.00, 60, '周边', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('XZ-CHARGER-05', '小智AI 快充头', '30W USB-C 快充，质保一年', 79.00, 150, '数码', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 订单（依赖客户 id）
INSERT INTO t_order (order_no, customer_id, status, total_amount, currency, shipping_address, create_time, update_time)
SELECT 'ORD-10001', c.id, 'DELIVERED', 238.90, 'CNY', '上海市浦东新区张江路 88 号',
       CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP
FROM t_customer c WHERE c.email = 'zhangsan@example.com'
ON CONFLICT DO NOTHING;

INSERT INTO t_order (order_no, customer_id, status, total_amount, currency, shipping_address, create_time, update_time)
SELECT 'ORD-10002', c.id, 'SHIPPED', 89.00, 'CNY', '杭州市西湖区文三路 100 号',
       CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP
FROM t_customer c WHERE c.email = 'lisi@example.com'
ON CONFLICT DO NOTHING;

INSERT INTO t_order (order_no, customer_id, status, total_amount, currency, shipping_address, create_time, update_time)
SELECT 'ORD-10003', c.id, 'PAID', 199.00, 'CNY', '上海市浦东新区张江路 88 号',
       CURRENT_TIMESTAMP - INTERVAL '6 hours', CURRENT_TIMESTAMP
FROM t_customer c WHERE c.email = 'zhangsan@example.com'
ON CONFLICT DO NOTHING;

INSERT INTO t_order (order_no, customer_id, status, total_amount, currency, shipping_address, create_time, update_time)
SELECT 'ORD-10004', c.id, 'SHIPPED', 208.00, 'CNY', '深圳市南山区科技园南路 1 号',
       CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP
FROM t_customer c WHERE c.email = 'wangwu@example.com'
ON CONFLICT DO NOTHING;

INSERT INTO t_order (order_no, customer_id, status, total_amount, currency, shipping_address, create_time, update_time)
SELECT 'ORD-10005', c.id, 'DELIVERED', 79.00, 'USD', '221B Baker St, London',
       CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP
FROM t_customer c WHERE c.email = 'alice@example.com'
ON CONFLICT DO NOTHING;

-- 订单明细（仅在空表时灌入，避免重复）
INSERT INTO t_order_item (order_id, product_id, sku, product_name, quantity, unit_price)
SELECT o.id, p.id, p.sku, p.name, 1, p.price
FROM t_order o
JOIN t_product p ON p.sku = 'XZ-SOCK-01'
WHERE o.order_no = 'ORD-10001'
  AND NOT EXISTS (SELECT 1 FROM t_order_item oi WHERE oi.order_id = o.id AND oi.sku = p.sku);

INSERT INTO t_order_item (order_id, product_id, sku, product_name, quantity, unit_price)
SELECT o.id, p.id, p.sku, p.name, 1, p.price
FROM t_order o
JOIN t_product p ON p.sku = 'XZ-HOODIE-02'
WHERE o.order_no = 'ORD-10001'
  AND NOT EXISTS (SELECT 1 FROM t_order_item oi WHERE oi.order_id = o.id AND oi.sku = p.sku);

INSERT INTO t_order_item (order_id, product_id, sku, product_name, quantity, unit_price)
SELECT o.id, p.id, p.sku, p.name, 1, p.price
FROM t_order o
JOIN t_product p ON p.sku = 'XZ-CUP-03'
WHERE o.order_no = 'ORD-10002'
  AND NOT EXISTS (SELECT 1 FROM t_order_item oi WHERE oi.order_id = o.id);

INSERT INTO t_order_item (order_id, product_id, sku, product_name, quantity, unit_price)
SELECT o.id, p.id, p.sku, p.name, 1, p.price
FROM t_order o
JOIN t_product p ON p.sku = 'XZ-HOODIE-02'
WHERE o.order_no = 'ORD-10003'
  AND NOT EXISTS (SELECT 1 FROM t_order_item oi WHERE oi.order_id = o.id);

INSERT INTO t_order_item (order_id, product_id, sku, product_name, quantity, unit_price)
SELECT o.id, p.id, p.sku, p.name, 1, p.price
FROM t_order o
JOIN t_product p ON p.sku = 'XZ-BAG-04'
WHERE o.order_no = 'ORD-10004'
  AND NOT EXISTS (SELECT 1 FROM t_order_item oi WHERE oi.order_id = o.id AND oi.sku = 'XZ-BAG-04');

INSERT INTO t_order_item (order_id, product_id, sku, product_name, quantity, unit_price)
SELECT o.id, p.id, p.sku, p.name, 1, p.price
FROM t_order o
JOIN t_product p ON p.sku = 'XZ-CHARGER-05'
WHERE o.order_no IN ('ORD-10004', 'ORD-10005')
  AND NOT EXISTS (SELECT 1 FROM t_order_item oi WHERE oi.order_id = o.id AND oi.sku = p.sku);

-- 物流
INSERT INTO t_shipment (order_id, tracking_number, carrier, status, latest_event, create_time, update_time)
SELECT o.id, 'SF1234567890', '顺丰', 'DELIVERED', '已签收', CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP
FROM t_order o WHERE o.order_no = 'ORD-10001'
  AND NOT EXISTS (SELECT 1 FROM t_shipment s WHERE s.order_id = o.id);

INSERT INTO t_shipment (order_id, tracking_number, carrier, status, latest_event, create_time, update_time)
SELECT o.id, 'YT9876543210', '圆通', 'IN_TRANSIT', '运输中：到达杭州转运中心', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM t_order o WHERE o.order_no = 'ORD-10002'
  AND NOT EXISTS (SELECT 1 FROM t_shipment s WHERE s.order_id = o.id);

INSERT INTO t_shipment (order_id, tracking_number, carrier, status, latest_event, create_time, update_time)
SELECT o.id, 'ZT5556667778', '中通', 'IN_TRANSIT', '已发往深圳南山营业部', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM t_order o WHERE o.order_no = 'ORD-10004'
  AND NOT EXISTS (SELECT 1 FROM t_shipment s WHERE s.order_id = o.id);

INSERT INTO t_shipment (order_id, tracking_number, carrier, status, latest_event, create_time, update_time)
SELECT o.id, 'DHL99887766', 'DHL', 'DELIVERED', 'Delivered to recipient', CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP
FROM t_order o WHERE o.order_no = 'ORD-10005'
  AND NOT EXISTS (SELECT 1 FROM t_shipment s WHERE s.order_id = o.id);

-- 待审批动作
INSERT INTO t_commerce_action_request (action_type, order_no, customer_contact, reason, payload, status, create_time, update_time)
SELECT 'RETURN', 'ORD-10001', 'zhangsan@example.com', '尺码不合适，申请退货', '联名袜+卫衣', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_commerce_action_request WHERE order_no = 'ORD-10001' AND action_type = 'RETURN');

INSERT INTO t_commerce_action_request (action_type, order_no, customer_contact, reason, payload, status, create_time, update_time)
SELECT 'ADDRESS_CHANGE', 'ORD-10003', '13800001111', '改寄公司地址', '上海市静安区南京西路 100 号', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_commerce_action_request WHERE order_no = 'ORD-10003' AND action_type = 'ADDRESS_CHANGE');

-- 对话历史（通用聊天）
INSERT INTO t_chat (uuid, summary, create_time, update_time)
VALUES
    ('demo-chat-001', '咨询 ORD-10001 物流', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP),
    ('demo-chat-002', '退货政策问答', CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO t_chat_message (chat_uuid, content, role, create_time)
SELECT 'demo-chat-001', '我的订单 ORD-10001 到哪了？邮箱 zhangsan@example.com', 'user', CURRENT_TIMESTAMP - INTERVAL '2 days'
WHERE NOT EXISTS (SELECT 1 FROM t_chat_message WHERE chat_uuid = 'demo-chat-001' AND role = 'user');

INSERT INTO t_chat_message (chat_uuid, content, role, create_time)
SELECT 'demo-chat-001', '您的订单已签收，运单号 SF1234567890（顺丰）。', 'assistant', CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '1 minute'
WHERE NOT EXISTS (SELECT 1 FROM t_chat_message WHERE chat_uuid = 'demo-chat-001' AND role = 'assistant');

-- 客服会话消息 + 状态
INSERT INTO t_conversation_state (chat_id, status, create_time, update_time)
VALUES
    ('cs-demo-001', 'AI_WORKING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('cs-demo-002', 'HUMAN_PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO t_cs_message (chat_id, role, content, meta_json, create_time)
SELECT 'cs-demo-001', 'user', '卫衣支持七天无理由退货吗？', NULL, CURRENT_TIMESTAMP - INTERVAL '30 minutes'
WHERE NOT EXISTS (SELECT 1 FROM t_cs_message WHERE chat_id = 'cs-demo-001');

INSERT INTO t_cs_message (chat_id, role, content, meta_json, create_time)
SELECT 'cs-demo-001', 'assistant', '小智AI 连帽卫衣支持签收后 7 天无理由退货，需保持吊牌完整。', NULL, CURRENT_TIMESTAMP - INTERVAL '29 minutes'
WHERE (SELECT COUNT(*) FROM t_cs_message WHERE chat_id = 'cs-demo-001') = 1;

INSERT INTO t_cs_message (chat_id, role, content, meta_json, create_time)
SELECT 'cs-demo-002', 'user', '我要转人工，订单 ORD-10002 一直没更新物流', NULL, CURRENT_TIMESTAMP - INTERVAL '10 minutes'
WHERE NOT EXISTS (SELECT 1 FROM t_cs_message WHERE chat_id = 'cs-demo-002');

-- 工单
INSERT INTO t_ticket (ticket_no, chat_id, customer_contact, order_no, subject, summary, status, assignee, create_time, update_time)
SELECT 'TKT-10001', 'cs-demo-002', 'lisi@example.com', 'ORD-10002',
       '物流长时间未更新', '客户反馈运单 YT9876543210 停滞，请求人工跟进',
       'HUMAN_PENDING', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_ticket WHERE ticket_no = 'TKT-10001');

-- Trace 样例
INSERT INTO t_agent_run (run_id, conversation_id, user_message, specialist, source_lang, status, create_time, update_time)
SELECT 'run-demo-001', 'cs-demo-001', '卫衣支持七天无理由退货吗？', 'policy_rag', 'zh', 'DONE',
       CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '29 minutes'
WHERE NOT EXISTS (SELECT 1 FROM t_agent_run WHERE run_id = 'run-demo-001');

INSERT INTO t_agent_step (run_id, step_type, name, detail, success, latency_ms, create_time)
SELECT 'run-demo-001', 'triage', 'policy_rag', '分流到政策 RAG', true, 5, CURRENT_TIMESTAMP - INTERVAL '30 minutes'
WHERE NOT EXISTS (SELECT 1 FROM t_agent_step WHERE run_id = 'run-demo-001' AND step_type = 'triage');

INSERT INTO t_agent_step (run_id, step_type, name, detail, success, latency_ms, create_time)
SELECT 'run-demo-001', 'tool', 'refundPolicyRAG', '召回退货政策片段', true, 320, CURRENT_TIMESTAMP - INTERVAL '29 minutes' - INTERVAL '20 seconds'
WHERE NOT EXISTS (SELECT 1 FROM t_agent_step WHERE run_id = 'run-demo-001' AND step_type = 'tool');

INSERT INTO t_tool_call_log (conversation_id, tool_name, args_summary, result_summary, success, latency_ms, create_time)
SELECT 'cs-demo-001', 'refundPolicyRAG', 'question=卫衣七天无理由', '命中卫衣退货政策', true, 320, CURRENT_TIMESTAMP - INTERVAL '29 minutes'
WHERE NOT EXISTS (SELECT 1 FROM t_tool_call_log WHERE conversation_id = 'cs-demo-001' AND tool_name = 'refundPolicyRAG');
