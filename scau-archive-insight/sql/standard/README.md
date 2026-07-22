# 华南农业大学学生档案元数据标准

> 参考 DA/T 31-2017《纸质档案数字化技术规范》扩展
> 版本 1.0 — 2026-07

---

## 1 范围

本标准规定了华南农业大学学生档案数据的元数据元素定义、实体类型划分及数据交换格式。

本标准适用于以下三类学生档案在系统间交换、归档与共享时的数据描述与校验：

- 录取档案（Admission）
- 学籍档案（Student Status）
- 毕业档案（Graduation）

## 2 规范性引用文件

| 标准编号 | 标准名称 |
|----------|----------|
| DA/T 31-2017 | 纸质档案数字化技术规范 |
| GB/T 14946.1-2009 | 个人信息分类与代码 第 1 部分：个人信息分类与代码 |
| GB/T 2260 | 中华人民共和国行政区划代码 |
| GB/T 3304 | 中国各民族名称的罗马字母拼写法和代码 |
| GB/T 4762 | 政治面貌代码 |

## 3 术语和定义

| 术语 | 定义 |
|------|------|
| 元数据 | 描述档案数据内容和上下文的数据 |
| 实体类型 | 同一类档案记录的抽象，共享相同的元数据元素集合 |
| 必选 | 该元素必须出现 |
| 条件必选 | 当特定条件满足时该元素必须出现 |
| 可选 | 该元素可以出现也可以不出现 |

## 4 元数据元素定义

每个元素按以下属性描述：

- **编号**：元素唯一标识符（SCAU 前缀为扩展元素）
- **名称**：元素的中文名称
- **定义**：元素的语义说明
- **数据类型**：元素值的类型
- **值域**：允许的取值范围
- **约束**：必选 / 条件必选 / 可选
- **最大出现次数**：元素可重复的最大次数
- **对应字段**：映射到的数据库字段或维度表

### 4.1 公共元素（适用于所有档案类型）

| 编号 | 名称 | 定义 | 数据类型 | 值域 | 约束 | 最大出现次数 | 对应字段 |
|------|------|------|----------|------|------|-------------|----------|
| M1 | 实体标识符 | 每条档案记录在系统内的唯一标识 | `integer` | 正整数 | 必选 | 1 | 各事实表.id |
| M2 | 题名 | 档案记录的名称描述 | `string` | 自由文本 | 条件必选 | 1 | 自动组合生成 |
| M3 | 归档日期 | 记录创建/归档的时间戳 | `dateTime` | ISO 8601 | 必选 | 1 | create_time |
| M4 | 档号 | 档案分类编号（年度-类型-序号） | `string` | 模式：`\d{4}-[A-Z]{2}-\d{6}` | 条件必选 | 1 | 自动生成 |
| SCAU-C01 | 学号 | 学生唯一学号 | `string` | 长度 ≤ 32 | 必选 | 1 | student_no |
| SCAU-C02 | 姓名 | 学生姓名 | `string` | 长度 ≤ 30 | 必选 | 1 | name |
| SCAU-C03 | 身份证号 | 学生身份证件号码 | `string` | 18 位数字+字母，符合 GB 11643 | 条件必选 | 1 | id_card |
| SCAU-C04 | 性别 | 学生性别 | `string` | 男 / 女 / 未知 | 可选 | 1 | gender |
| SCAU-C05 | 学院名称 | 学生所属学院 | `string` | 长度 ≤ 50，须在学院维度中 | 条件必选 | 1 | college_dim.college_name |
| SCAU-C06 | 专业名称 | 学生所学专业 | `string` | 长度 ≤ 50，须在专业维度中 | 条件必选 | 1 | major_dim.major_name |
| SCAU-C07 | 专业代码 | 专业国家标准代码 | `string` | 长度 ≤ 20 | 可选 | 1 | major_dim.major_code |
| SCAU-C08 | 生源省份 | 学生生源所在省份 | `string` | 长度 ≤ 30，须在省份维度中 | 可选 | 1 | province_dim.province_name |
| SCAU-C09 | 民族 | 学生民族 | `string` | 长度 ≤ 20，须在民族维度中 | 可选 | 1 | nation_dim.nation_name |
| SCAU-C10 | 政治面貌 | 学生政治面貌 | `string` | 长度 ≤ 20，须在政治面貌维度中 | 可选 | 1 | political_dim.political_name |

### 4.2 录取档案特有元素

| 编号 | 名称 | 定义 | 数据类型 | 值域 | 约束 | 最大出现次数 | 对应字段 |
|------|------|------|----------|------|------|-------------|----------|
| SCAU-A01 | 考生号 | 高考考生编号 | `string` | 长度 ≤ 32 | 条件必选 | 1 | exam_no |
| SCAU-A02 | 录取分数 | 高考录取总分数 | `integer` | [0, 900] | 可选 | 1 | admission_score |
| SCAU-A03 | 录取日期 | 录取通知书发放日期 | `date` | ISO 8601 | 可选 | 1 | admission_date |

### 4.3 学籍档案特有元素

| 编号 | 名称 | 定义 | 数据类型 | 值域 | 约束 | 最大出现次数 | 对应字段 |
|------|------|------|----------|------|------|-------------|----------|
| SCAU-S01 | 班级名称 | 学生所属班级 | `string` | 长度 ≤ 50，须在班级维度中 | 条件必选 | 1 | class_dim.class_name |
| SCAU-S02 | 年级 | 学生入学年级 | `string` | 格式 `\d{4}` | 可选 | 1 | class_dim.grade |
| SCAU-S03 | 学制 | 专业标准修业年限（年） | `integer` | [2, 5] | 可选 | 1 | class_dim.study_length |
| SCAU-S04 | 入学日期 | 学生入学注册日期 | `date` | ISO 8601 | 可选 | 1 | student_dim.start_date |
| SCAU-S05 | 是否毕业 | 学生是否已完成毕业 | `boolean` | true / false | 可选 | 1 | student_fact.graduated |
| SCAU-S06 | 离校日期 | 学生离校日期 | `date` | ISO 8601 | 条件必选 | 1 | student_dim.end_date |

### 4.4 毕业档案特有元素

| 编号 | 名称 | 定义 | 数据类型 | 值域 | 约束 | 最大出现次数 | 对应字段 |
|------|------|------|----------|------|------|-------------|----------|
| SCAU-G01 | 学位名称 | 授予的学位名称 | `string` | 长度 ≤ 30，须在学位维度中 | 条件必选 | 1 | degree_dim.degree_name |
| SCAU-G02 | 毕业日期 | 毕业证书发放日期 | `date` | ISO 8601 | 条件必选 | 1 | graduation_date |
| SCAU-G03 | 毕业去向 | 学生毕业后去向类别 | `string` | 长度 ≤ 30，须在去向维度中 | 可选 | 1 | destination_dim.dest_name |
| SCAU-G04 | 毕业结论 | 毕业审核结论描述 | `string` | 自由文本 | 可选 | 1 | 来源文件文本 |

## 5 实体类型定义

### 5.1 AdmissionRecord（录取档案实体）

包含 M1–M4 + SCAU-C01~C10 + SCAU-A01~A03。

```json
{
  "entityType": "AdmissionRecord",
  "elements": {
    "identifier": 10001,
    "title": "张三-2025年录取档案",
    "archiveDate": "2025-09-01T10:00:00",
    "archiveCode": "2025-AD-000001",
    "studentNo": "2025001001",
    "name": "张三",
    "idCard": "440106200701011234",
    "gender": "男",
    "college": "数学与信息学院",
    "major": "计算机科学与技术",
    "majorCode": "080901",
    "province": "广东省",
    "nation": "汉族",
    "politicalStatus": "共青团员",
    "examNo": "25440101123456",
    "admissionScore": 580,
    "admissionDate": "2025-08-15"
  }
}
```

### 5.2 StudentStatusRecord（学籍档案实体）

包含 M1–M4 + SCAU-C01~C10 + SCAU-S01~S06。

```json
{
  "entityType": "StudentStatusRecord",
  "elements": {
    "identifier": 20001,
    "title": "张三-2025学年学籍档案",
    "archiveDate": "2025-10-08T09:00:00",
    "archiveCode": "2025-SS-000001",
    "studentNo": "2025001001",
    "name": "张三",
    "idCard": "440106200701011234",
    "gender": "男",
    "college": "数学与信息学院",
    "major": "计算机科学与技术",
    "majorCode": "080901",
    "province": "广东省",
    "nation": "汉族",
    "politicalStatus": "共青团员",
    "className": "2025级计算机科学与技术1班",
    "grade": "2025",
    "studyLength": 4,
    "startDate": "2025-09-01",
    "graduated": false,
    "endDate": null
  }
}
```

### 5.3 GraduationRecord（毕业档案实体）

包含 M1–M4 + SCAU-C01~C10 + SCAU-G01~G04。

```json
{
  "entityType": "GraduationRecord",
  "elements": {
    "identifier": 30001,
    "title": "张三-2029年毕业档案",
    "archiveDate": "2029-06-30T17:00:00",
    "archiveCode": "2029-GR-000001",
    "studentNo": "2025001001",
    "name": "张三",
    "idCard": "440106200701011234",
    "gender": "男",
    "college": "数学与信息学院",
    "major": "计算机科学与技术",
    "majorCode": "080901",
    "province": "广东省",
    "nation": "汉族",
    "politicalStatus": "中共党员",
    "degree": "工学学士学位",
    "graduationDate": "2029-06-25",
    "destination": "签就业协议形式就业",
    "graduationDecision": "准予毕业"
  }
}
```

## 6 Schema 文件

本标准同时提供两种机器可读的 Schema 定义，两者语义等价：

| 格式 | 文件 | 用途 |
|------|------|------|
| JSON Schema | `archive-metadata-schema.json` | JSON 交换格式校验 |
| XML Schema (XSD) | `archive-metadata-schema.xsd` | XML 交换格式校验 |

## 7 数据质量要求

档案数据交换时，应满足以下质量约束：

- **完整性**：必选元素不可缺失
- **一致性**：维度字段值必须在对应维度表中存在（参照完整性）
- **准确性**：身份证号、学号等标识符应符合校验规则
- **时效性**：归档日期不应晚于当前时间

---

*（完）*
