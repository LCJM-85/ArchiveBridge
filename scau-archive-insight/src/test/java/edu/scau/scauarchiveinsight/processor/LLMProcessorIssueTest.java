package edu.scau.scauarchiveinsight.processor;

import edu.scau.scauarchiveinsight.service.ReviewDraftService;
import edu.scau.scauarchiveinsight.service.LLMExtractionService;
import edu.scau.scauarchiveinsight.service.OCRLogService;
import edu.scau.scauarchiveinsight.service.OCRTaskManager;
import edu.scau.scauarchiveinsight.service.QualityScoreService;
import edu.scau.scauarchiveinsight.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LLMProcessorIssueTest {

    private LLMExtractionService extraction;
    private ReviewDraftService persistence;
    private OCRLogService logs;
    private QualityScoreService scores;
    private StorageService storage;
    private OCRTaskManager tasks;
    private LLMProcessor processor;

    @BeforeEach
    void setUp() {
        extraction = mock(LLMExtractionService.class);
        persistence = mock(ReviewDraftService.class);
        logs = mock(OCRLogService.class);
        scores = mock(QualityScoreService.class);
        storage = mock(StorageService.class);
        tasks = mock(OCRTaskManager.class);
        processor = new LLMProcessor(extraction, persistence, logs, scores, storage, tasks);
    }

    @Test
    void partialPdfPageFailureMustBeExposedAsIssue() throws Exception {
        when(extraction.extractWithIssues("page-1.png")).thenThrow(new IllegalStateException("模型超时"));
        when(extraction.extractWithIssues("page-2.png")).thenReturn(new LLMExtractionService.ExtractionResult(
                List.of(Map.of("student_no", "20240001")), List.of()));

        processor.processPdfPages("batch.pdf", List.of("page-1.png", "page-2.png"),
                "admission", null, null, null);

        verify(persistence).stage(eq("batch.pdf"), eq("pdf-llm"), eq("admission"), anyList(), org.mockito.ArgumentMatchers.argThat(issues -> issues.toString().contains("模型超时")));
    }

    @Test
    void imageParsingDoesNotScoreOrArchiveBeforeReview() throws Exception {
        when(extraction.extractWithIssues("photo.png")).thenReturn(new LLMExtractionService.ExtractionResult(
                List.of(Map.of("student_no", "20240001")), List.of()));

        processor.process(List.of("photo.png"), "admission", null, null, null);

        org.mockito.Mockito.verify(storage, org.mockito.Mockito.never()).moveArchiveFile("photo.png");
        org.mockito.Mockito.verifyNoInteractions(scores);
        verify(persistence).stage(eq("photo.png"), eq("picture-llm"), eq("admission"), anyList(), anyList());
    }
}
