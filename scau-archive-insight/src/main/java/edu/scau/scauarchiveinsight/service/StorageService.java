package edu.scau.scauarchiveinsight.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class StorageService {

    private static final Path STORAGE_ROOT = Paths.get(System.getProperty("user.dir"), "storage", "temp");
    private static final Path ARCHIVE_ROOT = Paths.get(System.getProperty("user.dir"), "storage", "archive");
    private static final Path FAILED_ROOT = Paths.get(System.getProperty("user.dir"), "storage", "failed");

    public Map<String, Object> saveFiles(List<MultipartFile> files, String type) {
        List<Map<String, String>> uploaded = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                errors.add("空文件: " + file.getOriginalFilename());
                continue;
            }
            try {
                String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                Path dir = STORAGE_ROOT.resolve(dateStr).resolve(type);
                Files.createDirectories(dir);

                String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path target = dir.resolve(filename);
                file.transferTo(target.toFile());

                Map<String, String> info = new HashMap<>();
                info.put("name", file.getOriginalFilename());
                info.put("path", target.toString());
                info.put("size", String.valueOf(file.getSize()));
                uploaded.add(info);
            } catch (IOException e) {
                errors.add("存储失败: " + file.getOriginalFilename() + " - " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", errors.isEmpty());
        result.put("uploaded", uploaded);
        result.put("errors", errors);
        return result;
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
        try (var stream = Files.walk(STORAGE_ROOT)) {
            Optional<Path> matched = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(fileName))
                    .findFirst();

            Path source = matched.orElseThrow(
                    () -> new IOException("文件未找到: " + fileName)
            );

            // 计算相对路径（相对于 STORAGE_ROOT），保持目录结构
            Path relative = STORAGE_ROOT.relativize(source);
            Path target = ARCHIVE_ROOT.resolve(relative);

            // 创建目标目录
            Files.createDirectories(target.getParent());

            // 如果目标文件已存在，先删除
            if (Files.exists(target)) {
                Files.delete(target);
            }

            // 移动文件
            Files.move(source, target);

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
        try (var stream = Files.walk(STORAGE_ROOT)) {
            Optional<Path> matched = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(fileName))
                    .findFirst();

            Path source = matched.orElseThrow(
                    () -> new IOException("文件未找到: " + fileName)
            );

            Path relative = STORAGE_ROOT.relativize(source);
            Path target = FAILED_ROOT.resolve(relative);

            Files.createDirectories(target.getParent());

            if (Files.exists(target)) {
                Files.delete(target);
            }

            Files.move(source, target);

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
