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

    source VARCHAR(500) DEFAULT NULL COMMENT '知识来源名称',
    source_url VARCHAR(500) DEFAULT NULL COMMENT '知识来源链接',
    author VARCHAR(100) DEFAULT NULL COMMENT '作者',
    publish_date DATE DEFAULT NULL COMMENT '发布日期',
    access_date DATE DEFAULT NULL COMMENT '访问日期',
    page_no VARCHAR(50) DEFAULT NULL COMMENT '页码',
    rights_note VARCHAR(500) DEFAULT NULL COMMENT '版权声明',
    verified TINYINT NOT NULL DEFAULT 0 COMMENT '是否已核验：0否 1是',

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
    provider_task_id VARCHAR(128) DEFAULT NULL COMMENT '外部AI服务任务ID（如DashScope task_id）',

    input_text TEXT COMMENT '输入内容',
    prompt TEXT COMMENT '生成Prompt',

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        COMMENT '任务状态：PENDING/RUNNING/SUCCESS/FAILED',

    result_url VARCHAR(500) DEFAULT NULL COMMENT '结果地址',
    error_message TEXT COMMENT '错误信息',
    sources_json TEXT COMMENT 'RAG来源JSON（保存作品时关联写入biz_work_source）',

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

-- ==================== AI 对话会话与消息 ====================

DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS chat_session;

CREATE TABLE chat_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
    title VARCHAR(100) NOT NULL DEFAULT '新对话' COMMENT '会话标题（首条消息前20字）',
    heritage_project_id BIGINT UNSIGNED DEFAULT NULL COMMENT '限定的非遗项目ID（可选）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0已删除 1正常',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    KEY idx_updated_at (updated_at),

    CONSTRAINT fk_chat_session_user
        FOREIGN KEY (user_id)
        REFERENCES sys_user(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='AI对话会话表';

CREATE TABLE chat_message (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    session_id BIGINT UNSIGNED NOT NULL COMMENT '所属会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：user / assistant',
    content LONGTEXT NOT NULL COMMENT '消息内容',
    sources JSON DEFAULT NULL COMMENT 'assistant消息的RAG检索来源',
    seq_no INT NOT NULL COMMENT '消息在会话中的顺序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    KEY idx_session_id (session_id),
    KEY idx_session_seq (session_id, seq_no),

    CONSTRAINT fk_chat_message_session
        FOREIGN KEY (session_id)
        REFERENCES chat_session(id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='AI对话消息表';

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
    '苗族银饰锻制技艺',
    2,
    '贵州省雷山县、湖南省凤凰县',
    '苗族银饰锻制技艺在国家级名录中的官方名称即为「苗族银饰锻制技艺」，属传统技艺类，项目编号Ⅶ-40，2006年列入第一批国家级非物质文化遗产名录。',
    '苗族银饰历史悠久，经历了从原始装饰品到岩石贝壳装饰品、从植物花卉饰品到金银饰品的演进历程。',
    '苗族银饰不仅具有装饰作用，还承载着民族文化、社会身份和传统习俗的文化内涵，被视为苗族精神品质的体现。',
    '银饰锻制由苗族银匠手工制作，包含铸炼、捶打、焊接、编结、洗涤等环节，工艺水平极高。',
    'https://picsum.photos/seed/miaozu-silver/800/500',
    1
);

INSERT INTO knowledge_document
(heritage_project_id, title, doc_type, content, source, source_url, author, publish_date, access_date, page_no, rights_note, verified)
VALUES
(1, '苗族银饰锻制技艺（第一批国家级非物质文化遗产）基础介绍', '基础介绍',
 '苗族银饰锻制技艺在国家级名录中的官方名称即为「苗族银饰锻制技艺」，属传统技艺类，项目编号Ⅶ-40，2006年列入第一批国家级非物质文化遗产名录，申报地区或单位为贵州省雷山县，保护单位为雷山县非物质文化遗产保护中心（另有一批名录另将湖南省凤凰县列为同批申报单元）。银饰是苗族最喜爱的传统饰物，主要用于妇女装饰，品种分为头饰、面饰、颈饰、肩饰、胸饰、腰饰、臂饰、脚饰、手饰等，彼此配合形成整体装饰效果。银凤冠和银花帽是头饰中的主要饰品，也是整套银饰系列之首，素有「龙头凤尾」之美称，制作复杂，使用的小件饰品少则一百五十余件，多则达两百余件，价值昂贵。苗族银饰精致美观，以贵州省雷山县和湖南省凤凰县的制品为代表，其中雷山县银匠主要集中在西江镇的控排、麻料、乌高村三村。银饰锻制是苗族民间独有的技艺，所有饰件均手工制作，式样和构造经匠师精心设计，由绘图到雕刻和制作共有30道工序，包含铸炼、捶打、焊接、编结、洗涤等环节，工艺水平极高。该技艺先后经历了从原始装饰品到岩石贝壳装饰品、从植物花卉饰品到金银饰品的演进历程，模式和形态基本定型，后品种式样仍不断翻新。文化层面，苗族人民在对外交往中把银饰作为礼品赠送友人，和藏族的哈达、汉族的珠宝一样珍贵；银饰洁白可爱、纯净无瑕、质地坚硬，被视为苗族精神品质的体现。保护现状方面，银饰锻打技艺一般在家庭内部传承，无法择优而授，原有艺人多已年老，真正能继承这一精湛工艺者不多，发展前景不容乐观，有必要加强保护。',
 '中国非物质文化遗产网·中国非物质文化遗产数字博物馆（国家级项目名录页）',
 'https://www.ihchina.cn/project_details/14331/',
 NULL, NULL, '2026-10-21', NULL,
 '中国非物质文化遗产网官方公开项目介绍',
 1);

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;