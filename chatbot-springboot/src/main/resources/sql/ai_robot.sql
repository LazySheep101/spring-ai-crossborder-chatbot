-- 小智AI 核心元数据表（PostgreSQL + pgvector，幂等）

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

-- Spring AI PgVector 表；若不存在则创建（已存在时跳过）
CREATE TABLE IF NOT EXISTS t_vector_store
(
    id        uuid DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    content   text,
    metadata  json,
    embedding vector(1536)
);
CREATE INDEX IF NOT EXISTS t_vector_store_embedding_idx
    ON t_vector_store USING hnsw (embedding vector_cosine_ops);
