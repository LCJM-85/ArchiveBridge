# 招生趋势分析模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 5 trend analysis charts for the admission data module.

**Architecture:** Backend adds 5 aggregation SQL queries via MyBatis `@Select` in a new `TrendAnalysisService`, exposed through `AdmissionController` as REST GET endpoints. Frontend rewrites `AddmissionTrend.vue` with ECharts 6, fetching data via 5 new API functions in `admission.js`.

**Tech Stack:** Java 17 / Spring Boot 3.5 / MyBatis-Plus / PostgreSQL / Vue 3 / ECharts 6 / Element Plus

---

### Task 1: 后端 Mapper — 新增 5 个聚合查询 @Select 方法

**Files:**
- Modify: `scau-archive-insight/src/main/java/edu/scau/scauarchiveinsight/mapper/AdmissionFactMapper.java`

- [ ] **Step 1: Add 5 @Select query methods to AdmissionFactMapper**

Add these methods after the existing `extends BaseMapper<AdmissionFact>`:

```java
package edu.scau.scauarchiveinsight.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.scau.scauarchiveinsight.pojo.AdmissionFact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdmissionFactMapper extends BaseMapper<AdmissionFact> {

    @Select("<script>" +
            "SELECT EXTRACT(YEAR FROM admission_date)::int AS year, COUNT(*) AS count " +
            "FROM admission_fact " +
            "WHERE admission_date IS NOT NULL " +
            "<if test='startDate != null'>AND admission_date &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND admission_date &lt;= #{endDate}</if> " +
            "GROUP BY year ORDER BY year" +
            "</script>")
    List<Map<String, Object>> yearlyTrend(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @Select("<script>" +
            "SELECT EXTRACT(YEAR FROM f.admission_date)::int AS year, m.major_name AS majorName, COUNT(*)::int AS count " +
            "FROM admission_fact f " +
            "LEFT JOIN major_dim m ON f.major_id = m.major_id " +
            "WHERE f.admission_date IS NOT NULL " +
            "<if test='startDate != null'>AND f.admission_date &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND f.admission_date &lt;= #{endDate}</if> " +
            "GROUP BY year, m.major_name ORDER BY year, count DESC" +
            "</script>")
    List<Map<String, Object>> majorTrend(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Select("<script>" +
            "SELECT EXTRACT(YEAR FROM f.admission_date)::int AS year, p.province_name AS provinceName, COUNT(*)::int AS count " +
            "FROM admission_fact f " +
            "LEFT JOIN province_dim p ON f.province_id = p.province_id " +
            "WHERE f.admission_date IS NOT NULL " +
            "<if test='startDate != null'>AND f.admission_date &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND f.admission_date &lt;= #{endDate}</if> " +
            "GROUP BY year, p.province_name ORDER BY year, count DESC" +
            "</script>")
    List<Map<String, Object>> provinceTrend(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    @Select("<script>" +
            "SELECT EXTRACT(YEAR FROM admission_date)::int AS year, " +
            "AVG(admission_score)::int AS avgScore, " +
            "MAX(admission_score)::int AS maxScore, " +
            "MIN(admission_score)::int AS minScore " +
            "FROM admission_fact " +
            "WHERE admission_date IS NOT NULL AND admission_score IS NOT NULL " +
            "<if test='startDate != null'>AND admission_date &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND admission_date &lt;= #{endDate}</if> " +
            "GROUP BY year ORDER BY year" +
            "</script>")
    List<Map<String, Object>> scoreTrend(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Select("<script>" +
            "SELECT EXTRACT(YEAR FROM admission_date)::int AS year, gender, COUNT(*)::int AS count " +
            "FROM admission_fact " +
            "WHERE admission_date IS NOT NULL " +
            "<if test='startDate != null'>AND admission_date &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND admission_date &lt;= #{endDate}</if> " +
            "GROUP BY year, gender ORDER BY year, gender" +
            "</script>")
    List<Map<String, Object>> genderTrend(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}
```

Note: The `<script>` wrapper enables MyBatis dynamic SQL. `&gt;=` and `&lt;=` are XML-escaped `>=` and `<=`. This is required inside the annotation string.

- [ ] **Step 2: Verify compilation**

Run: `JAVA_HOME="D:/java/jdk-21.0.5" ./mvnw clean compile -q`
Expected: BUILD SUCCESS

---

### Task 2: 后端 Service — 新增 TrendAnalysisService

**Files:**
- Create: `scau-archive-insight/src/main/java/edu/scau/scauarchiveinsight/service/TrendAnalysisService.java`

- [ ] **Step 1: Create TrendAnalysisService**

```java
package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.mapper.AdmissionFactMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class TrendAnalysisService {

    @Autowired
    private AdmissionFactMapper admissionFactMapper;

    public List<Map<String, Object>> yearlyTrend(Integer startYear, Integer endYear) {
        LocalDate startDate = startYear != null ? LocalDate.of(startYear, 1, 1) : null;
        LocalDate endDate = endYear != null ? LocalDate.of(endYear, 12, 31) : null;
        return admissionFactMapper.yearlyTrend(startDate, endDate);
    }

    public List<Map<String, Object>> majorTrend(Integer startYear, Integer endYear) {
        LocalDate startDate = startYear != null ? LocalDate.of(startYear, 1, 1) : null;
        LocalDate endDate = endYear != null ? LocalDate.of(endYear, 12, 31) : null;
        return admissionFactMapper.majorTrend(startDate, endDate);
    }

    public List<Map<String, Object>> provinceTrend(Integer startYear, Integer endYear) {
        LocalDate startDate = startYear != null ? LocalDate.of(startYear, 1, 1) : null;
        LocalDate endDate = endYear != null ? LocalDate.of(endYear, 12, 31) : null;
        return admissionFactMapper.provinceTrend(startDate, endDate);
    }

    public List<Map<String, Object>> scoreTrend(Integer startYear, Integer endYear) {
        LocalDate startDate = startYear != null ? LocalDate.of(startYear, 1, 1) : null;
        LocalDate endDate = endYear != null ? LocalDate.of(endYear, 12, 31) : null;
        return admissionFactMapper.scoreTrend(startDate, endDate);
    }

    public List<Map<String, Object>> genderTrend(Integer startYear, Integer endYear) {
        LocalDate startDate = startYear != null ? LocalDate.of(startYear, 1, 1) : null;
        LocalDate endDate = endYear != null ? LocalDate.of(endYear, 12, 31) : null;
        return admissionFactMapper.genderTrend(startDate, endDate);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `JAVA_HOME="D:/java/jdk-21.0.5" ./mvnw clean compile -q`
Expected: BUILD SUCCESS

---

### Task 3: 后端 Controller — 新增 5 个趋势 API 端点

**Files:**
- Modify: `scau-archive-insight/src/main/java/edu/scau/scauarchiveinsight/controller/AdmissionController.java`

- [ ] **Step 1: Wire TrendAnalysisService into AdmissionController + add 5 endpoints**

Existing imports remain. Add after `AdmissionService admissionService`:

```java
@Autowired
private TrendAnalysisService trendAnalysisService;
```

Add these methods before the class closing brace:

```java
@GetMapping("/trend/yearly")
public R<List<Map<String, Object>>> yearlyTrend(
        @RequestParam(required = false) Integer startYear,
        @RequestParam(required = false) Integer endYear) {
    if (startYear != null && endYear != null && startYear > endYear) {
        return R.error("startYear must not be greater than endYear");
    }
    return R.ok(trendAnalysisService.yearlyTrend(startYear, endYear));
}

@GetMapping("/trend/major")
public R<List<Map<String, Object>>> majorTrend(
        @RequestParam(required = false) Integer startYear,
        @RequestParam(required = false) Integer endYear) {
    if (startYear != null && endYear != null && startYear > endYear) {
        return R.error("startYear must not be greater than endYear");
    }
    return R.ok(trendAnalysisService.majorTrend(startYear, endYear));
}

@GetMapping("/trend/province")
public R<List<Map<String, Object>>> provinceTrend(
        @RequestParam(required = false) Integer startYear,
        @RequestParam(required = false) Integer endYear) {
    if (startYear != null && endYear != null && startYear > endYear) {
        return R.error("startYear must not be greater than endYear");
    }
    return R.ok(trendAnalysisService.provinceTrend(startYear, endYear));
}

@GetMapping("/trend/score")
public R<List<Map<String, Object>>> scoreTrend(
        @RequestParam(required = false) Integer startYear,
        @RequestParam(required = false) Integer endYear) {
    if (startYear != null && endYear != null && startYear > endYear) {
        return R.error("startYear must not be greater than endYear");
    }
    return R.ok(trendAnalysisService.scoreTrend(startYear, endYear));
}

@GetMapping("/trend/gender")
public R<List<Map<String, Object>>> genderTrend(
        @RequestParam(required = false) Integer startYear,
        @RequestParam(required = false) Integer endYear) {
    if (startYear != null && endYear != null && startYear > endYear) {
        return R.error("startYear must not be greater than endYear");
    }
    return R.ok(trendAnalysisService.genderTrend(startYear, endYear));
}
```

Also add the import for `TrendAnalysisService` and `List`:
```java
import edu.scau.scauarchiveinsight.service.TrendAnalysisService;
import java.util.List;
```

- [ ] **Step 2: Verify compilation**

Run: `JAVA_HOME="D:/java/jdk-21.0.5" ./mvnw clean compile -q`
Expected: BUILD SUCCESS

---

### Task 4: 前端 API 层 — 新增趋势数据调用接口

**Files:**
- Modify: `scau_archive-frontend/src/api/modules/admission.js`

- [ ] **Step 1: Add 5 API functions**

Add after the existing `fetchMajors` export:

```js
export function fetchTrendYearly(params) {
  return request.get('/api/admission/trend/yearly', { params })
}

export function fetchTrendMajor(params) {
  return request.get('/api/admission/trend/major', { params })
}

export function fetchTrendProvince(params) {
  return request.get('/api/admission/trend/province', { params })
}

export function fetchTrendScore(params) {
  return request.get('/api/admission/trend/score', { params })
}

export function fetchTrendGender(params) {
  return request.get('/api/admission/trend/gender', { params })
}
```

---

### Task 5: 前端页面 — 重写 AddmissionTrend.vue

**Files:**
- Write: `scau_archive-frontend/src/views/charts/AddmissionTrend.vue`

- [ ] **Step 1: Rewrite AddmissionTrend.vue with ECharts 6**

Full file content:

```vue
<template>
  <div class="page-wrapper">
    <!-- 年份筛选栏 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="filter-left">
          <el-icon size="18" color="var(--color-primary)"><TrendCharts /></el-icon>
          <span class="filter-title">招生趋势分析</span>
        </div>
        <div class="filter-right">
          <el-select v-model="startYear" placeholder="起始年" clearable style="width:120px;margin-right:8px">
            <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
          </el-select>
          <el-select v-model="endYear" placeholder="结束年" clearable style="width:120px;margin-right:12px">
            <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
          </el-select>
          <el-button type="primary" :loading="loading" @click="fetchData">刷新</el-button>
        </div>
      </div>
    </el-card>

    <!-- 图表网格 -->
    <div class="chart-grid">
      <!-- 1. 年度招生人数趋势 -->
      <el-card shadow="never" class="chart-card">
        <template #header><span>年度招生人数趋势</span></template>
        <div ref="yearlyChartRef" class="chart-container"></div>
      </el-card>

      <!-- 2. 各专业录取人数趋势 -->
      <el-card shadow="never" class="chart-card">
        <template #header><span>各专业录取人数趋势</span></template>
        <div ref="majorChartRef" class="chart-container"></div>
      </el-card>

      <!-- 3. 各省份录取人数趋势 -->
      <el-card shadow="never" class="chart-card">
        <template #header><span>各省份录取人数趋势</span></template>
        <div ref="provinceChartRef" class="chart-container"></div>
      </el-card>

      <!-- 4. 录取分数线趋势 -->
      <el-card shadow="never" class="chart-card">
        <template #header><span>录取分数线趋势</span></template>
        <div ref="scoreChartRef" class="chart-container"></div>
      </el-card>

      <!-- 5. 性别比例趋势 (占满整行) -->
      <el-card shadow="never" class="chart-card chart-card-full">
        <template #header><span>性别比例趋势</span></template>
        <div ref="genderChartRef" class="chart-container"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  fetchTrendYearly,
  fetchTrendMajor,
  fetchTrendProvince,
  fetchTrendScore,
  fetchTrendGender,
} from '@/api/modules/admission'

// ---- 筛选状态 ----
const startYear = ref(null)
const endYear = ref(null)
const loading = ref(false)

// 年份选项（近 20 年）
const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 20 }, (_, i) => currentYear - i)

// ---- 图表容器 refs ----
const yearlyChartRef = ref(null)
const majorChartRef = ref(null)
const provinceChartRef = ref(null)
const scoreChartRef = ref(null)
const genderChartRef = ref(null)

// ---- ECharts 实例 ----
const chartInstances = reactive({})

// ---- 获取数据 ----
async function fetchData() {
  loading.value = true
  const params = {}
  if (startYear.value) params.startYear = startYear.value
  if (endYear.value) params.endYear = endYear.value

  try {
    const [yearly, major, province, score, gender] = await Promise.all([
      fetchTrendYearly(params),
      fetchTrendMajor(params),
      fetchTrendProvince(params),
      fetchTrendScore(params),
      fetchTrendGender(params),
    ])
    updateYearlyChart(yearly.data || [])
    updateMajorChart(major.data || [])
    updateProvinceChart(province.data || [])
    updateScoreChart(score.data || [])
    updateGenderChart(gender.data || [])
  } catch (e) {
    console.error('获取趋势数据失败:', e)
  } finally {
    loading.value = false
  }
}

// ---- 各图表更新函数 ----

function updateYearlyChart(data) {
  const chart = getOrCreateChart('yearly', yearlyChartRef.value)
  if (!chart) return
  if (!data.length) {
    chart.clear()
    chart.setOption(getEmptyOption('暂无年度招生数据'))
    return
  }
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, bottom: 40, top: 20 },
    xAxis: { type: 'category', data: data.map(d => String(d.year)), axisLabel: { fontSize: 12 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: data.map(d => d.count),
      itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', fontSize: 11 },
    }],
  })
}

function updateMajorChart(data) {
  const chart = getOrCreateChart('major', majorChartRef.value)
  if (!chart) return
  if (!data.length) {
    chart.clear()
    chart.setOption(getEmptyOption('暂无专业数据'))
    return
  }

  const years = [...new Set(data.map(d => d.year))].sort()
  const majorTotals = {}
  data.forEach(d => { majorTotals[d.majorName || '未知'] = (majorTotals[d.majorName || '未知'] || 0) + d.count })
  const topMajors = Object.entries(majorTotals)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10)
    .map(([name]) => name)

  const colors = ['#409eff','#67c23a','#e6a23c','#f56c6c','#909399','#b37feb','#36cfc9','#ff85c0','#ffc53d','#5cdbd3']

  const series = topMajors.map((name, i) => ({
    name,
    type: 'bar',
    stack: 'total',
    itemStyle: { color: colors[i % colors.length] },
    data: years.map(year => {
      const item = data.find(d => d.year === year && (d.majorName || '未知') === name)
      return item ? item.count : 0
    }),
  }))

  // "其他" 系列
  const otherData = years.map(year => {
    const yearTotal = data.filter(d => d.year === year).reduce((s, d) => s + d.count, 0)
    const topTotal = topMajors.reduce((s, name) => {
      const item = data.find(d => d.year === year && (d.majorName || '未知') === name)
      return s + (item ? item.count : 0)
    }, 0)
    return Math.max(0, yearTotal - topTotal)
  })
  if (otherData.some(v => v > 0)) {
    series.push({ name: '其他', type: 'bar', stack: 'total', itemStyle: { color: '#dcdfe6' }, data: otherData })
  }

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { type: 'scroll', bottom: 0, fontSize: 11 },
    grid: { left: 60, right: 20, bottom: 50, top: 20 },
    xAxis: { type: 'category', data: years.map(String), axisLabel: { fontSize: 12 } },
    yAxis: { type: 'value', minInterval: 1 },
    series,
  })
}

function updateProvinceChart(data) {
  const chart = getOrCreateChart('province', provinceChartRef.value)
  if (!chart) return
  if (!data.length) {
    chart.clear()
    chart.setOption(getEmptyOption('暂无省份数据'))
    return
  }

  const years = [...new Set(data.map(d => d.year))].sort()
  const provinces = [...new Set(data.map(d => d.provinceName || '未知'))]
  const heatmapData = data.map(d => [
    years.indexOf(d.year),
    provinces.indexOf(d.provinceName || '未知'),
    d.count,
  ])
  const maxVal = Math.max(...data.map(d => d.count), 1)

  chart.setOption({
    tooltip: {
      position: 'top',
      formatter: p => `年份: ${years[p.data[0]]}<br/>省份: ${provinces[p.data[1]]}<br/>人数: ${p.data[2]}`,
    },
    grid: { left: 80, right: 60, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: years.map(String), splitArea: { show: true } },
    yAxis: { type: 'category', data: provinces, splitArea: { show: true } },
    visualMap: {
      min: 0,
      max: maxVal,
      calculable: true,
      orient: 'vertical',
      right: 0,
      top: 'center',
      inRange: { color: ['#e8f5e9', '#a5d6a7', '#43a047', '#1b5e20'] },
    },
    series: [{
      type: 'heatmap',
      data: heatmapData,
      label: { show: heatmapData.length < 50, fontSize: 10 },
      emphasis: { itemStyle: { shadowBlur: 10 } },
    }],
  })
}

function updateScoreChart(data) {
  const chart = getOrCreateChart('score', scoreChartRef.value)
  if (!chart) return
  if (!data.length) {
    chart.clear()
    chart.setOption(getEmptyOption('暂无分数线数据'))
    return
  }

  const years = data.map(d => String(d.year))
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, fontSize: 11 },
    grid: { left: 60, right: 20, bottom: 40, top: 20 },
    xAxis: { type: 'category', data: years, axisLabel: { fontSize: 12 } },
    yAxis: { type: 'value' },
    series: [
      { name: '最高分', type: 'line', data: data.map(d => d.maxScore), smooth: true, symbol: 'circle', lineStyle: { width: 2 } },
      { name: '平均分', type: 'line', data: data.map(d => d.avgScore), smooth: true, symbol: 'diamond', lineStyle: { width: 2 } },
      { name: '最低分', type: 'line', data: data.map(d => d.minScore), smooth: true, symbol: 'triangle', lineStyle: { width: 2 } },
    ],
  })
}

function updateGenderChart(data) {
  const chart = getOrCreateChart('gender', genderChartRef.value)
  if (!chart) return
  if (!data.length) {
    chart.clear()
    chart.setOption(getEmptyOption('暂无性别数据'))
    return
  }

  const years = [...new Set(data.map(d => d.year))].sort()
  const maleData = years.map(y => { const d = data.find(v => v.year === y && v.gender === '男'); return d ? d.count : 0 })
  const femaleData = years.map(y => { const d = data.find(v => v.year === y && v.gender === '女'); return d ? d.count : 0 })

  chart.setOption({
    tooltip: { trigger: 'axis', formatter: p => {
      const year = p[0].axisValue
      const male = p[0].value
      const female = p[1].value
      const total = male + female
      return `${year}年<br/>男: ${male} (${total ? ((male/total)*100).toFixed(1) : 0}%)<br/>女: ${female} (${total ? ((female/total)*100).toFixed(1) : 0}%)`
    }},
    legend: { bottom: 0, fontSize: 11 },
    grid: { left: 60, right: 20, bottom: 40, top: 20 },
    xAxis: { type: 'category', data: years.map(String), axisLabel: { fontSize: 12 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '男', type: 'bar', stack: 'gender', data: maleData, itemStyle: { color: '#409eff' } },
      { name: '女', type: 'bar', stack: 'gender', data: femaleData, itemStyle: { color: '#f56c6c' } },
    ],
  })
}

// ---- 工具函数 ----

function getOrCreateChart(key, dom) {
  if (!dom) return null
  if (!chartInstances[key]) {
    chartInstances[key] = echarts.init(dom)
  }
  return chartInstances[key]
}

function getEmptyOption(msg) {
  return {
    xAxis: { show: false },
    yAxis: { show: false },
    graphic: {
      type: 'text',
      left: 'center',
      top: 'center',
      style: { text: msg, fill: '#bbb', fontSize: 14 },
    },
  }
}

// ---- 自适应 ----
function handleResize() {
  Object.values(chartInstances).forEach(chart => chart?.resize())
}

watch([yearlyChartRef, majorChartRef, provinceChartRef, scoreChartRef, genderChartRef], () => {
  nextTick(fetchData)
})

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  Object.values(chartInstances).forEach(chart => chart?.dispose())
})
</script>

<style scoped>
.page-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card {
  flex-shrink: 0;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.filter-right {
  display: flex;
  align-items: center;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card-full {
  grid-column: 1 / -1;
}

.chart-container {
  width: 100%;
  height: 360px;
}

@media (max-width: 900px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
```

---

### Task 6: 集成验证

- [ ] **Step 1: 编译后端**

```bash
cd scau-archive-insight
JAVA_HOME="D:/java/jdk-21.0.5" ./mvnw clean compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 2: 启动后端**

```bash
JAVA_HOME="D:/java/jdk-21.0.5" ./mvnw spring-boot:run
```
Expected: starts on port 8080 without errors

- [ ] **Step 3: 验证 API**

```bash
curl -s http://localhost:8080/api/admission/trend/yearly | head -c 200
```
Expected: `{"code":200,"data":[],"msg":"success"}`
(admission_fact 表当前无数据，应返回空数组)

- [ ] **Step 4: 启动前端并验证**

```bash
cd scau_archive-frontend
npm run dev
```
Expected: starts on port 5173. Navigate to "招生趋势分析" in the sidebar → sees 5 chart cards with "暂无数据" states.

- [ ] **Step 5: 提交代码**

```bash
git add -A
git commit -m "feat: implement admission trend analysis module with 5 ECharts charts"
```
