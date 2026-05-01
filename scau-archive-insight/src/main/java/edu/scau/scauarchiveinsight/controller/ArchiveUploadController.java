package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ArchiveUploadController {

    @Autowired
    private StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("type") String type) {

        Map<String, Object> result = storageService.saveFiles(files, type);
        return ResponseEntity.ok(result);
    }
}
