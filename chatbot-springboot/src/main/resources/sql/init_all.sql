-- =============================================================================
-- 小智 AI（spring-ai-crossborder-chatbot）一键初始化脚本
-- 数据库：PostgreSQL + pgvector | 目标库：robot
-- 组成：ai_robot.sql + commerce_agent.sql + seed_demo_data.sql（幂等）
-- 用法示例：
--   docker exec -i PostgreSQL psql -U postgres -d robot < init_all.sql
-- 说明：应用启动仍按拆分文件自动建表；本文件供空库手工一键初始化 / DataGrip 执行
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Part 1 / 3 · 核心元数据（原 ai_robot.sql）
-- ---------------------------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS t_ai_customer_service_file_storage
(
    id              bigserial PRIMARY KEY,
    file_name       varchar(160)                        NOT NULL,
    file_md5        varchar(32)                         NOT NULL,
    file_path       varchar(500)                        NOT NULL,
    file_size       bigint                              NOT NULL,
    total_chunks    integer                             NOT NULL,
    uploaded_chunks integer   DEFAULT 0                 NOT NULL,
    status          smallint  DEFAULT 0,
    remark          varchar(200),
    create_time     timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time     timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_file_md5 ON t_ai_customer_service_file_storage (file_md5);
CREATE INDEX IF NOT EXISTS idx_file_storage_status ON t_ai_customer_service_file_storage (status);
CREATE INDEX IF NOT EXISTS idx_file_storage_create_time ON t_ai_customer_service_file_storage (create_time);
CREATE INDEX IF NOT EXISTS idx_file_storage_file_name ON t_ai_customer_service_file_storage (file_name);

CREATE TABLE IF NOT EXISTS t_ai_customer_service_md_storage
(
    id                 bigserial PRIMARY KEY,
    original_file_name varchar(160) NOT NULL,
    new_file_name      varchar(160) NOT NULL,
    file_path          varchar(500) NOT NULL,
    file_size          bigint       NOT NULL,
    status             smallint  DEFAULT 0,
    remark             varchar(200),
    create_time        timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time        timestamp DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_md_storage_status ON t_ai_customer_service_md_storage (status);
CREATE INDEX IF NOT EXISTS idx_md_storage_create_time ON t_ai_customer_service_md_storage (create_time);
CREATE INDEX IF NOT EXISTS idx_md_storage_original_file_name ON t_ai_customer_service_md_storage (original_file_name);

CREATE TABLE IF NOT EXISTS t_chat
(
    id          bigserial PRIMARY KEY,
    uuid        varchar(60)                         NOT NULL UNIQUE,
    summary     varchar(60),
    create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_chat_update_time ON t_chat (update_time);

CREATE TABLE IF NOT EXISTS t_chat_message
(
    id                bigserial PRIMARY KEY,
    chat_uuid         varchar(60)                         NOT NULL,
    content           text                                NOT NULL,
    role              varchar(12),
    create_time       timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    reasoning_content text
);
CREATE INDEX IF NOT EXISTS idx_chat_message_create_time ON t_chat_message (create_time);
CREATE INDEX IF NOT EXISTS idx_chat_message_uuid ON t_chat_message (chat_uuid);

CREATE TABLE IF NOT EXISTS t_file_chunk_info
(
    id           bigserial PRIMARY KEY,
    file_md5     varchar(32)                         NOT NULL,
    chunk_number integer                             NOT NULL,
    chunk_path   varchar(500)                        NOT NULL,
    chunk_size   bigint                              NOT NULL,
    create_time  timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_file_chunk_md5 ON t_file_chunk_info (file_md5);

CREATE TABLE IF NOT EXISTS t_vector_store
(
    id        uuid DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    content   text,
    metadata  json,
    embedding vector(1536)
);
CREATE INDEX IF NOT EXISTS t_vector_store_embedding_idx
    ON t_vector_store USING hnsw (embedding vector_cosine_ops);

-- ---------------------------------------------------------------------------
-- Part 2 / 3 · 电商 Agent 业务表（原 commerce_agent.sql）
-- ---------------------------------------------------------------------------

create table if not exists t_customer
(
    id          bigserial primary key,
    name        varchar(64)  not null,
    email       varchar(128) not null,
    phone       varchar(32),
    create_time timestamp default CURRENT_TIMESTAMP not null,
    update_time timestamp default CURRENT_TIMESTAMP not null
);
create unique index if not exists uk_customer_email on t_customer (email);

create table if not exists t_product
(
    id          bigserial primary key,
    sku         varchar(64)  not null,
    name        varchar(160) not null,
    description varchar(500),
    price       numeric(12, 2) not null,
    stock       integer      not null default 0,
    category    varchar(64),
    create_time timestamp default CURRENT_TIMESTAMP not null,
    update_time timestamp default CURRENT_TIMESTAMP not null
);
create unique index if not exists uk_product_sku on t_product (sku);

create table if not exists t_order
(
    id               bigserial primary key,
    order_no         varchar(64)  not null,
    customer_id      bigint       not null,
    status           varchar(32)  not null,
    total_amount     numeric(12, 2) not null,
    currency         varchar(8)   not null default 'CNY',
    shipping_address varchar(255),
    create_time      timestamp default CURRENT_TIMESTAMP not null,
    update_time      timestamp default CURRENT_TIMESTAMP not null
);
create unique index if not exists uk_order_no on t_order (order_no);
create index if not exists idx_order_customer on t_order (customer_id);

create table if not exists t_order_item
(
    id          bigserial primary key,
    order_id    bigint         not null,
    product_id  bigint         not null,
    sku         varchar(64)    not null,
    product_name varchar(160)  not null,
    quantity    integer        not null,
    unit_price  numeric(12, 2) not null
);
create index if not exists idx_order_item_order on t_order_item (order_id);

create table if not exists t_shipment
(
    id               bigserial primary key,
    order_id         bigint       not null,
    tracking_number  varchar(64)  not null,
    carrier          varchar(64)  not null,
    status           varchar(32)  not null,
    latest_event     varchar(255),
    create_time      timestamp default CURRENT_TIMESTAMP not null,
    update_time      timestamp default CURRENT_TIMESTAMP not null
);
create index if not exists idx_shipment_order on t_shipment (order_id);

create table if not exists t_commerce_action_request
(
    id              bigserial primary key,
    action_type     varchar(32)  not null,
    order_no        varchar(64)  not null,
    customer_contact varchar(128) not null,
    reason          varchar(500),
    payload         text,
    status          varchar(32)  not null default 'PENDING',
    reviewer        varchar(64),
    review_note     varchar(500),
    create_time     timestamp default CURRENT_TIMESTAMP not null,
    update_time     timestamp default CURRENT_TIMESTAMP not null
);
create index if not exists idx_action_status on t_commerce_action_request (status);

create table if not exists t_tool_call_log
(
    id            bigserial primary key,
    conversation_id varchar(64),
    tool_name     varchar(64)  not null,
    args_summary  varchar(1000),
    result_summary varchar(2000),
    success       boolean      not null default true,
    latency_ms    bigint,
    create_time   timestamp default CURRENT_TIMESTAMP not null
);
create index if not exists idx_tool_log_time on t_tool_call_log (create_time desc);

create table if not exists t_ticket
(
    id              bigserial primary key,
    ticket_no       varchar(64)  not null,
    chat_id         varchar(64),
    customer_contact varchar(128),
    order_no        varchar(64),
    subject         varchar(200) not null,
    summary         text,
    status          varchar(32)  not null default 'AI_WORKING',
    assignee        varchar(64),
    internal_note   text,
    create_time     timestamp default CURRENT_TIMESTAMP not null,
    update_time     timestamp default CURRENT_TIMESTAMP not null
);
create unique index if not exists uk_ticket_no on t_ticket (ticket_no);
create index if not exists idx_ticket_status on t_ticket (status);
create index if not exists idx_ticket_chat on t_ticket (chat_id);

alter table t_ticket add column if not exists chat_id varchar(64);

create table if not exists t_agent_run
(
    id               bigserial primary key,
    run_id           varchar(64) not null,
    conversation_id  varchar(64),
    user_message     varchar(1000),
    specialist       varchar(64),
    source_lang      varchar(16),
    status           varchar(32) not null default 'RUNNING',
    create_time      timestamp default CURRENT_TIMESTAMP not null,
    update_time      timestamp default CURRENT_TIMESTAMP not null
);
create unique index if not exists uk_agent_run_id on t_agent_run (run_id);

create table if not exists t_agent_step
(
    id          bigserial primary key,
    run_id      varchar(64) not null,
    step_type   varchar(32) not null,
    name        varchar(128),
    detail      varchar(2000),
    error       varchar(500),
    success     boolean default true,
    latency_ms  bigint,
    create_time timestamp default CURRENT_TIMESTAMP not null
);
create index if not exists idx_agent_step_run on t_agent_step (run_id);

create table if not exists t_cs_message
(
    id          bigserial primary key,
    chat_id     varchar(64) not null,
    role        varchar(16) not null,
    content     text not null,
    meta_json   text,
    create_time timestamp default CURRENT_TIMESTAMP not null
);
create index if not exists idx_cs_message_chat on t_cs_message (chat_id);

create table if not exists t_conversation_state
(
    id          bigserial primary key,
    chat_id     varchar(64) not null,
    status      varchar(32) not null default 'AI_WORKING',
    create_time timestamp default CURRENT_TIMESTAMP not null,
    update_time timestamp default CURRENT_TIMESTAMP not null
);
create unique index if not exists uk_conversation_chat on t_conversation_state (chat_id);

create table if not exists t_admin_user
(
    id          bigserial primary key,
    username    varchar(64)  not null,
    password    varchar(128) not null,
    display_name varchar(64) not null,
    role        varchar(32)  not null,
    create_time timestamp default CURRENT_TIMESTAMP not null
);
create unique index if not exists uk_admin_username on t_admin_user (username);

create table if not exists t_lang_event
(
    id              bigserial primary key,
    conversation_id varchar(64),
    source_lang     varchar(16),
    target_lang     varchar(16),
    tool_name       varchar(64),
    fallback        boolean default false,
    reason          varchar(128),
    snippet         varchar(500),
    create_time     timestamp default CURRENT_TIMESTAMP not null
);
create index if not exists idx_lang_event_time on t_lang_event (create_time);

-- ---------------------------------------------------------------------------
-- Part 3 / 3 · 演示种子数据（原 seed_demo_data.sql）
-- ---------------------------------------------------------------------------

INSERT INTO t_admin_user (username, password, display_name, role, create_time)
VALUES
    ('admin', '123456', '管理员', 'supervisor', CURRENT_TIMESTAMP),
    ('agent', 'agent123', '小智AI 客服', 'agent', CURRENT_TIMESTAMP),
    ('supervisor', 'super123', '客服主管', 'supervisor', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO t_customer (name, email, phone, create_time, update_time)
VALUES
    ('张三', 'zhangsan@example.com', '13800001111', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('李四', 'lisi@example.com', '13900002222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('王五', 'wangwu@example.com', '13700003333', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Alice Chen', 'alice@example.com', '13600004444', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO t_product (sku, name, description, price, stock, category, create_time, update_time)
VALUES
    ('XZ-SOCK-01', '小智AI 联名袜', '透气棉袜，最终售出商品不支持无理由退货', 39.90, 200, '服饰', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('XZ-HOODIE-02', '小智AI 连帽卫衣', '加绒卫衣，支持签收后 7 天无理由退货', 199.00, 80, '服饰', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('XZ-CUP-03', '小智AI 保温杯', '不锈钢保温杯 500ml', 89.00, 120, '周边', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('XZ-BAG-04', '小智AI 托特包', '大容量帆布包，跨境直邮', 129.00, 60, '周边', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('XZ-CHARGER-05', '小智AI 快充头', '30W USB-C 快充，质保一年', 79.00, 150, '数码', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

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

INSERT INTO t_commerce_action_request (action_type, order_no, customer_contact, reason, payload, status, create_time, update_time)
SELECT 'RETURN', 'ORD-10001', 'zhangsan@example.com', '尺码不合适，申请退货', '联名袜+卫衣', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_commerce_action_request WHERE order_no = 'ORD-10001' AND action_type = 'RETURN');

INSERT INTO t_commerce_action_request (action_type, order_no, customer_contact, reason, payload, status, create_time, update_time)
SELECT 'ADDRESS_CHANGE', 'ORD-10003', '13800001111', '改寄公司地址', '上海市静安区南京西路 100 号', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_commerce_action_request WHERE order_no = 'ORD-10003' AND action_type = 'ADDRESS_CHANGE');

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

INSERT INTO t_ticket (ticket_no, chat_id, customer_contact, order_no, subject, summary, status, assignee, create_time, update_time)
SELECT 'TKT-10001', 'cs-demo-002', 'lisi@example.com', 'ORD-10002',
       '物流长时间未更新', '客户反馈运单 YT9876543210 停滞，请求人工跟进',
       'HUMAN_PENDING', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_ticket WHERE ticket_no = 'TKT-10001');

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
