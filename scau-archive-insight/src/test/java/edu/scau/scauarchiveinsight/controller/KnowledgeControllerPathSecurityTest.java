package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.service.KnowledgeFileStorage;
import edu.scau.scauarchiveinsight.service.KnowledgeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class KnowledgeControllerPathSecurityTest {

    private Path testBase;
    private Path ragRoot;
    private KnowledgeService knowledgeService;
    private KnowledgeController controller;

    @BeforeEach
    void setUp() throws Exception {
        testBase = Path.of("target", "security-test-data", UUID.randomUUID().toString())
                .toAbsolutePath().normalize();
        ragRoot = testBase.resolve("rag");
        Files.createDirectories(ragRoot);
        knowledgeService = mock(KnowledgeService.class);
        controller = new KnowledgeController(knowledgeService, new KnowledgeFileStorage(ragRoot));
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
    void uploadAndProcessUsesOpaqueIdInsteadOfClientPath() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "../../private/guide.txt", "text/plain",
                "knowledge".getBytes(StandardCharsets.UTF_8));
        R<List<Map<String, Object>>> uploadResponse = controller.uploadFile(List.of(file));
        Map<String, Object> uploaded = uploadResponse.getData().get(0);

        assertThat(uploaded).containsKeys("fileId", "name", "size");
        assertThat(uploaded).doesNotContainKey("path");
        String fileId = (String) uploaded.get("fileId");
        when(knowledgeService.processFile(anyString(), anyString(), anyString()))
                .thenReturn(Map.of("chunkCount", 1));

        R<Map<String, Object>> processResponse = controller.upload(Map.of(
                "fileId", fileId,
                "fileName", (String) uploaded.get("name"),
                "fileType", "exe"
        ));

        assertThat(processResponse.getCode()).isEqualTo(200);
        verify(knowledgeService).processFile(
                ragRoot.resolve(fileId).toAbsolutePath().normalize().toString(),
                "guide",
                "txt"
        );
    }

    @Test
    void rejectsClientProvidedLocalPathWithoutCallingProcessor() throws Exception {
        Path outside = testBase.resolve("server-secret.txt");
        Files.writeString(outside, "secret");

        R<Map<String, Object>> response = controller.upload(Map.of(
                "filePath", outside.toString(),
                "fileName", "server-secret.txt",
                "fileType", "txt"
        ));

        assertThat(response.getCode()).isEqualTo(400);
        verifyNoInteractions(knowledgeService);
    }

    @Test
    void rejectsNonHttpKnowledgeUrlBeforeCallingProcessor() {
        R<Map<String, Object>> response = controller.addUrl(Map.of(
                "url", "file:///etc/passwd",
                "title", "local file"
        ));

        assertThat(response.getCode()).isEqualTo(400);
        assertThat(response.getMsg()).contains("HTTP");
        verifyNoInteractions(knowledgeService);
    }

    @Test
    void rejectsKnowledgeUrlWithoutHostBeforeCallingProcessor() {
        R<Map<String, Object>> response = controller.addUrl(Map.of(
                "url", "https:///missing-host",
                "title", "invalid"
        ));

        assertThat(response.getCode()).isEqualTo(400);
        verifyNoInteractions(knowledgeService);
    }

    @Test
    void rejectsKnowledgeUrlWithEmbeddedCredentialsBeforeCallingProcessor() {
        R<Map<String, Object>> response = controller.addUrl(Map.of(
                "url", "https://user:password@example.com/private",
                "title", "invalid"
        ));

        assertThat(response.getCode()).isEqualTo(400);
        verifyNoInteractions(knowledgeService);
    }
}
