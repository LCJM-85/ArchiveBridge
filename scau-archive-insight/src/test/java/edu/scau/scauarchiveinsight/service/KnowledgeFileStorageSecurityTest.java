package edu.scau.scauarchiveinsight.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeFileStorageSecurityTest {

    private Path testBase;
    private Path ragRoot;
    private KnowledgeFileStorage storage;

    @BeforeEach
    void setUp() throws Exception {
        testBase = Path.of("target", "security-test-data", UUID.randomUUID().toString())
                .toAbsolutePath().normalize();
        ragRoot = testBase.resolve("rag");
        Files.createDirectories(ragRoot);
        storage = new KnowledgeFileStorage(ragRoot);
    }

    @AfterEach
    void cleanUp() throws Exception {
        if (!Files.exists(testBase)) return;
        try (var paths = Files.walk(testBase)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Test
    void storesUploadWithOpaqueIdAndSafeDisplayName() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "../../private/guide.txt", "text/plain",
                "knowledge".getBytes(StandardCharsets.UTF_8));

        KnowledgeFileStorage.StoredFile stored = storage.store(file);

        assertThat(stored.fileId()).matches("[0-9a-f]{32}\\.txt");
        assertThat(stored.originalName()).isEqualTo("guide.txt");
        assertThat(stored.path().startsWith(ragRoot)).isTrue();
        assertThat(Files.readString(stored.path())).isEqualTo("knowledge");
    }

    @Test
    void rejectsTraversalAbsoluteAndMissingIdentifiers() {
        assertThatThrownBy(() -> storage.resolve("../outside.txt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.resolve(testBase.resolve("outside.txt").toString()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.resolve("00000000000000000000000000000000.txt"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
