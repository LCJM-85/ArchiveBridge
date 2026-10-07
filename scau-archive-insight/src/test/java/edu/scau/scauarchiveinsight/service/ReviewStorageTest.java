package edu.scau.scauarchiveinsight.service;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class ReviewStorageTest {
    @TempDir Path temp;
    @Test @SuppressWarnings("unchecked") void archiveRetryIsIdempotentAndDoesNotChangeOtherTaskCount() throws Exception {
        var service=new StorageService(temp);
        ReflectionTestUtils.setField(service,"cacheService",mock(CacheService.class));
        var result=service.saveFiles(List.of(
            new MockMultipartFile("files","one.csv","text/csv",new byte[]{1,2,3}),
            new MockMultipartFile("files","two.csv","text/csv",new byte[]{4})),"csv");
        var files=(List<Map<String,String>>)result.get("uploaded");
        String name=Path.of(files.get(0).get("path")).getFileName().toString();
        String relative=service.reviewSourcePath(name);
        service.markProcessingFinished(name);
        assertEquals(1,service.getProcessingCount());
        Path archive=Path.of(service.moveArchiveFile(name));
        assertEquals(archive,Path.of(service.moveArchiveFile(name)));
        assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(archive));
        assertEquals(1,service.getProcessingCount());
        assertEquals(archive,service.resolveReviewSource(relative));
    }
}
