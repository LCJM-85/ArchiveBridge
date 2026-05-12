# 文件上传 → 数据持久化完整流程

## 整体架构

```
前端上传 → Controller → 保存到 temp/ → Processor(处理) → 持久化 DB → 归档到 archive/failed
```

---

## 1. 前端上传

**文件**: `scau_archive-frontend/src/views/archive/ArchiveUpload.vue`

- 选择文件（图片/PDF/Excel/CSV），通过 `uploadFiles()` 发送
- 用 FormData 上传：`files` + `type`(文件类型) + `archiveType`(档案类型: admission/graduation)
- 超时 600 秒（OCR 慢）
- 上传失败后文件保留在列表中，可重新上传

**API**: `scau_archive-frontend/src/api/modules/archive.js`

```
POST /api/upload
参数: files, type, archiveType
```

---

## 2. 后端接收 & 存储

**Controller**: `ArchiveUploadController.java`

```
接收文件 → StorageService.saveFiles()
                  ↓
          保存到 storage/temp/{yyyyMMdd}/{type}/{timestamp}_{原名}
                  ↓
          按扩展名分发到不同 Processor
```

| 文件类型 | 扩展名 | Processor |
|---------|--------|-----------|
| CSV | .csv | CSVProcessor |
| Excel | .xls/.xlsx | ExcelProcessor |
| PDF | .pdf | PDFProcessor |
| 图片 | .jpg/.png/.tiff 等 | ImageProcessor |

---

## 3. 图片处理流程 (ImageProcessor)

```
图片 → OpenCV 增强 → PPStructureV3(OCR) → JSON 解析 → 持久化
```

### 3.1 OpenCV 增强 (`opencv.py`)
- 灰度化 → 高斯去噪 → 自适应二值化 → 锐化

### 3.2 PPStructureV3 表格识别 (`ocr_table.py`)
- 调用 PaddleX PPStructureV3 识别表格
- 输出 HTML 格式 (`<tr><td>`)
- 解析 HTML 提取单元格文本

### 3.3 字段映射 (同文件内 `extract_metadata`)
- 用 metadata_standard 表中的规则匹配列头
- 匹配优先级：`fieldName > sourceField > fieldCode`
- 四级匹配：精确 → 去空白 → 包含 → Levenshtein 距离
- 输出 `{field_code: value}` JSON

---

## 4. PDF 处理流程 (PDFProcessor)

```
PDF → PDF转图片(每页一张) → 逐页做图片处理(同 ImageProcessor) → 合并数据 → 持久化
```

### 4.1 PDF 转图片 (`pdf2image.py`)
- 使用 PyMuPDF 将 PDF 每页转为 200dpi PNG

### 4.2 逐页处理
- 每页走 OpenCV 增强 → PPStructureV3 OCR → 解析 JSON

### 4.3 异常处理
- OCR 或持久化失败 → 文件移入 `storage/failed/` + 错误日志
- 成功 → 移入 `storage/archive/`
- 所有异常都会记录日志（不再静默吞掉）

### 4.4 临时文件清理
- PDF 转出的临时图片在处理完成后删除

---

## 5. 数据持久化 (DataPersistenceService)

### 5.1 整体流程

```
OCR 输出 field_code → value
        ↓
autoCorrectFields — 评分 + 贪心最优分配
        ↓
sanitizeFields — 清洗非法字段
        ↓
维度表模糊查询 (fuzzyLookup)
        ↓
INSERT / UPDATE (去重)
```

### 5.2 智能列纠正 (`autoCorrectFields`)

对每个字段值计算"像什么"的评分，用贪心算法找到最优列分配：

| 字段 | 评分规则 |
|------|---------|
| gender | 男/女=100 |
| id_card | 18位数字+X=100, 长度18=40 |
| name | 2-4个汉字=75 |
| degree_name | 包含学/硕/博士=100, Levenshtein匹配=70 |
| province_name | 精确匹配=100, 短名匹配=80, Levenshtein匹配=70 |
| admission_date / graduation_date | 日期格式=100 |
| student_no | 8-12位数字=75, 字母数字6-20=40 |
| exam_no | 9-15位数字=70 |
| class_name | 含"班"=60 |

评分最高的 (value, field) 优先匹配，已匹配的字段不重复分配。评分≤0 的值保留在原位。

### 5.3 字段清洗 (`sanitizeFields`)

- id_card 不是 18 位 → 置 null（避免数据库 CHECK 约束违反）

### 5.4 维度表模糊匹配

| 维度 | 匹配顺序 | 自动插入 |
|------|---------|---------|
| 省份 | 精确 → Levenshtein | ❌ 已禁用 |
| 专业 | 精确 → Levenshtein → 校验纯数字 | ✅  |
| 班级 | 精确 → Levenshtein → 校验纯数字 | ✅  |
| 学历 | 精确 → 关键词 → Levenshtein → 校验纯数字 | ✅  |
| 去向 | 精确 → Levenshtein → 校验纯数字 | ✅  |

### 5.5 招生入库 (`saveAdmissionData`)

**student_fact** 去重: `student_no` → `id_card`
- 找到 → UPDATE，没找到 → INSERT

**admission_fact** 去重: `student_no` → `id_card` → `exam_no`
- 找到 → UPDATE，没找到 → INSERT

### 5.6 毕业入库 (`saveGraduationData`)

**graduation_fact** 去重: `student_no` → `id_card`
- 找到 → UPDATE，没找到 → INSERT
- 同步标记 `student_fact.graduated = true`

### 5.7 字段实体映射

```
student_fact:   student_no, name, id_card, gender, province_id, major_id, class_id, admission_date
admission_fact: student_no, exam_no, name, id_card, gender, province_id, major_id, admission_date
graduation_fact: student_no, name, id_card, gender, degree_id, dest_id, graduation_date
```

---

## 6. 文件归档

| 结果 | 位置 | 触发条件 |
|------|------|---------|
| 成功 | `storage/archive/{date}/{type}/` | 数据持久化完成 |
| 失败 | `storage/failed/{date}/{type}/` + `.error.json` | 无有效数据 / 处理异常 / DB异常 |

`StorageService` 通过文件名在 `storage/temp/` 中递归查找文件并移动。

---

## 7. 质量评分 & OCR 日志

- 数据持久化成功后，计算质量评分写入 `quality_score_dim`
- 有警告信息则写入 `ocr_log_dim`
- OCR 日志也可通过 `syncTodayLogs()` 扫描 archive/failed 目录生成
