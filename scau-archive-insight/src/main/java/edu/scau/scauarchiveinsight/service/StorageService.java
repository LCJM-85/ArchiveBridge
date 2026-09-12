package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class StorageService {

    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS_BY_TYPE = Map.of(
            "image", Set.of("jpg", "jpeg", "png", "bmp", "gif", "tif", "tiff", "webp"),
            "pdf", Set.of("pdf"),
            "excel", Set.of("xls", "xlsx"),
            "csv", Set.of("csv")
    );

    private final Path storageRoot;
    private final Path archiveRoot;
    private final Path failedRoot;

    private final AtomicInteger processingCount = new AtomicInteger(0);

    @Autowired
    private CacheService cacheService;

    public StorageService() {
        this(Paths.get(System.getProperty("user.dir"), "storage"));
    }

    StorageService(Path storageBase) {
        Path normalizedBase = storageBase.toAbsolutePath().normalize();
        this.storageRoot = normalizedBase.resolve("temp");
        this.archiveRoot = normalizedBase.resolve("archive");
        this.failedRoot = normalizedBase.resolve("failed");
    }

    /**
     * 当前处理中的文件数
     */
    public int getProcessingCount() {
        return processingCount.get();
    }

    public Map<String, Object> saveFiles(List<MultipartFile> files, String type) {
        List<Map<String, String>> uploaded = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        Set<String> allowedExtensions = ALLOWED_EXTENSIONS_BY_TYPE.get(type);
        if (allowedExtensions == null) {
            errors.add("不支持的上传类型: " + type);
            return uploadResult(uploaded, errors);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                errors.add("空文件: " + file.getOriginalFilename());
                continue;
            }
            try {
                String extension = extractExtension(file.getOriginalFilename());
                if (!allowedExtensions.contains(extension)) {
                    errors.add("文件类型与上传类型不匹配: " + file.getOriginalFilename());
                    continue;
                }

                String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                Path dir = storageRoot.resolve(dateStr).resolve(type).normalize();
                if (!dir.startsWith(storageRoot)) {
                    errors.add("非法上传路径: " + file.getOriginalFilename());
                    continue;
                }
                Files.createDirectories(dir);

                String filename = buildStoredFileName(file.getOriginalFilename(), extension);
                Path target = dir.resolve(filename).normalize();
                if (!target.startsWith(dir)) {
                    errors.add("非法上传路径: " + file.getOriginalFilename());
                    continue;
                }
                Files.copy(file.getInputStream(), target);

                Map<String, String> info = new HashMap<>();
                info.put("name", file.getOriginalFilename());
                info.put("path", target.toString());
                info.put("size", String.valueOf(file.getSize()));
                uploaded.add(info);
            } catch (IOException e) {
                errors.add("存储失败: " + file.getOriginalFilename() + " - " + e.getMessage());
            }
        }

        processingCount.addAndGet(uploaded.size());
        return uploadResult(uploaded, errors);
    }

    private Map<String, Object> uploadResult(List<Map<String, String>> uploaded, List<String> errors) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", errors.isEmpty());
        result.put("uploaded", uploaded);
        result.put("errors", errors);
        return result;
    }

    private String extractExtension(String originalFilename) {
        String leafName = leafName(originalFilename);
        int dot = leafName.lastIndexOf('.');
        if (dot <= 0 || dot == leafName.length() - 1) return "";
        return leafName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String buildStoredFileName(String originalFilename, String extension) {
        String leafName = leafName(originalFilename);
        int dot = leafName.lastIndexOf('.');
        String baseName = dot > 0 ? leafName.substring(0, dot) : leafName;
        String safeBaseName = baseName.replaceAll("[^\\p{L}\\p{N}._-]", "_");
        if (safeBaseName.isBlank()) safeBaseName = "file";
        int codePointCount = safeBaseName.codePointCount(0, safeBaseName.length());
        if (codePointCount > 80) {
            safeBaseName = safeBaseName.substring(0, safeBaseName.offsetByCodePoints(0, 80));
        }
        return UUID.randomUUID().toString().replace("-", "") + "_" + safeBaseName + "." + extension;
    }

    private String leafName(String originalFilename) {
        if (originalFilename == null) return "file";
        String normalized = originalFilename.replace('\\', '/');
        String leafName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        return leafName.isEmpty() ? "file" : leafName;
    }

    /**
     * 删除 temp 下的文件并调整处理计数（用于 PDF 转图片后清理原始 PDF）
     */
    public void removeTempFile(String fileName) throws IOException {
        try (var stream = Files.walk(storageRoot)) {
            Optional<Path> matched = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(fileName))
                    .findFirst();
            if (matched.isPresent()) {
                Files.deleteIfExists(matched.get());
                processingCount.decrementAndGet();
            }
        }
    }

    /**
     * 将 storage/temp 下的文件转移到 storage/archive
     *
     * @param fileName 文件名（支持模糊匹配，如 "2025_报告.xlsx"）
     * @return 归档后的完整路径
     * @throws IOException 文件未找到或移动失败时抛出
     */
    public String moveArchiveFile(String fileName) throws IOException {
        // 递归搜索 storage/temp 下匹配的文件
        try (var stream = Files.walk(storageRoot)) {
            Optional<Path> matched = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(fileName))
                    .findFirst();

            Path source = matched.orElseThrow(
                    () -> new IOException("文件未找到: " + fileName)
            );

            // 计算相对路径（相对于 storageRoot），保持目录结构
            Path relative = storageRoot.relativize(source);
            Path target = archiveRoot.resolve(relative);

            // 创建目标目录
            Files.createDirectories(target.getParent());

            // 如果目标文件已存在，先删除
            if (Files.exists(target)) {
                Files.delete(target);
            }

            // 移动文件
            Files.move(source, target);

            processingCount.decrementAndGet();

            cacheService.evictDashboard();

            return target.toString();
        }
    }

    /**
     * 将 storage/temp 下的文件转移到 storage/failed
     */
    public String failedFile(String fileName) throws IOException {
        return failedFile(fileName, null);
    }

    /**
     * 将文件转移到 failed 并写入错误原因
     */
    public String failedFile(String fileName, String errorMessage) throws IOException {
        try (var stream = Files.walk(storageRoot)) {
            Optional<Path> matched = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(fileName))
                    .findFirst();

            Path source = matched.orElseThrow(
                    () -> new IOException("文件未找到: " + fileName)
            );

            Path relative = storageRoot.relativize(source);
            Path target = failedRoot.resolve(relative);

            Files.createDirectories(target.getParent());

            if (Files.exists(target)) {
                Files.delete(target);
            }

            Files.move(source, target);

            processingCount.decrementAndGet();

            cacheService.evictDashboard();

            if (errorMessage != null && !errorMessage.isEmpty()) {
                Path errorFile = target.resolveSibling(target.getFileName() + ".error.json");
                String errorJson = "{\"message\":" + jsonEncode(errorMessage) + "}";
                Files.writeString(errorFile, errorJson);
            }

            return target.toString();
        }
    }

    private String jsonEncode(String s) {
        try {
            return new ObjectMapper().writeValueAsString(s);
        } catch (JsonProcessingException e) {
            return "\"" + s.replace("\"", "\\\"") + "\"";
        }
    }
}
