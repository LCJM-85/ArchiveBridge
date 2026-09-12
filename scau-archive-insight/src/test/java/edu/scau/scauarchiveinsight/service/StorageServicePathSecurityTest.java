package edu.scau.scauarchiveinsight.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StorageServicePathSecurityTest {

    Path tempDir;

    @BeforeEach
    void setUpTempDirectory() throws Exception {
        tempDir = Path.of("target", "security-test-data", UUID.randomUUID().toString())
                .toAbsolutePath().normalize();
        Files.createDirectories(tempDir);
    }

    @AfterEach
    void cleanUpTempDirectory() throws Exception {
        if (!Files.exists(tempDir)) return;
        try (var paths = Files.walk(tempDir)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    void rejectsUploadTypeOutsideServerAllowlist() {
        StorageService service = new StorageService(tempDir);
        MockMultipartFile file = new MockMultipartFile(
                "files", "archive.csv", "text/csv", "id,name".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> result = service.saveFiles(List.of(file), "../../archive");

        assertThat(result.get("success")).isEqualTo(false);
        assertThat((List<?>) result.get("uploaded")).isEmpty();
        assertThat((List<?>) result.get("errors")).isNotEmpty();
        assertThat(Files.exists(tempDir.resolve("archive"))).isFalse();
    }

    @Test
    void storesTraversalFilenameAsRandomLeafInsideSelectedDirectory() throws Exception {
        StorageService service = new StorageService(tempDir);
        MockMultipartFile file = new MockMultipartFile(
                "files", "../../../../outside.csv", "text/csv", "id,name".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> result = service.saveFiles(List.of(file), "csv");

        assertThat(result.get("success")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> uploaded = (List<Map<String, String>>) result.get("uploaded");
        assertThat(uploaded).hasSize(1);

        Path storedPath = Path.of(uploaded.get(0).get("path")).toAbsolutePath().normalize();
        Path expectedRoot = tempDir.resolve("temp").toAbsolutePath().normalize();
        assertThat(storedPath.startsWith(expectedRoot)).isTrue();
        assertThat(storedPath.getFileName().toString()).matches("[0-9a-f]{32}_outside\\.csv");
        assertThat(Files.readString(storedPath)).isEqualTo("id,name");
        assertThat(Files.exists(tempDir.resolve("outside.csv"))).isFalse();
    }
}
