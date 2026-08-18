package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.scauarchiveinsight.mapper.ArchiveFileDimMapper;
import edu.scau.scauarchiveinsight.mapper.OCRLogDimMapper;
import edu.scau.scauarchiveinsight.pojo.ArchiveFileDim;
import edu.scau.scauarchiveinsight.pojo.OCRLogDim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class OCRLogService {

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
                  .forEach(dataFile -> {
                      String fileName = dataFile.getFileName().toString();
                      // type: {dateDir}/{type}/{filename} → 取倒数第二段
                      String typePart = dataFile.getNameCount() > 2
                              ? dataFile.getName(dataFile.getNameCount() - 2).toString() : "unknown";

                      OCRLogDim existing = findLatestByFileName(fileName).orElse(null);
                      if (existing != null && !"processing".equals(existing.getRecognizeStatus())) return;

                      // 从 archive_file_dim 查找 fileId
                      Integer fileId = null;
                      ArchiveFileDim archiveFile = archiveFileDimMapper.selectOne(
                              new LambdaQueryWrapper<ArchiveFileDim>()
                                      .eq(ArchiveFileDim::getFileName, fileName));
                      if (archiveFile != null) {
                          fileId = archiveFile.getFileId();
                      }

                      OCRLogDim log = existing != null ? existing : new OCRLogDim();
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

                      if (existing == null) ocrLogDimMapper.insert(log);
                      else ocrLogDimMapper.updateById(log);
                  });
        } catch (IOException ignored) {
        }
    }

    /**
     * 手动写入一条日志
     */
    public void addLog(Integer fileId, String fileName, String fileType, String status, String errorMessage) {
        OCRLogDim log = findLatestByFileName(fileName).orElseGet(OCRLogDim::new);
        if ("cancelled".equals(log.getRecognizeStatus())) return;
        log.setFileId(fileId);
        log.setFileName(fileName);
        log.setFileType(fileType);
        log.setRecognizeStatus(status);
        log.setRecognizeTime(LocalDateTime.now());
        log.setErrorMessage(errorMessage);
        log.setMessage(null);
        log.setUpdatedAt(LocalDateTime.now());
        if (log.getLogId() == null) ocrLogDimMapper.insert(log);
        else ocrLogDimMapper.updateById(log);
        cacheService.evictDashboard();
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
                .set(OCRLogDim::getErrorMessage, errorMessage)
                .set(OCRLogDim::getUpdatedAt, LocalDateTime.now()));
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
        return ocrLogDimMapper.selectById(logId);
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
        return ocrLogDimMapper.selectList(wrapper);
    }

    /**
     * 分页查询历史日志
     */
    public IPage<OCRLogDim> getHistory(int current, int size) {
        Page<OCRLogDim> page = new Page<>(current, size);
        LambdaQueryWrapper<OCRLogDim> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(OCRLogDim::getRecognizeTime);
        return ocrLogDimMapper.selectPage(page, wrapper);
    }
}
