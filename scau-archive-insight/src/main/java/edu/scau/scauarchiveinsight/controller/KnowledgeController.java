package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "知识库", description = "RAG 知识库管理")
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private static final Path RAG_STORAGE = Paths.get(System.getProperty("user.dir"), "storage", "rag");

    @Autowired
    private KnowledgeService knowledgeService;

    @Operation(summary = "上传文件（仅保存到 storage/rag，返回路径）")
    @PostMapping("/upload/file")
    public R<List<Map<String, Object>>> uploadFile(@RequestParam("files") List<MultipartFile> files) {
        List<Map<String, Object>> fileList = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                Files.createDirectories(RAG_STORAGE);
                String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path target = RAG_STORAGE.resolve(filename);
                file.transferTo(target.toFile());
                Map<String, Object> info = new HashMap<>();
                info.put("name", file.getOriginalFilename());
                info.put("path", target.toString());
                info.put("size", file.getSize());
                fileList.add(info);
            } catch (IOException e) {
                // 跳过失败文件
            }
        }
        return R.ok(fileList);
    }

    @Operation(summary = "上传文件到知识库并处理")
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestBody Map<String, String> body) {
        String filePath = body.get("filePath");
        String fileName = body.get("fileName");
        String fileType = body.get("fileType");

        if (filePath == null || filePath.isBlank()) {
            return R.error(400, "文件路径不能为空");
        }
        if (fileName == null || fileName.isBlank()) {
            fileName = filePath;
        }
        if (fileType == null || fileType.isBlank()) {
            fileType = filePath.replaceAll(".*\\.", "");
        }

        String title = fileName.replaceAll("\\.[^.]*$", "");
        Map<String, Object> result = knowledgeService.processFile(filePath, title, fileType);

        if (result.containsKey("error")) {
            return R.error(500, (String) result.get("error"));
        }
        return R.ok(result);
    }

    @Operation(summary = "添加网页链接到知识库")
    @PostMapping("/url")
    public R<Map<String, Object>> addUrl(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        String title = body.getOrDefault("title", "");

        if (url == null || url.isBlank()) {
            return R.error(400, "URL 不能为空");
        }

        Map<String, Object> result = knowledgeService.processUrl(url, title);
        if (result.containsKey("error")) {
            return R.error(500, (String) result.get("error"));
        }
        return R.ok(result);
    }

    @Operation(summary = "知识库文档列表")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        return R.ok(knowledgeService.listDocuments());
    }

    @Operation(summary = "删除知识库文档")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable int id) {
        boolean deleted = knowledgeService.deleteDocument(id);
        if (!deleted) {
            return R.error(404, "文档不存在");
        }
        return R.ok(null);
    }
}
