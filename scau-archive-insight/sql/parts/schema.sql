-- ============================================================
-- SCAU Archive Insight — 数据库结构定义 (DDL)
-- PostgreSQL 18 + PostGIS
-- ============================================================

-- 启用 PostGIS 扩展（用于地理空间数据）
CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;

-- ============================================================
-- 触发器函数：自动更新 update_time 字段
-- ============================================================
CREATE OR REPLACE FUNCTION public.update_time_trigger()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

-- ============================================================
-- 维度表
-- ============================================================

-- 学院
CREATE TABLE IF NOT EXISTS public.college_dim (
    college_id   INTEGER PRIMARY KEY,
    college_name VARCHAR(50) NOT NULL UNIQUE
);
CREATE SEQUENCE IF NOT EXISTS public.college_dim_college_id_seq AS INTEGER
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.college_dim_college_id_seq OWNED BY public.college_dim.college_id;
ALTER TABLE public.college_dim ALTER COLUMN college_id SET DEFAULT nextval('public.college_dim_college_id_seq');

-- 专业
CREATE TABLE IF NOT EXISTS public.major_dim (
    major_id   INTEGER PRIMARY KEY,
    college_id INTEGER NOT NULL,
    degree_id  INTEGER,
    major_name VARCHAR(50) NOT NULL,
    major_code VARCHAR(20) NOT NULL UNIQUE
);
CREATE SEQUENCE IF NOT EXISTS public.major_dim_major_id_seq AS INTEGER
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.major_dim_major_id_seq OWNED BY public.major_dim.major_id;
ALTER TABLE public.major_dim ALTER COLUMN major_id SET DEFAULT nextval('public.major_dim_major_id_seq');
CREATE INDEX IF NOT EXISTS idx_major_college_id ON public.major_dim (college_id);
CREATE INDEX IF NOT EXISTS idx_major_name      ON public.major_dim (major_name);

-- 班级
CREATE TABLE IF NOT EXISTS public.class_dim (
    class_id    INTEGER PRIMARY KEY,
    major_id    INTEGER NOT NULL,
    class_name  VARCHAR(50) NOT NULL,
    grade       VARCHAR(10) NOT NULL,
    study_length INTEGER NOT NULL
);
CREATE SEQUENCE IF NOT EXISTS public.class_dim_class_id_seq AS INTEGER
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.class_dim_class_id_seq OWNED BY public.class_dim.class_id;
ALTER TABLE public.class_dim ALTER COLUMN class_id SET DEFAULT nextval('public.class_dim_class_id_seq');
CREATE INDEX IF NOT EXISTS idx_class_major_id ON public.class_dim (major_id);
CREATE INDEX IF NOT EXISTS idx_class_name     ON public.class_dim (class_name);

-- 民族
CREATE TABLE IF NOT EXISTS public.nation_dim (
    nation_id   INTEGER PRIMARY KEY,
    nation_name VARCHAR(20) NOT NULL UNIQUE
);
CREATE SEQUENCE IF NOT EXISTS public.nation_dim_nation_id_seq AS INTEGER
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.nation_dim_nation_id_seq OWNED BY public.nation_dim.nation_id;
ALTER TABLE public.nation_dim ALTER COLUMN nation_id SET DEFAULT nextval('public.nation_dim_nation_id_seq');

-- 政治面貌
CREATE TABLE IF NOT EXISTS public.political_dim (
    political_id   INTEGER PRIMARY KEY,
    political_name VARCHAR(20) NOT NULL UNIQUE
);
CREATE SEQUENCE IF NOT EXISTS public.political_dim_political_id_seq AS INTEGER
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.political_dim_political_id_seq OWNED BY public.political_dim.political_id;
ALTER TABLE public.political_dim ALTER COLUMN political_id SET DEFAULT nextval('public.political_dim_political_id_seq');

-- 省份（含地理边界数据）
CREATE TABLE IF NOT EXISTS public.province_dim (
    province_id   INTEGER PRIMARY KEY,
    province_name VARCHAR(30) NOT NULL UNIQUE,
    geom          public.geometry(MultiPolygon, 4326),
    center        public.geometry(Point, 4326)
);
CREATE SEQUENCE IF NOT EXISTS public.province_dim_province_id_seq AS INTEGER
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.province_dim_province_id_seq OWNED BY public.province_dim.province_id;
ALTER TABLE public.province_dim ALTER COLUMN province_id SET DEFAULT nextval('public.province_dim_province_id_seq');

-- 学位
CREATE TABLE IF NOT EXISTS public.degree_dim (
    degree_id   INTEGER PRIMARY KEY,
    degree_name VARCHAR(30) NOT NULL UNIQUE
);
CREATE SEQUENCE IF NOT EXISTS public.degree_dim_degree_id_seq AS INTEGER
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.degree_dim_degree_id_seq OWNED BY public.degree_dim.degree_id;
ALTER TABLE public.degree_dim ALTER COLUMN degree_id SET DEFAULT nextval('public.degree_dim_degree_id_seq');

-- 毕业去向
CREATE TABLE IF NOT EXISTS public.destination_dim (
    dest_id   INTEGER PRIMARY KEY,
    dest_name VARCHAR(30) NOT NULL UNIQUE
);
CREATE SEQUENCE IF NOT EXISTS public.destination_dim_dest_id_seq AS INTEGER
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.destination_dim_dest_id_seq OWNED BY public.destination_dim.dest_id;
ALTER TABLE public.destination_dim ALTER COLUMN dest_id SET DEFAULT nextval('public.destination_dim_dest_id_seq');

-- ============================================================
-- 事实表
-- ============================================================

-- 学生基本信息表
CREATE TABLE IF NOT EXISTS public.student_dim (
    dim_id       INTEGER PRIMARY KEY,
    student_no   VARCHAR(32) NOT NULL,
    name         VARCHAR(30),
    nation_id    INTEGER,
    political_id INTEGER,
    start_date   DATE,
    end_date     DATE,
    is_current   SMALLINT,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE SEQUENCE IF NOT EXISTS public.student_dim_dim_id_seq
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.student_dim_dim_id_seq OWNED BY public.student_dim.dim_id;
ALTER TABLE public.student_dim ALTER COLUMN dim_id SET DEFAULT nextval('public.student_dim_dim_id_seq');
CREATE INDEX IF NOT EXISTS idx_student_dim_no       ON public.student_dim (student_no);
CREATE INDEX IF NOT EXISTS idx_student_dim_nation    ON public.student_dim (nation_id);
CREATE INDEX IF NOT EXISTS idx_student_dim_political ON public.student_dim (political_id);

-- 学生事实表
CREATE TABLE IF NOT EXISTS public.student_fact (
    id             BIGINT PRIMARY KEY,
    student_no     VARCHAR(32) UNIQUE,
    name           VARCHAR(30),
    id_card        VARCHAR(18) UNIQUE,
    gender         VARCHAR(4) DEFAULT '未知',
    degree_id      INTEGER,
    major_id       INTEGER,
    class_id       INTEGER,
    province_id    INTEGER,
    admission_date DATE,
    create_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    graduated      BOOLEAN DEFAULT FALSE,
    file_id        INTEGER,
    CONSTRAINT student_fact_id_card_check CHECK (char_length(id_card) = 18)
);
COMMENT ON COLUMN public.student_fact.graduated IS '是否已毕业';
COMMENT ON COLUMN public.student_fact.file_id   IS '来源文件ID，关联archive_file_dim';
CREATE SEQUENCE IF NOT EXISTS public.student_fact_id_seq
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.student_fact_id_seq OWNED BY public.student_fact.id;
CREATE INDEX IF NOT EXISTS idx_student_major         ON public.student_fact (major_id);
CREATE INDEX IF NOT EXISTS idx_student_province       ON public.student_fact (province_id);
CREATE INDEX IF NOT EXISTS idx_student_class          ON public.student_fact (class_id);
CREATE INDEX IF NOT EXISTS idx_student_admission_date ON public.student_fact (admission_date);

-- 录取事实表
CREATE TABLE IF NOT EXISTS public.admission_fact (
    id              BIGINT PRIMARY KEY,
    student_no      VARCHAR(32),
    exam_no         VARCHAR(32),
    province_id     INTEGER,
    major_id        INTEGER,
    admission_date  DATE,
    file_id         INTEGER,
    id_card         VARCHAR(18),
    name            VARCHAR(30),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gender          VARCHAR(10),
    degree_id       INTEGER,
    admission_score INTEGER
);
CREATE SEQUENCE IF NOT EXISTS public.admission_fact_id_seq
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.admission_fact_id_seq OWNED BY public.admission_fact.id;
CREATE INDEX IF NOT EXISTS idx_admission_student     ON public.admission_fact (student_no);
CREATE INDEX IF NOT EXISTS idx_admission_exam_no     ON public.admission_fact (exam_no);
CREATE INDEX IF NOT EXISTS idx_admission_id_card     ON public.admission_fact (id_card);
CREATE INDEX IF NOT EXISTS idx_admission_name        ON public.admission_fact (name);
CREATE INDEX IF NOT EXISTS idx_admission_date        ON public.admission_fact (admission_date);
CREATE INDEX IF NOT EXISTS idx_admission_province    ON public.admission_fact (province_id);
CREATE INDEX IF NOT EXISTS idx_admission_major       ON public.admission_fact (major_id);
CREATE INDEX IF NOT EXISTS idx_admission_file_id     ON public.admission_fact (file_id);

-- 毕业事实表
CREATE TABLE IF NOT EXISTS public.graduation_fact (
    id              BIGINT PRIMARY KEY,
    student_no      VARCHAR(32),
    degree_id       INTEGER,
    dest_id         INTEGER,
    graduation_date DATE,
    file_id         INTEGER,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    id_card         VARCHAR(18),
    name            VARCHAR(30),
    gender          VARCHAR(10)
);
CREATE SEQUENCE IF NOT EXISTS public.graduation_fact_id_seq
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.graduation_fact_id_seq OWNED BY public.graduation_fact.id;
CREATE INDEX IF NOT EXISTS idx_graduation_student_no  ON public.graduation_fact (student_no);
CREATE INDEX IF NOT EXISTS idx_graduation_date        ON public.graduation_fact (graduation_date);
CREATE INDEX IF NOT EXISTS idx_graduation_degree_id   ON public.graduation_fact (degree_id);
CREATE INDEX IF NOT EXISTS idx_graduation_dest_id     ON public.graduation_fact (dest_id);
CREATE INDEX IF NOT EXISTS idx_graduation_file_id     ON public.graduation_fact (file_id);

-- ============================================================
-- 元数据与日志表
-- ============================================================

-- 元数据标准字段映射
CREATE TABLE IF NOT EXISTS public.metadata_standard (
    field_code     VARCHAR(32) NOT NULL,
    field_name     VARCHAR(50) NOT NULL,
    field_type     VARCHAR(20),
    source_field   VARCHAR(100) NOT NULL,
    transform_type       VARCHAR(50) DEFAULT 'DIRECT',
    transform_rule       VARCHAR(255),
    is_required    BOOLEAN DEFAULT FALSE,
    metadata_id   INTEGER NOT NULL PRIMARY KEY
);
CREATE SEQUENCE IF NOT EXISTS public.metadata_standard_metadata_id_seq AS INTEGER
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.metadata_standard_metadata_id_seq OWNED BY public.metadata_standard.metadata_id;
ALTER TABLE public.metadata_standard ALTER COLUMN metadata_id SET DEFAULT nextval('public.metadata_standard_metadata_id_seq');

-- 归档文件记录
CREATE TABLE IF NOT EXISTS public.archive_file_dim (
    file_id     INTEGER PRIMARY KEY,
    file_name   VARCHAR(255) NOT NULL,
    file_type   VARCHAR(20) NOT NULL,
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE SEQUENCE IF NOT EXISTS public.archive_file_dim_file_id_seq AS INTEGER
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.archive_file_dim_file_id_seq OWNED BY public.archive_file_dim.file_id;
ALTER TABLE public.archive_file_dim ALTER COLUMN file_id SET DEFAULT nextval('public.archive_file_dim_file_id_seq');
CREATE INDEX IF NOT EXISTS idx_archive_file_type ON public.archive_file_dim (file_type);
CREATE INDEX IF NOT EXISTS idx_archive_upload_time ON public.archive_file_dim (upload_time);

-- OCR 识别日志
CREATE TABLE IF NOT EXISTS public.ocr_log_dim (
    log_id            INTEGER PRIMARY KEY,
    file_id           INTEGER,
    recognize_status  VARCHAR(20),
    recognize_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    f1_score          NUMERIC(5, 2),
    file_name         VARCHAR(255),
    file_type         VARCHAR(50),
    error_message     TEXT,
    message           TEXT,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE SEQUENCE IF NOT EXISTS public.ocr_log_dim_log_id_seq AS INTEGER
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.ocr_log_dim_log_id_seq OWNED BY public.ocr_log_dim.log_id;
ALTER TABLE public.ocr_log_dim ALTER COLUMN log_id SET DEFAULT nextval('public.ocr_log_dim_log_id_seq');
CREATE INDEX IF NOT EXISTS idx_ocr_log_file_id    ON public.ocr_log_dim (file_id);
CREATE INDEX IF NOT EXISTS idx_ocr_log_time       ON public.ocr_log_dim (recognize_time DESC);
CREATE INDEX IF NOT EXISTS idx_ocr_log_file_status ON public.ocr_log_dim (file_name, recognize_status);

-- 质量评分
CREATE TABLE IF NOT EXISTS public.quality_score_dim (
    score_id     INTEGER PRIMARY KEY,
    file_id      INTEGER NOT NULL,
    completeness INTEGER,
    consistency  INTEGER,
    accuracy     INTEGER,
    timeliness   INTEGER,
    total_score  INTEGER,
    check_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_quality_completeness CHECK (completeness >= 0 AND completeness <= 100),
    CONSTRAINT ck_quality_consistency  CHECK (consistency  >= 0 AND consistency  <= 100),
    CONSTRAINT ck_quality_accuracy     CHECK (accuracy     >= 0 AND accuracy     <= 100),
    CONSTRAINT ck_quality_timeliness   CHECK (timeliness   >= 0 AND timeliness   <= 100),
    CONSTRAINT ck_quality_total_score  CHECK (total_score  >= 0 AND total_score  <= 100)
);
CREATE SEQUENCE IF NOT EXISTS public.quality_score_dim_score_id_seq AS INTEGER
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.quality_score_dim_score_id_seq OWNED BY public.quality_score_dim.score_id;
ALTER TABLE public.quality_score_dim ALTER COLUMN score_id SET DEFAULT nextval('public.quality_score_dim_score_id_seq');
CREATE INDEX IF NOT EXISTS idx_quality_file_id ON public.quality_score_dim (file_id);

-- ============================================================
-- 系统用户表
-- ============================================================

CREATE TABLE IF NOT EXISTS public.sys_user (
    id          INTEGER PRIMARY KEY,
    username    VARCHAR(50) NOT NULL UNIQUE,
    password    VARCHAR(150) NOT NULL,
    real_name   VARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    status      INTEGER DEFAULT 1,
    role        VARCHAR(20) DEFAULT 'user',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark      VARCHAR(255)
);
CREATE SEQUENCE IF NOT EXISTS public.sys_user_id_seq AS INTEGER
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.sys_user_id_seq OWNED BY public.sys_user.id;

-- ============================================================
-- 自动触发器
-- ============================================================

DROP TRIGGER IF EXISTS trg_update_time ON public.sys_user;
CREATE TRIGGER trg_update_time
    BEFORE UPDATE ON public.sys_user
    FOR EACH ROW EXECUTE FUNCTION public.update_time_trigger();

-- ============================================================
-- 知识库（RAG）
-- ============================================================

-- 启用 pgvector 扩展（用于向量相似度搜索）
CREATE EXTENSION IF NOT EXISTS vector;

-- 知识库文档表
CREATE TABLE IF NOT EXISTS public.knowledge_base (
    id           SERIAL PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    file_type    VARCHAR(20) NOT NULL,
    source       VARCHAR(50) DEFAULT 'upload',
    url          TEXT,
    chunk_count  INTEGER DEFAULT 0,
    status       VARCHAR(20) DEFAULT 'parsing',
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_msg    TEXT,
    file_path    TEXT
);

-- 知识库文本块表（含向量）
CREATE TABLE IF NOT EXISTS public.knowledge_chunks (
    id           SERIAL PRIMARY KEY,
    kb_id        INTEGER NOT NULL REFERENCES public.knowledge_base(id) ON DELETE CASCADE,
    chunk_index  INTEGER NOT NULL,
    content      TEXT NOT NULL,
    metadata     JSONB DEFAULT '{}',
    embedding    vector(1024)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_kb_chunks_kb_id ON public.knowledge_chunks (kb_id);
CREATE INDEX IF NOT EXISTS idx_kb_status ON public.knowledge_base (status);
