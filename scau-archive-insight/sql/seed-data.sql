-- ============================================================
-- SCAU Archive Insight — 初始种子数据
-- ============================================================
-- 包含：系统用户、维度表（学院、专业、民族、政治面貌、学位、毕业去向、元数据标准）
-- 省份地理边界数据见 province-geo.sql（含 PostGIS GeoJSON，体积较大）
-- ============================================================

-- ----- 学院 -----
INSERT INTO public.college_dim VALUES (1, '数学与信息学院');
INSERT INTO public.college_dim VALUES (2, '工程学院');
INSERT INTO public.college_dim VALUES (3, '经济管理学院');
INSERT INTO public.college_dim VALUES (4, '人文与法学学院');
INSERT INTO public.college_dim VALUES (5, '外国语学院');

-- ----- 专业 -----
INSERT INTO public.major_dim VALUES (1, 1, 1, '软件工程', 'SE001');
INSERT INTO public.major_dim VALUES (2, 1, 1, '计算机科学', 'CS001');
INSERT INTO public.major_dim VALUES (3, 2, 1, '电子信息工程', 'EE001');
INSERT INTO public.major_dim VALUES (4, 2, 1, '机械设计制造及其自动化', 'ME001');
INSERT INTO public.major_dim VALUES (5, 3, 1, '会计学', 'AC001');
INSERT INTO public.major_dim VALUES (6, 3, 1, '金融学', 'FN001');
INSERT INTO public.major_dim VALUES (7, 4, 1, '法学', 'LA001');
INSERT INTO public.major_dim VALUES (8, 5, 1, '英语', 'EN001');

-- ----- 民族 -----
INSERT INTO public.nation_dim VALUES (1, '汉族');
INSERT INTO public.nation_dim VALUES (2, '蒙古族');
INSERT INTO public.nation_dim VALUES (3, '回族');
INSERT INTO public.nation_dim VALUES (4, '藏族');
INSERT INTO public.nation_dim VALUES (5, '维吾尔族');
INSERT INTO public.nation_dim VALUES (6, '苗族');
INSERT INTO public.nation_dim VALUES (7, '彝族');
INSERT INTO public.nation_dim VALUES (8, '壮族');
INSERT INTO public.nation_dim VALUES (9, '布依族');
INSERT INTO public.nation_dim VALUES (10, '朝鲜族');
INSERT INTO public.nation_dim VALUES (11, '满族');
INSERT INTO public.nation_dim VALUES (12, '侗族');
INSERT INTO public.nation_dim VALUES (13, '瑶族');
INSERT INTO public.nation_dim VALUES (14, '白族');
INSERT INTO public.nation_dim VALUES (15, '土家族');
INSERT INTO public.nation_dim VALUES (16, '哈尼族');
INSERT INTO public.nation_dim VALUES (17, '哈萨克族');
INSERT INTO public.nation_dim VALUES (18, '傣族');
INSERT INTO public.nation_dim VALUES (19, '黎族');
INSERT INTO public.nation_dim VALUES (20, '傈僳族');
INSERT INTO public.nation_dim VALUES (21, '佤族');
INSERT INTO public.nation_dim VALUES (22, '畲族');
INSERT INTO public.nation_dim VALUES (23, '高山族');
INSERT INTO public.nation_dim VALUES (24, '拉祜族');
INSERT INTO public.nation_dim VALUES (25, '水族');
INSERT INTO public.nation_dim VALUES (26, '东乡族');
INSERT INTO public.nation_dim VALUES (27, '纳西族');
INSERT INTO public.nation_dim VALUES (28, '景颇族');
INSERT INTO public.nation_dim VALUES (29, '柯尔克孜族');
INSERT INTO public.nation_dim VALUES (30, '土族');
INSERT INTO public.nation_dim VALUES (31, '达斡尔族');
INSERT INTO public.nation_dim VALUES (32, '仫佬族');
INSERT INTO public.nation_dim VALUES (33, '羌族');
INSERT INTO public.nation_dim VALUES (34, '布朗族');
INSERT INTO public.nation_dim VALUES (35, '撒拉族');
INSERT INTO public.nation_dim VALUES (36, '毛难族');
INSERT INTO public.nation_dim VALUES (37, '仡佬族');
INSERT INTO public.nation_dim VALUES (38, '锡伯族');
INSERT INTO public.nation_dim VALUES (39, '阿昌族');
INSERT INTO public.nation_dim VALUES (40, '普米族');
INSERT INTO public.nation_dim VALUES (41, '塔吉克族');
INSERT INTO public.nation_dim VALUES (42, '怒族');
INSERT INTO public.nation_dim VALUES (43, '乌孜别克族');
INSERT INTO public.nation_dim VALUES (44, '俄罗斯族');
INSERT INTO public.nation_dim VALUES (45, '鄂温克族');
INSERT INTO public.nation_dim VALUES (46, '德昂族');
INSERT INTO public.nation_dim VALUES (47, '保安族');
INSERT INTO public.nation_dim VALUES (48, '裕固族');
INSERT INTO public.nation_dim VALUES (49, '京族');
INSERT INTO public.nation_dim VALUES (50, '塔塔尔族');
INSERT INTO public.nation_dim VALUES (51, '独龙族');
INSERT INTO public.nation_dim VALUES (52, '鄂伦春族');
INSERT INTO public.nation_dim VALUES (53, '赫哲族');
INSERT INTO public.nation_dim VALUES (54, '门巴族');
INSERT INTO public.nation_dim VALUES (55, '珞巴族');
INSERT INTO public.nation_dim VALUES (56, '基诺族');

-- ----- 政治面貌 -----
INSERT INTO public.political_dim VALUES (1, '中共党员');
INSERT INTO public.political_dim VALUES (2, '共青团员');
INSERT INTO public.political_dim VALUES (3, '群众');
INSERT INTO public.political_dim VALUES (4, '九三学社社员');
INSERT INTO public.political_dim VALUES (5, '中国民主同盟盟员');
INSERT INTO public.political_dim VALUES (6, '中国民主建国会会员');
INSERT INTO public.political_dim VALUES (7, '中国民主促进会会员');
INSERT INTO public.political_dim VALUES (8, '中国农工民主党党员');
INSERT INTO public.political_dim VALUES (9, '中国致公党党员');
INSERT INTO public.political_dim VALUES (10, '台湾民主自治同盟盟员');
INSERT INTO public.political_dim VALUES (11, '无党派人士');

-- ----- 学位 -----
INSERT INTO public.degree_dim VALUES (1, '学士');
INSERT INTO public.degree_dim VALUES (2, '硕士');
INSERT INTO public.degree_dim VALUES (3, '博士');
INSERT INTO public.degree_dim VALUES (4, '理学学士学位');
INSERT INTO public.degree_dim VALUES (13, '法学学士');
INSERT INTO public.degree_dim VALUES (18, '普通本科生');
INSERT INTO public.degree_dim VALUES (19, '无学位');
INSERT INTO public.degree_dim VALUES (20, '工学学士学位');
INSERT INTO public.degree_dim VALUES (21, '农学学士学位');
INSERT INTO public.degree_dim VALUES (22, '管理学学士学位');
INSERT INTO public.degree_dim VALUES (23, '法学学士学位');
INSERT INTO public.degree_dim VALUES (24, '经济学学士学位');
INSERT INTO public.degree_dim VALUES (25, '文学学士学位');
INSERT INTO public.degree_dim VALUES (26, '历史学学士学位');
INSERT INTO public.degree_dim VALUES (27, '哲学学士学位');
INSERT INTO public.degree_dim VALUES (28, '艺术学学士学位');
INSERT INTO public.degree_dim VALUES (29, '1455 法学学士 法学学士');

-- ----- 毕业去向 -----
INSERT INTO public.destination_dim VALUES (1, '就业');
INSERT INTO public.destination_dim VALUES (2, '升学');
INSERT INTO public.destination_dim VALUES (3, '毕业');
INSERT INTO public.destination_dim VALUES (4, '结业');
INSERT INTO public.destination_dim VALUES (5, '延长学习年限');

-- ----- 元数据标准字段映射 -----
INSERT INTO public.metadata_standard VALUES ('student_no', '学号', 'string', '学号', 'direct', NULL, false, 1);
INSERT INTO public.metadata_standard VALUES ('name', '姓名', 'string', '姓名', 'direct', NULL, false, 2);
INSERT INTO public.metadata_standard VALUES ('gender', '性别', 'varchar', '性别', 'direct', '', false, 4);
INSERT INTO public.metadata_standard VALUES ('degree_name', '学位名称', 'varchar', '学位类别', 'direct', '', false, 5);
INSERT INTO public.metadata_standard VALUES ('id_card', '身份证号', 'varchar', '证件号码', 'direct', '', false, 3);
INSERT INTO public.metadata_standard VALUES ('graduation_date', '毕业日期', 'date', '发证日期', 'direct', '', false, 7);
INSERT INTO public.metadata_standard VALUES ('dest_name', '毕业去向', 'varchar', '毕业结论', 'direct', '', false, 8);
INSERT INTO public.metadata_standard VALUES ('province_name', '省份名', 'varchar', '省份', 'direct', '', false, 9);
INSERT INTO public.metadata_standard VALUES ('admission_date', '录取日期', 'varchar', '录取日期', 'direct', '', false, 10);
INSERT INTO public.metadata_standard VALUES ('exam_no', '考生号', 'varchar', '考号', 'direct', '', false, 11);
INSERT INTO public.metadata_standard VALUES ('admission_score', '录取分数', 'varchar', '录取分数', 'direct', '', false, 13);

-- ----- 系统用户（密码 BCrypt 加密）-----
INSERT INTO public.sys_user (id, username, password, real_name, phone, email, role, create_time, update_time) VALUES (1, 'admin', '$2a$10$A.tIFkVGXxDxE8WMHGseiOXwxKfSDgL7EKfMUM26VvJYSqFjr5p6.', '管理员', '13692258486', '858558192@qq.com', '1', '2026-04-24 11:24:15.515268', '2026-05-01 17:34:44.566752');
