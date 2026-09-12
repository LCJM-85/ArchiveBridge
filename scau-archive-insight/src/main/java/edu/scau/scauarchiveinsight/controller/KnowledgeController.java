package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.service.KnowledgeFileStorage;
import edu.scau.scauarchiveinsight.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "知识库", description = "RAG 知识库管理")
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final KnowledgeFileStorage fileStorage;

    @Autowired
    public KnowledgeController(KnowledgeService knowledgeService, KnowledgeFileStorage fileStorage) {
        this.knowledgeService = knowledgeService;
        this.fileStorage = fileStorage;
    }

    @Operation(summary = "上传文件（仅保存到 storage/rag，返回不透明文件标识）")
    @PostMapping("/upload/file")
    public R<List<Map<String, Object>>> uploadFile(@RequestParam("files") List<MultipartFile> files) {
        List<Map<String, Object>> fileList = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                KnowledgeFileStorage.StoredFile stored = fileStorage.store(file);
                Map<String, Object> info = new HashMap<>();
                info.put("name", stored.originalName());
                info.put("fileId", stored.fileId());
                info.put("size", stored.size());
                fileList.add(info);
            } catch (IOException | IllegalArgumentException e) {
                // 跳过失败文件
            }
        }
        return R.ok(fileList);
    }

    @Operation(summary = "使用已上传文件标识写入知识库并处理")
    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestBody Map<String, String> body) {
        String fileId = body.get("fileId");
        String fileName = body.get("fileName");

        if (fileId == null || fileId.isBlank()) {
            return R.error(400, "文件标识不能为空");
        }

        final Path filePath;
        try {
            filePath = fileStorage.resolve(fileId);
        } catch (IllegalArgumentException e) {
            return R.error(400, e.getMessage());
        }

        if (fileName == null || fileName.isBlank()) {
            fileName = filePath.getFileName().toString();
        }
        fileName = KnowledgeFileStorage.safeLeafName(fileName);
        String fileType = KnowledgeFileStorage.extensionOf(filePath.getFileName().toString());

        String title = fileName.replaceAll("\\.[^.]*$", "");
        Map<String, Object> result = knowledgeService.processFile(filePath.toString(), title, fileType);

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
        url = url.trim();
        if (!isValidHttpUrl(url)) {
            return R.error(400, "URL 仅支持具有主机名且不含用户凭据的 HTTP/HTTPS 地址");
        }

        Map<String, Object> result = knowledgeService.processUrl(url, title);
        if (result.containsKey("error")) {
            return R.error(500, (String) result.get("error"));
        }
        return R.ok(result);
    }

    private static boolean isValidHttpUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            int port = uri.getPort();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank()
                    && uri.getRawUserInfo() == null
                    && port <= 65535;
        } catch (IllegalArgumentException e) {
            return false;
        }
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
