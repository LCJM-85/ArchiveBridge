package edu.scau.scauarchiveinsight.service;

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
}
