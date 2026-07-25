-- ============================================================
-- SCAU Archive Insight — 数据库初始化脚本
-- ============================================================
-- 用法: psql -h localhost -U postgres -f 项目路径/sql/init.sql
-- ============================================================

-- 0. 创建数据库（如果已存在则跳过）
SELECT 'CREATE DATABASE scau_archive'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'scau_archive')\gexec

-- 切换到 scau_archive 数据库
\c scau_archive

-- 1. 创建数据库结构（表、序列、索引、触发器）
\ir parts/schema.sql

-- 2. 导入初始种子数据（维度表、系统用户）
\ir parts/seed-data.sql

-- 3. 导入省份地理边界数据（PostGIS MultiPolygon，约 2.7MB）
\ir parts/province-geo.sql

-- 4. 导入演示业务数据（录取/毕业/学籍/知识库，由 seed_fake_data.py 生成）
\ir parts/demo-data.sql
