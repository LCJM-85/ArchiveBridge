package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.mapper.ArchiveFileDimMapper;
import edu.scau.scauarchiveinsight.mapper.OCRLogDimMapper;
import edu.scau.scauarchiveinsight.pojo.ArchiveFileDim;
import edu.scau.scauarchiveinsight.pojo.OCRLogDim;
import edu.scau.scauarchiveinsight.pojo.ProcessingIssue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class OCRLogService {

    private static final Logger log = LoggerFactory.getLogger(OCRLogService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private OCRLogDimMapper ocrLogDimMapper;

    @Autowired
    private ArchiveFileDimMapper archiveFileDimMapper;

    @Autowired
    private CacheService cacheService;

    private static final Path STORAGE_ROOT = Paths.get(System.getProperty("user.dir"), "storage");

    /**
     * 扫描 storage 目录，将当天处理的文件写入日志
     */
    public void syncTodayLogs() {
        LocalDate today = LocalDate.now();
        String dateStr = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        scanDir(STORAGE_ROOT.resolve("archive"), dateStr, "success");
        scanDir(STORAGE_ROOT.resolve("failed"), dateStr, "failed");
        cacheService.evictDashboard();
    }

    private void scanDir(Path root, String dateStr, String status) {
        Path dateDir = root.resolve(dateStr);
        if (!Files.isDirectory(dateDir)) return;

        try (Stream<Path> stream = Files.walk(dateDir)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> !p.getFileName().toString().endsWith(".error.json"))
                  .filter(p -> !p.getFileName().toString().endsWith(".warn.json"))
                  .forEach(dataFile -> {
                      String fileName = dataFile.getFileName().toString();
                      // type: {dateDir}/{type}/{filename} → 取倒数第二段
                      String typePart = dataFile.getNameCount() > 2
                              ? dataFile.getName(dataFile.getNameCount() - 2).toString() : "unknown";

                      OCRLogDim existing = findLatestByFileName(fileName).orElse(null);
                      // 已有任务由处理线程主动写入终态；扫描不能提前结束任务或覆盖警告/取消。
                      if (existing != null) return;

                      // 从 archive_file_dim 查找 fileId
                      Integer fileId = null;
                      ArchiveFileDim archiveFile = archiveFileDimMapper.selectOne(
                              new LambdaQueryWrapper<ArchiveFileDim>()
                                      .eq(ArchiveFileDim::getFileName, fileName));
                      if (archiveFile != null) {
                          fileId = archiveFile.getFileId();
                      }

                      OCRLogDim log = new OCRLogDim();
                      log.setFileId(fileId);
                      log.setFileName(fileName);
                      log.setFileType(typePart);
                      log.setRecognizeStatus(status);
                      log.setRecognizeTime(LocalDateTime.now());
                      log.setMessage(null);
                      log.setUpdatedAt(LocalDateTime.now());

                      // 尝试读取对应的错误侧边文件
                      Path errorFile = dataFile.resolveSibling(dataFile.getFileName() + ".error.json");
                      if (Files.exists(errorFile)) {
                          try {
                              log.setErrorMessage(Files.readString(errorFile));
                          } catch (IOException ignored) {}
                      }

                      ocrLogDimMapper.insert(log);
                  });
        } catch (IOException ignored) {
        }
    }

    /**
     * 手动写入一条日志
     */
    public void addLog(Integer fileId, String fileName, String fileType, String status, String errorMessage) {
        ProcessingIssue issue = "warning".equals(status)
                ? ProcessingIssue.warning(errorMessage)
                : ProcessingIssue.error(errorMessage);
        addIssues(fileId, fileName, fileType, status, List.of(issue));
    }

    public void addIssues(Integer fileId, String fileName, String fileType, String status,
                          List<ProcessingIssue> issues) {
        OCRLogDim log = findLatestByFileName(fileName).orElseGet(OCRLogDim::new);
        if ("cancelled".equals(log.getRecognizeStatus())) return;
        log.setFileId(fileId);
        log.setFileName(fileName);
        log.setFileType(fileType);
        log.setRecognizeStatus(status);
        log.setRecognizeTime(LocalDateTime.now());
        log.setErrorMessage(encodeIssues(issues));
        log.setIssues(issues);
        log.setMessage(null);
        log.setUpdatedAt(LocalDateTime.now());
        if (log.getLogId() == null) ocrLogDimMapper.insert(log);
        else ocrLogDimMapper.updateById(log);
        cacheService.evictDashboard();
    }

    public void addWarningMessages(Integer fileId, String fileName, String fileType,
                                   Collection<String> messages) {
        List<ProcessingIssue> issues = messages.stream()
                .filter(message -> message != null && !message.isBlank())
                .map(ProcessingIssue::warning)
                .toList();
        if (!issues.isEmpty()) addIssues(fileId, fileName, fileType, "warning", issues);
    }

    /** 在已有数据提示后追加系统级提示，避免评分异常覆盖字段问题。 */
    public void appendWarningMessages(Integer fileId, String fileName, String fileType,
                                      Collection<String> messages) {
        List<ProcessingIssue> issues = new ArrayList<>();
        findLatestByFileName(fileName).map(this::hydrateIssues)
                .map(OCRLogDim::getIssues)
                .ifPresent(issues::addAll);
        messages.stream()
                .filter(message -> message != null && !message.isBlank())
                .map(ProcessingIssue::warning)
                .forEach(issues::add);
        if (!issues.isEmpty()) addIssues(fileId, fileName, fileType, "warning", issues);
    }

    public boolean tryAddWarningMessages(Integer fileId, String fileName, String fileType,
                                         Collection<String> messages) {
        try {
            addWarningMessages(fileId, fileName, fileType, messages);
            return true;
        } catch (Exception e) {
            log.error("数据提示写入失败: fileName={}", fileName, e);
            return false;
        }
    }

    public boolean tryAppendWarningMessages(Integer fileId, String fileName, String fileType,
                                            Collection<String> messages) {
        try {
            appendWarningMessages(fileId, fileName, fileType, messages);
            return true;
        } catch (Exception e) {
            log.error("数据提示追加失败: fileName={}", fileName, e);
            return false;
        }
    }

    public boolean tryAddMappingIssues(Integer fileId, String fileName, String fileType,
                                       List<Map<String, Object>> mappingErrors) {
        try {
            addMappingIssues(fileId, fileName, fileType, mappingErrors);
            return true;
        } catch (Exception e) {
            log.error("字段问题写入失败: fileName={}", fileName, e);
            return false;
        }
    }

    /** 将元数据校验结果拆成可逐项展示的问题。 */
    public void addMappingIssues(Integer fileId, String fileName, String fileType,
                                 List<Map<String, Object>> mappingErrors) {
        List<ProcessingIssue> issues = new ArrayList<>();
        for (Map<String, Object> error : mappingErrors) {
            Integer row = toInteger(error.get("row"));
            Object rawMessages = error.get("messages");
            Object singleMessage = error.containsKey("message") ? error.get("message") : error.get("msg");
            Collection<?> messages = rawMessages instanceof Collection<?> values
                    ? values : List.of(singleMessage != null ? singleMessage : "数据校验异常");
            for (Object value : messages) {
                String message = String.valueOf(value);
                String field = extractField(message);
                String code = message.startsWith("未匹配的列") ? "UNMATCHED_COLUMN" : "FIELD_VALIDATION";
                issues.add(new ProcessingIssue("warning", code, row, field, message, null));
            }
        }
        if (!issues.isEmpty()) addIssues(fileId, fileName, fileType, "warning", issues);
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) return number.intValue();
        try {
            return value == null ? null : Integer.valueOf(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String extractField(String message) {
        int start = message.indexOf('[');
        int end = message.indexOf(']', start + 1);
        return start >= 0 && end > start ? message.substring(start + 1, end) : null;
    }

    public Integer createProcessingLog(String fileName, String fileType) {
        OCRLogDim log = new OCRLogDim();
        log.setFileName(fileName);
        log.setFileType(fileType);
        log.setRecognizeStatus("processing");
        log.setRecognizeTime(LocalDateTime.now());
        log.setMessage("等待处理");
        log.setUpdatedAt(LocalDateTime.now());
        ocrLogDimMapper.insert(log);
        cacheService.evictDashboard();
        return log.getLogId();
    }

    public void updateMessage(Integer logId, String message) {
        ocrLogDimMapper.update(null, new LambdaUpdateWrapper<OCRLogDim>()
                .eq(OCRLogDim::getLogId, logId)
                .eq(OCRLogDim::getRecognizeStatus, "processing")
                .set(OCRLogDim::getMessage, message)
                .set(OCRLogDim::getUpdatedAt, LocalDateTime.now()));
    }

    public void markFailed(Integer logId, String errorMessage) {
        ocrLogDimMapper.update(null, new LambdaUpdateWrapper<OCRLogDim>()
                .eq(OCRLogDim::getLogId, logId)
                .ne(OCRLogDim::getRecognizeStatus, "cancelled")
                .set(OCRLogDim::getRecognizeStatus, "failed")
                .set(OCRLogDim::getMessage, null)
                .set(OCRLogDim::getErrorMessage, encodeIssues(List.of(ProcessingIssue.error(errorMessage))))
                .set(OCRLogDim::getUpdatedAt, LocalDateTime.now()));
    }

    /** 按任务主动结束，不依赖当天目录扫描，也不覆盖已有警告或取消结果。 */
    public void finishProcessing(Integer logId, String fileName) {
        ArchiveFileDim file = archiveFileDimMapper.selectOne(new LambdaQueryWrapper<ArchiveFileDim>()
                .eq(ArchiveFileDim::getFileName, fileName));
        ocrLogDimMapper.update(null, new LambdaUpdateWrapper<OCRLogDim>()
                .eq(OCRLogDim::getLogId, logId)
                .eq(OCRLogDim::getRecognizeStatus, "processing")
                .set(OCRLogDim::getFileId, file == null ? null : file.getFileId())
                .set(OCRLogDim::getRecognizeStatus, "success")
                .set(OCRLogDim::getMessage, null)
                .set(OCRLogDim::getUpdatedAt, LocalDateTime.now()));
        cacheService.evictDashboard();
    }

    public boolean markCancelled(Integer logId) {
        return ocrLogDimMapper.update(null, new LambdaUpdateWrapper<OCRLogDim>()
                .eq(OCRLogDim::getLogId, logId)
                .eq(OCRLogDim::getRecognizeStatus, "processing")
                .set(OCRLogDim::getRecognizeStatus, "cancelled")
                .set(OCRLogDim::getMessage, "用户已取消")
                .set(OCRLogDim::getErrorMessage, null)
                .set(OCRLogDim::getUpdatedAt, LocalDateTime.now())) > 0;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void markInterruptedTasksAfterRestart() {
        ocrLogDimMapper.update(null, new LambdaUpdateWrapper<OCRLogDim>()
                .eq(OCRLogDim::getRecognizeStatus, "processing")
                .set(OCRLogDim::getRecognizeStatus, "failed")
                .set(OCRLogDim::getMessage, null)
                .set(OCRLogDim::getErrorMessage, "服务重启导致任务中断")
                .set(OCRLogDim::getUpdatedAt, LocalDateTime.now()));
    }

    public Optional<OCRLogDim> findLatestByFileName(String fileName) {
        return Optional.ofNullable(ocrLogDimMapper.selectOne(
                new LambdaQueryWrapper<OCRLogDim>()
                        .eq(OCRLogDim::getFileName, fileName)
                        .orderByDesc(OCRLogDim::getLogId)
                        .last("LIMIT 1")));
    }

    public OCRLogDim getById(Integer logId) {
        return hydrateIssues(ocrLogDimMapper.selectById(logId));
    }

    public void removeById(Integer logId) {
        // 查出日志记录，获取文件名
        OCRLogDim log = ocrLogDimMapper.selectById(logId);
        if (log != null && log.getFileName() != null) {
            deleteStorageFile(log.getFileName());
        }
        ocrLogDimMapper.deleteById(logId);
        cacheService.evictDashboard();
    }

    /**
     * 在 storage 目录中递归查找并删除文件及侧边文件
     */
    private void deleteStorageFile(String fileName) {
        Path storageDir = Paths.get(System.getProperty("user.dir"), "storage");
        try (Stream<Path> stream = Files.walk(storageDir)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.getFileName().toString().equals(fileName)
                          || p.getFileName().toString().equals(fileName + ".error.json")
                          || p.getFileName().toString().equals(fileName + ".warn.json"))
                  .forEach(p -> {
                      try {
                          Files.deleteIfExists(p);
                      } catch (IOException ignored) {}
                  });
        } catch (IOException ignored) {}
    }

    /**
     * 获取今日日志
     */
    public List<OCRLogDim> getTodayLogs() {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<OCRLogDim> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(OCRLogDim::getRecognizeTime, today.atStartOfDay(), today.atTime(LocalTime.MAX))
               .orderByDesc(OCRLogDim::getRecognizeTime);
        List<OCRLogDim> logs = ocrLogDimMapper.selectList(wrapper);
        logs.forEach(this::hydrateIssues);
        return logs;
    }

    /**
     * 分页查询历史日志
     */
    public IPage<OCRLogDim> getHistory(int current, int size) {
        Page<OCRLogDim> page = new Page<>(current, size);
        LambdaQueryWrapper<OCRLogDim> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(OCRLogDim::getRecognizeTime);
        IPage<OCRLogDim> result = ocrLogDimMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::hydrateIssues);
        return result;
    }

    private String encodeIssues(List<ProcessingIssue> issues) {
        try {
            return OBJECT_MAPPER.writeValueAsString(issues == null ? List.of() : issues);
        } catch (Exception e) {
            throw new IllegalStateException("问题信息序列化失败", e);
        }
    }

    private OCRLogDim hydrateIssues(OCRLogDim log) {
        if (log == null) return null;
        String raw = log.getErrorMessage();
        if (raw == null || raw.isBlank()) {
            log.setIssues(List.of());
            return log;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(raw);
            if (root.isArray()) {
                log.setIssues(OBJECT_MAPPER.convertValue(root, new TypeReference<List<ProcessingIssue>>() {}));
            } else if (root.isObject() && root.hasNonNull("message")) {
                log.setIssues(List.of(levelIssue(log.getRecognizeStatus(), root.get("message").asText())));
            } else {
                log.setIssues(List.of(levelIssue(log.getRecognizeStatus(), raw)));
            }
        } catch (Exception ignored) {
            // 兼容改造前已经写入数据库的普通文本。
            log.setIssues(List.of(levelIssue(log.getRecognizeStatus(), raw)));
        }
        return log;
    }

    private ProcessingIssue levelIssue(String status, String message) {
        return "warning".equals(status) ? ProcessingIssue.warning(message) : ProcessingIssue.error(message);
    }
}
