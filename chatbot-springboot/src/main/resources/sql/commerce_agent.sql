-- 小智AI 电商 Agent 相关表（PostgreSQL）

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

-- 兼容已有库：补 chat_id 列
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
