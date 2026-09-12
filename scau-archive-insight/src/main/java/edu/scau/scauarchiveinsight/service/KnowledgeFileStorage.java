package edu.scau.scauarchiveinsight.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class KnowledgeFileStorage {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "xlsx", "txt");
    private static final Pattern FILE_ID_PATTERN = Pattern.compile(
            "^[0-9a-f]{32}\\.(pdf|docx|xlsx|txt)$");

    private final Path ragRoot;

    public KnowledgeFileStorage() {
        this(Paths.get(System.getProperty("user.dir"), "storage", "rag"));
    }

    public KnowledgeFileStorage(Path ragRoot) {
        this.ragRoot = ragRoot.toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalName = safeLeafName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的知识库文件类型");
        }

        Files.createDirectories(ragRoot);
        String fileId = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = ragRoot.resolve(fileId).normalize();
        if (!target.startsWith(ragRoot)) {
            throw new IllegalArgumentException("非法知识库文件路径");
        }

        Files.copy(file.getInputStream(), target);
        return new StoredFile(fileId, originalName, target, file.getSize());
    }

    public Path resolve(String fileId) {
        if (fileId == null || !FILE_ID_PATTERN.matcher(fileId).matches()) {
            throw new IllegalArgumentException("无效的文件标识");
        }

        try {
            Path realRoot = ragRoot.toRealPath();
            Path candidate = ragRoot.resolve(fileId).normalize();
            if (!candidate.startsWith(ragRoot)) {
                throw new IllegalArgumentException("无效的文件标识");
            }

            Path realCandidate = candidate.toRealPath();
            if (!realCandidate.startsWith(realRoot)
                    || !Files.isRegularFile(realCandidate, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("无效的文件标识或文件不存在");
            }
            return realCandidate;
        } catch (IOException e) {
            throw new IllegalArgumentException("无效的文件标识或文件不存在", e);
        }
    }

    public boolean deleteManagedFile(String storedPath) throws IOException {
        if (storedPath == null || storedPath.isBlank() || !Files.exists(ragRoot)) {
            return false;
        }

        final Path candidate;
        try {
            candidate = Path.of(storedPath).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return false;
        }

        if (!candidate.startsWith(ragRoot) || !Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }

        Path realRoot = ragRoot.toRealPath();
        Path realCandidate = candidate.toRealPath();
        if (!realCandidate.startsWith(realRoot) || !Files.isRegularFile(realCandidate)) {
            return false;
        }
        return Files.deleteIfExists(candidate);
    }

    public static String safeLeafName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) return "file";
        String normalized = originalFilename.replace('\\', '/');
        String leaf = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        return leaf.isEmpty() ? "file" : leaf;
    }

    public static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(String fileId, String originalName, Path path, long size) {}
}
