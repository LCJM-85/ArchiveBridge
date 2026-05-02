package edu.scau.scauarchiveinsight.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@RestController
@RequestMapping("/storage")
public class StorageController {

    private static final Path STORAGE_ROOT = Paths.get(System.getProperty("user.dir"), "storage");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        List<Map<String, Object>> files = new ArrayList<>();

        files.addAll(scanDir(STORAGE_ROOT.resolve("temp"), "processing"));
        files.addAll(scanDir(STORAGE_ROOT.resolve("archive"), "success"));
        files.addAll(scanDir(STORAGE_ROOT.resolve("failed"), "error"));

        // 按日期倒序排列
        files.sort((a, b) -> String.valueOf(b.get("date")).compareTo(String.valueOf(a.get("date"))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("data", files);
        return ResponseEntity.ok(result);
    }

    private List<Map<String, Object>> scanDir(Path root, String status) {
        List<Map<String, Object>> files = new ArrayList<>();
        if (!Files.isDirectory(root)) return files;

        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> !p.getFileName().toString().endsWith(".error.json"))
                  .forEach(path -> {
                      Path relative = STORAGE_ROOT.relativize(path);
                      int nameCount = relative.getNameCount();
                      String datePart = nameCount > 2 ? relative.getName(1).toString() : "";
                      String typePart = nameCount > 3 ? relative.getName(2).toString() : "unknown";

                      Map<String, Object> item = new LinkedHashMap<>();
                      item.put("name", path.getFileName().toString());
                      item.put("path", relative.toString().replace("\\", "/"));
                      item.put("date", datePart);
                      item.put("type", typePart);
                      item.put("status", status);

                      // 失败文件读取错误详情
                      if ("error".equals(status)) {
                          Path errorFile = path.resolveSibling(path.getFileName() + ".error.json");
                          if (Files.exists(errorFile)) {
                              try {
                                  Map<String, Object> errData = objectMapper.readValue(
                                          errorFile.toFile(), new TypeReference<Map<String, Object>>() {});
                                  item.put("errorMessage", errData.getOrDefault("message", ""));
                                  item.put("errors", errData.getOrDefault("errors", List.of()));
                              } catch (IOException ignored) {
                              }
                          }
                      }

                      files.add(item);
                  });
        } catch (IOException ignored) {
        }

        return files;
    }
}
