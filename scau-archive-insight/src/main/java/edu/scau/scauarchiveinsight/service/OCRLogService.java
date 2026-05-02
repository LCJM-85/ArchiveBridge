package edu.scau.scauarchiveinsight.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.scau.scauarchiveinsight.mapper.OCRLogDimMapper;
import edu.scau.scauarchiveinsight.pojo.OCRLogDim;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

@Service
public class OCRLogService {

    @Autowired
    private OCRLogDimMapper ocrLogDimMapper;

    private static final Path STORAGE_ROOT = Paths.get(System.getProperty("user.dir"), "storage");

    /**
     * 扫描 storage 目录，将当天处理的文件写入日志
     */
    public void syncTodayLogs() {
        LocalDate today = LocalDate.now();
        String dateStr = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        scanDir(STORAGE_ROOT.resolve("archive"), dateStr, "success");
        scanDir(STORAGE_ROOT.resolve("failed"), dateStr, "failed");
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

                      // 检查是否已记录
                      LambdaQueryWrapper<OCRLogDim> check = new LambdaQueryWrapper<>();
                      check.eq(OCRLogDim::getFileName, fileName)
                           .eq(OCRLogDim::getRecognizeStatus, status);
                      if (ocrLogDimMapper.selectCount(check) > 0) return;

                      OCRLogDim log = new OCRLogDim();
                      log.setFileName(fileName);
                      log.setFileType(typePart);
                      log.setRecognizeStatus(status);
                      log.setRecognizeTime(LocalDateTime.now());

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
    public void addLog(String fileName, String fileType, String status, String errorMessage) {
        OCRLogDim log = new OCRLogDim();
        log.setFileName(fileName);
        log.setFileType(fileType);
        log.setRecognizeStatus(status);
        log.setRecognizeTime(LocalDateTime.now());
        log.setErrorMessage(errorMessage);
        ocrLogDimMapper.insert(log);
    }

    public void removeById(Integer logId) {
        ocrLogDimMapper.deleteById(logId);
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
