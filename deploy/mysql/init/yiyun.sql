CREATE DATABASE IF NOT EXISTS yiyun
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE yiyun;

-- 禁用外键检查，避免DROP/INSERT顺序问题
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码密文',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    KEY idx_status (status)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='系统用户表';

DROP TABLE IF EXISTS heritage_category;

CREATE TABLE heritage_category (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '分类描述',
    sort_num INT NOT NULL DEFAULT 0 COMMENT '排序值',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name),
    KEY idx_status_sort (status, sort_num)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='非遗分类表';

DROP TABLE IF EXISTS heritage_project;

CREATE TABLE heritage_project (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '非遗项目ID',
    name VARCHAR(200) NOT NULL COMMENT '非遗项目名称',
    category_id BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
    region VARCHAR(200) DEFAULT NULL COMMENT '地域',

    description TEXT COMMENT '项目简介',
    history TEXT COMMENT '历史背景',
    cultural_meaning TEXT COMMENT '文化内涵',
    craft TEXT COMMENT '工艺与表现形式',

    cover_url VARCHAR(500) DEFAULT NULL COMMENT '封面图片地址',

    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0下架 1正常',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_category_id (category_id),
    KEY idx_region (region),
    KEY idx_status (status),

    CONSTRAINT fk_heritage_category
        FOREIGN KEY (category_id)
        REFERENCES heritage_category(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='非遗项目表';

DROP TABLE IF EXISTS knowledge_chunk;

CREATE TABLE knowledge_chunk (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '知识片段ID',
    document_id BIGINT UNSIGNED NOT NULL COMMENT '所属知识文档ID',
    heritage_project_id BIGINT UNSIGNED NOT NULL COMMENT '所属非遗项目ID（冗余字段，便于按项目检索）',

    chunk_index INT NOT NULL COMMENT '片段在原文中的序号',
    chunk_text TEXT NOT NULL COMMENT '片段文本内容',

    embedding_id VARCHAR(128) DEFAULT NULL COMMENT 'FAISS向量索引ID（对应chunk.id）',
    token_count INT DEFAULT NULL COMMENT '片段Token数量',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    KEY idx_document_id (document_id),
    KEY idx_heritage_project_id (heritage_project_id),
    KEY idx_chunk_index (chunk_index),

    CONSTRAINT fk_chunk_document
        FOREIGN KEY (document_id)
        REFERENCES knowledge_document(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='非遗RAG知识片段表';

DROP TABLE IF EXISTS knowledge_document;

CREATE TABLE knowledge_document (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '知识文档ID',
    heritage_project_id BIGINT UNSIGNED NOT NULL COMMENT '所属非遗项目ID',

    title VARCHAR(300) NOT NULL COMMENT '知识标题',
    doc_type VARCHAR(50) NOT NULL COMMENT '知识类型',
    content LONGTEXT NOT NULL COMMENT '知识正文',

    source VARCHAR(500) DEFAULT NULL COMMENT '知识来源',
    author VARCHAR(100) DEFAULT NULL COMMENT '作者',

    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_heritage_project_id (heritage_project_id),
    KEY idx_doc_type (doc_type),
    KEY idx_status (status),

    CONSTRAINT fk_knowledge_heritage
        FOREIGN KEY (heritage_project_id)
        REFERENCES heritage_project(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='非遗RAG知识文档表';

DROP TABLE IF EXISTS ai_task;

CREATE TABLE ai_task (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',

    task_type VARCHAR(50) NOT NULL COMMENT '任务类型：CHAT/IMAGE/VIDEO',
    provider VARCHAR(100) DEFAULT NULL COMMENT 'AI服务提供商',
    model VARCHAR(100) DEFAULT NULL COMMENT '模型名称',

    input_text TEXT COMMENT '输入内容',
    prompt TEXT COMMENT '生成Prompt',

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        COMMENT '任务状态：PENDING/RUNNING/SUCCESS/FAILED',

    result_url VARCHAR(500) DEFAULT NULL COMMENT '结果地址',
    error_message TEXT COMMENT '错误信息',

    started_at DATETIME DEFAULT NULL COMMENT '开始时间',
    finished_at DATETIME DEFAULT NULL COMMENT '完成时间',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_task_type (task_type),
    KEY idx_status (status),
    KEY idx_created_at (created_at),

    CONSTRAINT fk_ai_task_user
        FOREIGN KEY (user_id)
        REFERENCES sys_user(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='AI生成任务表';

DROP TABLE IF EXISTS biz_user_work;

CREATE TABLE biz_user_work (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '作品ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',

    work_type VARCHAR(30) NOT NULL COMMENT '作品类型：IMAGE/VIDEO',
    title VARCHAR(200) NOT NULL COMMENT '作品名称',

    prompt TEXT COMMENT '用户原始需求',
    enhanced_prompt LONGTEXT COMMENT 'AI增强后的Prompt',
    style VARCHAR(100) DEFAULT NULL COMMENT '作品风格',

    result_url VARCHAR(500) DEFAULT NULL COMMENT '作品地址',
    thumbnail_url VARCHAR(500) DEFAULT NULL COMMENT '缩略图地址',

    provider VARCHAR(100) DEFAULT NULL COMMENT '生成服务商',
    model VARCHAR(100) DEFAULT NULL COMMENT '生成模型',

    task_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'AI任务ID',

    status VARCHAR(30) NOT NULL DEFAULT 'GENERATING'
        COMMENT '状态：GENERATING/SUCCESS/FAILED/DELETED',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_task_id (task_id),
    KEY idx_work_type (work_type),
    KEY idx_status (status),
    KEY idx_created_at (created_at),

    CONSTRAINT fk_work_user
        FOREIGN KEY (user_id)
        REFERENCES sys_user(id),

    CONSTRAINT fk_work_task
        FOREIGN KEY (task_id)
        REFERENCES ai_task(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='用户AI作品表';

DROP TABLE IF EXISTS biz_work_source;

CREATE TABLE biz_work_source (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '来源ID',

    work_id BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
    heritage_project_id BIGINT UNSIGNED DEFAULT NULL COMMENT '非遗项目ID',

    source_type VARCHAR(50) NOT NULL COMMENT '来源类型',
    source_title VARCHAR(300) DEFAULT NULL COMMENT '来源标题',
    source_content TEXT COMMENT '检索到的知识内容',

    retrieval_score DECIMAL(10,6) DEFAULT NULL COMMENT 'RAG检索得分',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    KEY idx_work_id (work_id),
    KEY idx_heritage_project_id (heritage_project_id),

    CONSTRAINT fk_work_source_work
        FOREIGN KEY (work_id)
        REFERENCES biz_user_work(id),

    CONSTRAINT fk_work_source_heritage
        FOREIGN KEY (heritage_project_id)
        REFERENCES heritage_project(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='AI作品知识来源表';

DROP TABLE IF EXISTS sys_operation_log;

CREATE TABLE sys_operation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT UNSIGNED DEFAULT NULL COMMENT '操作用户ID',

    module VARCHAR(100) DEFAULT NULL COMMENT '操作模块',
    operation VARCHAR(100) DEFAULT NULL COMMENT '操作类型',

    request_method VARCHAR(20) DEFAULT NULL COMMENT '请求方法',
    request_url VARCHAR(500) DEFAULT NULL COMMENT '请求地址',

    request_params TEXT COMMENT '请求参数',
    response_status INT DEFAULT NULL COMMENT '响应状态',

    ip_address VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',

    execution_time BIGINT DEFAULT NULL COMMENT '执行耗时ms',
    error_message TEXT COMMENT '错误信息',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_module (module),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='系统操作日志表';

-- ==================== 初始化数据 ====================

INSERT INTO heritage_category
(name, description, sort_num)
VALUES
('传统美术', '传统美术类非物质文化遗产', 1),
('传统技艺', '传统技艺类非物质文化遗产', 2),
('传统音乐', '传统音乐类非物质文化遗产', 3),
('传统舞蹈', '传统舞蹈类非物质文化遗产', 4),
('传统戏剧', '传统戏剧类非物质文化遗产', 5),
('传统医药', '传统医药类非物质文化遗产', 6),
('民俗', '民俗类非物质文化遗产', 7);

-- 密码明文均为 123456，BCrypt 加密
INSERT INTO sys_user
(username, password, nickname, status)
VALUES
('admin', '$2a$10$JKi3X.4ZEFkswnAIfeUaLuK1rq2NFnuVtnyYiEau7USYa1uZDW.qq', '管理员', 1),
('test', '$2a$10$JKi3X.4ZEFkswnAIfeUaLuK1rq2NFnuVtnyYiEau7USYa1uZDW.qq', '测试用户', 1);

INSERT INTO heritage_project
(
    name,
    category_id,
    region,
    description,
    history,
    cultural_meaning,
    craft,
    cover_url,
    status
)
VALUES
(
    '苗族银饰',
    2,
    '贵州等地区',
    '苗族传统银饰是苗族传统文化的重要组成部分，具有鲜明的民族特色。',
    '介绍苗族银饰的历史发展与传承过程。',
    '苗族银饰具有装饰、社会身份和传统习俗等方面的文化意义。',
    '介绍苗族银饰的制作工艺、材料和表现形式。',
    '/images/heritage/miaozu-silver.jpg',
    1
);

INSERT INTO knowledge_document
(heritage_project_id, title, doc_type, content, source)
VALUES
(1, '苗族银饰文化内涵', '文化内涵',
 '苗族银饰不仅具有装饰作用，还与民族文化、社会身份和传统习俗存在联系。',
 '苗族传统文化资料'),

(1, '苗族银饰制作工艺', '工艺技法',
 '苗族银饰制作涉及熔炼、锻打、錾刻、焊接等传统工艺。',
 '非遗项目资料'),

(1, '苗族银饰相关民俗', '民俗',
 '苗族银饰在部分传统节日和婚俗活动中具有重要作用。',
 '苗族民俗资料');

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;