# 招生趋势分析模块设计文档

## 概述

在现有"可视化分析大屏"下实现招生趋势分析模块，包含 5 个维度的趋势图表，均基于 `admission_fact` 表的聚合查询。

## 后端设计

### API 端点

在 `AdmissionController` 中新增 5 个 GET 端点，基路径 `/api/admission/trend/`：

| 端点 | 返回格式 | 说明 |
|------|---------|------|
| `GET /api/admission/trend/yearly` | `[{year, count}]` | 按年份统计招生总数 |
| `GET /api/admission/trend/major` | `[{year, majorName, count}]` | 按年份+专业统计人数 |
| `GET /api/admission/trend/province` | `[{year, provinceName, count}]` | 按年份+省份统计人数 |
| `GET /api/admission/trend/score` | `[{year, avgScore, maxScore, minScore}]` | 每年录取分数线统计 |
| `GET /api/admission/trend/gender` | `[{year, gender, count}]` | 按年份+性别统计人数 |

所有端点支持可选查询参数 `startYear` 和 `endYear`（Integer），用于筛选年份范围。

统一返回 `R<List<Map<String, Object>>>` 包装。

### Service 层

新增 `TrendAnalysisService`，包含 5 个方法，每个方法执行 MyBatis-Plus 原生 SQL 聚合查询：

#### yearlyTrend(startYear, endYear)
```sql
SELECT EXTRACT(YEAR FROM admission_date) AS year, COUNT(*) AS count
FROM admission_fact
WHERE admission_date BETWEEN ? AND ?
GROUP BY year ORDER BY year
```

#### majorTrend(startYear, endYear)
```sql
SELECT EXTRACT(YEAR FROM f.admission_date) AS year, m.major_name, COUNT(*) AS count
FROM admission_fact f JOIN major_dim m ON f.major_id = m.major_id
WHERE f.admission_date BETWEEN ? AND ?
GROUP BY year, m.major_name ORDER BY year, count DESC
```

#### provinceTrend(startYear, endYear)
```sql
SELECT EXTRACT(YEAR FROM f.admission_date) AS year, p.province_name, COUNT(*) AS count
FROM admission_fact f JOIN province_dim p ON f.province_id = p.province_id
WHERE f.admission_date BETWEEN ? AND ?
GROUP BY year, p.province_name ORDER BY year, count DESC
```

#### scoreTrend(startYear, endYear)
```sql
SELECT EXTRACT(YEAR FROM admission_date) AS year,
       AVG(admission_score) AS avg_score,
       MAX(admission_score) AS max_score,
       MIN(admission_score) AS min_score
FROM admission_fact
WHERE admission_date BETWEEN ? AND ? AND admission_score IS NOT NULL
GROUP BY year ORDER BY year
```

#### genderTrend(startYear, endYear)
```sql
SELECT EXTRACT(YEAR FROM admission_date) AS year, gender, COUNT(*) AS count
FROM admission_fact
WHERE admission_date BETWEEN ? AND ?
GROUP BY year, gender ORDER BY year, gender
```

### Mapper 层

在 `AdmissionFactMapper` 中新增 5 个 `@Select` 注解方法，使用 MyBatis `@MapKey` 或直接返回 `List<Map<String, Object>>`。

### Error Handling

- 数据为空时返回空列表，不做特殊处理
- 年份参数校验：`startYear` ≤ `endYear`，不合法时返回 400

## 前端设计

### 页面结构

`AddmissionTrend.vue`（修正文件名拼写为 `AdmissionTrend.vue`）：

```
┌──────────────────────────────────────────────┐
│  招生趋势分析  [起始年 ▼] [结束年 ▼] [刷新]   │
├───────────────────┬──────────────────────────┤
│  年度招生人数趋势   │  各专业录取人数趋势        │
│  (ECharts 柱状图)  │  (ECharts 堆叠柱状图)     │
├───────────────────┼──────────────────────────┤
│  各省份录取人数趋势  │  录取分数线趋势            │
│  (ECharts 热力图)   │  (ECharts 折线图)        │
├───────────────────┴──────────────────────────┤
│  性别比例趋势                                  │
│  (ECharts 堆叠柱状图)                          │
└──────────────────────────────────────────────┘
```

### 组件拆分

- `AddmissionTrend.vue` — 主页面，包含年份筛选器和 5 个图表卡片
- 图表使用 `echarts` 6.x 的 `init` API，在 `onMounted` 中创建实例，`watch` 数据变化时更新

### 数据流

1. 页面 onMounted → 并发请求 5 个 API
2. 年份筛选变化 → 重新请求所有 API
3. 每个 API 返回后 → 更新对应图表的 option
4. 使用 `window.addEventListener('resize', ...)` 处理图表自适应

### API 层扩展

在 `api/modules/admission.js` 中新增：
```js
export function fetchTrendYearly(params) { return request.get('/api/admission/trend/yearly', { params }) }
export function fetchTrendMajor(params) { return request.get('/api/admission/trend/major', { params }) }
export function fetchTrendProvince(params) { return request.get('/api/admission/trend/province', { params }) }
export function fetchTrendScore(params) { return request.get('/api/admission/trend/score', { params }) }
export function fetchTrendGender(params) { return request.get('/api/admission/trend/gender', { params }) }
```

### ECharts 配置

每个图表的核心配置：

1. **年度招生人数趋势** — 柱状图
   - X 轴：年份，Y 轴：人数
   - 显示数据标签

2. **各专业录取人数趋势** — 堆叠柱状图
   - X 轴：年份，Y 轴：人数
   - 图例：专业名称（限制最多显示前 10 个专业，其余归为"其他"）

3. **各省份录取人数趋势** — 热力图
   - X 轴：年份，Y 轴：省份
   - 颜色梯度表示人数

4. **录取分数线趋势** — 折线图
   - 三条线：最高分、平均分、最低分
   - 图例可切换显示

5. **性别比例趋势** — 堆叠柱状图
   - X 轴：年份，Y 轴：人数
   - 两个系列：男、女

### 数据为空处理

- 显示 "暂无数据" 占位提示
- 使用 ECharts 的 `title` + `graphic` 或自定义空状态覆盖

### 样式

- 遵循现有项目风格（Element Plus Card）
- 2 列网格布局，响应式（大屏 2 列，小屏 1 列）

## 文件变更清单

### 后端
- `scau-archive-insight/.../service/TrendAnalysisService.java` — 新增
- `scau-archive-insight/.../mapper/AdmissionFactMapper.java` — 修改，新增 5 个 @Select 方法
- `scau-archive-insight/.../controller/AdmissionController.java` — 修改，新增 5 个端点

### 前端
- `scau_archive-frontend/src/views/charts/AddmissionTrend.vue` — 重写（文件名保留现有拼写，避免路由断裂）
- `scau_archive-frontend/src/api/modules/admission.js` — 修改，新增 5 个 API 方法

### 其他
- 文档：本文件

## 注意事项

1. `AddmissionTrend.vue` 文件名有拼写错误（Addmission → Admission），但为了不破坏现有路由和引用，保持文件名不变
2. `admission_fact` 当前无数据，API 返回空列表，前端展示空状态
3. ECharts 6.x 使用 `echarts.init()` 方式，需确保 DOM 元素在 `onMounted` 时已渲染
4. 后端查询使用原生 SQL 而非 MyBatis-Plus 的 LambdaQueryWrapper，因为涉及 `EXTRACT(YEAR ...)` 和 JOIN
