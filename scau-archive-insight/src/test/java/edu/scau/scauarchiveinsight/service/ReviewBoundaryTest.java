package edu.scau.scauarchiveinsight.service;

import edu.scau.scauarchiveinsight.processor.CSVProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ReviewBoundaryTest {
    @TempDir Path temp;
    @Test void parsingDoesNotImportOrArchiveBeforeConfirmation() throws Exception {
        Path file = temp.resolve("review.csv");
        Files.writeString(file, "姓名\n测试\n");
        var mapping = mock(MetaDataMappingService.class);
        when(mapping.process(anyList(), any(), any(), any())).thenReturn(
                Map.of("data", List.of(Map.of("name", "测试")), "errors", List.of()));
        var storage = mock(StorageService.class);
        var scores = mock(QualityScoreService.class);
        var reviews = mock(ReviewDraftService.class);
        new CSVProcessor(storage, mapping, mock(OCRLogService.class),
                scores, reviews).process(file.toString(), "admission");
        verifyNoInteractions(scores);
        verify(storage, never()).moveArchiveFile(anyString());
        verify(reviews).stage(eq("review.csv"), eq("CSV"), eq("admission"), anyList(), anyList());
    }
}
