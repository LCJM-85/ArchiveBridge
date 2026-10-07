package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.service.ReviewDraftService;
import org.springframework.http.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.*;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/review")
public class ReviewDraftController {
    private final ReviewDraftService reviews;
    public ReviewDraftController(ReviewDraftService reviews) { this.reviews=reviews; }
    public record EditRequest(int version,List<Map<String,String>> records) {}
    public record VersionRequest(int version) {}
    @GetMapping
    public R<?> list(@RequestParam(defaultValue="") String status,
            @RequestParam(defaultValue="") String archiveType,
            @RequestParam(defaultValue="") String keyword,
            @RequestParam(required=false) Integer logId,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size) {
        return R.ok(reviews.list(status,archiveType,keyword,logId,page,size));
    }
    @GetMapping("/{id}")
    public R<?> detail(@PathVariable long id) { return R.ok(reviews.detail(id)); }
    @PutMapping("/{id}")
    public R<?> save(@PathVariable long id,@RequestBody EditRequest body) {
        return R.ok(reviews.save(id,body.version(),body.records()));
    }
    @PostMapping("/{id}/confirm")
    public R<?> confirm(@PathVariable long id,@RequestBody EditRequest body) {
        return R.ok(reviews.confirm(id,body.version(),body.records()));
    }
    @PostMapping("/{id}/discard")
    public R<?> discard(@PathVariable long id,@RequestBody VersionRequest body) {
        return R.ok(reviews.discard(id,body.version()));
    }
    @GetMapping("/{id}/source")
    public ResponseEntity<FileSystemResource> source(@PathVariable long id) throws Exception {
        Path file=reviews.source(id);
        String mime=Files.probeContentType(file);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(
                mime==null ? "application/octet-stream" : mime))
            .body(new FileSystemResource(file));
    }
    @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class})
    public ResponseEntity<R<?>> invalid(RuntimeException error) {
        boolean conflict=error instanceof ReviewDraftService.VersionConflictException;
        int code=conflict ? 409 : 400;
        R<Map<String,String>> result=R.error(code,error.getMessage());
        result.setData(Map.of("reason",conflict ? "version_conflict" : "review_failed"));
        return ResponseEntity.status(code).body(result);
    }
}
