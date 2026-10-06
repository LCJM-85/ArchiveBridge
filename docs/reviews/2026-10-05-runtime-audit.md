# 运行问题审查与数据库、Redis 验证

审查日期：2026-10-05。范围为系统运行和功能行为，不包含安全审查。本轮仅新增测试和记录，没有修改业务实现。

> 本文是修复前的审查快照。2026-10-06 已按用户讨论修复，当前结果见 [修复与验证记录](2026-10-06-runtime-repairs.md)。其中来源字段 null 属于非法配置下的健壮性问题，不应视为正常数据库必现故障；实查数据库已有 NOT NULL，且现有配置无 null/空白值。原复现测试已改为验证正确行为的回归测试。

## 验证结果

- 在 Machine 环境变量找到 `DB_PASS`，当前启动的终端没有继承该值。仅对测试子进程加载配置，未打印或保存密码。`GLM_API_KEY` 同样需要加载；补齐后 AI 助手 Python 启动日志确认服务启动成功。未调用付费模型验证对话效果。
- 完整 `mvn -q test`：51 项，0 失败、0 错误、0 跳过，启用 `RUN_REDIS_INTEGRATION=true` 和 `RUN_RUNTIME_INTEGRATION=true`。
- RedisLiveIntegrationTest：真实 Redis JSON 缓存读写、验证码 getAndDelete 原子消费通过；测试只写随机命名测试键，结束后清理。
- RuntimeReadOnlyIntegrationTest：真实 PostgreSQL 的招生、毕业、学籍分页，五类趋势、地图查询、报表、培养路径查询通过。
- 文件级事务额外验证：直接调用 Spring 代理的 saveFileData，第一行写入，第二行空记录触发异常。异常后查询 archive_file_dim、student_fact、admission_fact，均无对应测试记录。测试事务已回滚，未留下业务测试数据。该项在最后调整为直接调用服务后单独重跑，4/4 通过。
- RuntimeAuditReproductionTest 的三项断言证明当前缺陷存在；测试通过不表示这些缺陷已修复。
- 不代表所有页面、实际 OCR 模型识别、预测算法与外部 AI 请求全部验证完成。

## 已复现的问题

### 1. CSV/Excel 上传附加信息补入失败（P1）

触发：文件没有省份、入学日期、培养层次列，用户在上传页选定这些信息。

MetaDataMappingService.java:54 为缺失字段写入空字符串，CSVProcessor.java:108、113、118 使用 putIfAbsent；空字符串已占用键，因此用户选择不会补入。Excel 同样如此。结果是入库数据仍缺省份、日期、培养层次，影响筛选和统计。当前复现测试覆盖 CSV 路径；Excel 同源逻辑由代码核对。

建议：只有已有值非空时保留原值；null、空字符串和纯空白均使用上传补充值。

### 2. 元数据规则中的 null 来源字段造成导入崩溃（P1，条件性）

触发：任意参与映射的规则 sourceField 为 null，即使实际输入能通过 fieldName 匹配。

MetaDataMappingService.java:85、94 使用 List.of(sourceField, fieldName, fieldCode)，List.of 不允许 null；循环内部的 key != null 检查执行不到。测试确认抛出 NullPointerException。这里证明的是允许出现 null 时的代码缺陷，不宣称当前数据库一定已有这类规则。

建议：构造可容纳 null 的候选集合并过滤，或显式按非空候选字段逐一匹配。

### 3. .tif 文件可上传但处理分支拒绝（P1）

StorageService.java:22 和前端允许 .tif，ArchiveUploadController.java:109 的图片分支只列 tiff，漏掉 tif。

复现调用 processFile(..., "tif", ...) 后触发“不支持的文件类型: tif”，ImageProcessor 未执行。

建议：统一扩展名集合，避免上传校验与处理分发分别维护不同列表。

## 代码确认的边界缺陷

### 4. 跨零点完成的任务可能持续显示处理中（P2）

文件按上传日期落盘，归档时保持该目录；OCRLogService.java:57–61 仅扫描当前日期目录。一个昨天上传、今天完成且未显式写入 warning 的成功任务，控制器结束时同步扫描不到昨天目录中的文件，因此日志不会由 processing 更新为 success。尚未等待真实零点进行端到端复现。

建议：处理结束时按 taskId 显式更新终态，目录扫描只用于补偿。

### 5. 首页专业分布固定统计 2020—2025（P2）

DashboardService.java:59 硬编码统计年份。2026 年及后续数据不会进入专业分布，而总人数和年度趋势采用不同时间范围，产生不一致。更早档案也被排除。代码可直接确认这一范围限制，是否属于预期统计口径需结合产品要求；页面当前没有明确告知该范围。

建议：采用与首页其他统计一致的时间范围，或在界面明确显示筛选范围。

## 待并发复现的风险

### 6. 已取消任务仍可能完成入库（P1，竞态风险）

OCRLogController.java:71–77 先把日志设 cancelled，再调用 Future.cancel(true) 并移动文件；DataPersistenceService.saveFileData 与 CSV/Excel 的入库循环没有检查中断或任务取消状态。中断是协作式请求，不会自动取消正在执行的 JDBC 写入。因此取消发生在入库阶段时，有可能日志显示已取消但数据库已经提交，随后文件移动失败。

此项来自调用链和中断行为分析，未进行真实并发取消复现，不将其描述为已观察到的故障。

建议：定义可取消阶段，在事务入口和提交前校验取消状态；进入已提交阶段时，接口不能继续把任务改为“取消成功”。

## 优先级

先处理 1、2、3：改动小且已复现；随后处理任务终态与取消边界，再统一统计口径。

新增测试位置：

- scau-archive-insight/src/test/java/edu/scau/scauarchiveinsight/service/RuntimeAuditReproductionTest.java
- scau-archive-insight/src/test/java/edu/scau/scauarchiveinsight/service/RuntimeReadOnlyIntegrationTest.java
