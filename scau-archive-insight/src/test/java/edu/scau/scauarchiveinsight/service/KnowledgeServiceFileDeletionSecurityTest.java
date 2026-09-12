package edu.scau.scauarchiveinsight.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeServiceFileDeletionSecurityTest {

    private static final String SELECT_PATH_SQL = "SELECT file_path FROM knowledge_base WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM knowledge_base WHERE id = ?";

    private Path testBase;
    private Path ragRoot;
    private JdbcTemplate jdbcTemplate;
    private KnowledgeService service;

    @BeforeEach
    void setUp() throws Exception {
        testBase = Path.of("target", "security-test-data", UUID.randomUUID().toString())
                .toAbsolutePath().normalize();
        ragRoot = testBase.resolve("rag");
        Files.createDirectories(ragRoot);
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new KnowledgeService(jdbcTemplate, new KnowledgeFileStorage(ragRoot));
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
    void deletingDatabaseRecordNeverDeletesFileOutsideRagRoot() throws Exception {
        Path outside = testBase.resolve("server-secret.txt");
        Files.writeString(outside, "secret");
        when(jdbcTemplate.queryForList(SELECT_PATH_SQL, String.class, 7))
                .thenReturn(List.of(outside.toString()));
        when(jdbcTemplate.update(DELETE_SQL, 7)).thenReturn(1);

        boolean deleted = service.deleteDocument(7);

        assertThat(deleted).isTrue();
        assertThat(Files.exists(outside)).isTrue();
    }

    @Test
    void deletingDatabaseRecordMayDeleteLegacyFileInsideRagRoot() throws Exception {
        Path managedFile = ragRoot.resolve("legacy-guide.txt");
        Files.writeString(managedFile, "knowledge");
        when(jdbcTemplate.queryForList(SELECT_PATH_SQL, String.class, 8))
                .thenReturn(List.of(managedFile.toString()));
        when(jdbcTemplate.update(DELETE_SQL, 8)).thenReturn(1);

        boolean deleted = service.deleteDocument(8);

        assertThat(deleted).isTrue();
        assertThat(Files.exists(managedFile)).isFalse();
    }
}
