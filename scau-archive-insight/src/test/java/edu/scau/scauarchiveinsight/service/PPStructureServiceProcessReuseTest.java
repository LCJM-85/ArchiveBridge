package edu.scau.scauarchiveinsight.service;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class PPStructureServiceProcessReuseTest {

    @Test
    void reusesOnePythonWorkerForConsecutiveOcrRequests() throws Exception {
        PPStructureService service = new PPStructureService();
        OCRTaskManager taskManager = mock(OCRTaskManager.class);
        ReflectionTestUtils.setField(service, "ocrTaskManager", taskManager);

        Process process = mock(Process.class);
        String responses = "READY\n"
                + "RESULT\t{\"grids\":[],\"errors\":[]}\n"
                + "RESULT\t{\"grids\":[],\"errors\":[]}\n";
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(
                responses.getBytes(StandardCharsets.UTF_8)));
        when(process.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(process.waitFor()).thenReturn(0);
        when(process.isAlive()).thenReturn(true);

        try (MockedConstruction<ProcessBuilder> builders = mockConstruction(
                ProcessBuilder.class,
                (builder, context) -> {
                    when(builder.environment()).thenReturn(new HashMap<>());
                    when(builder.redirectErrorStream(any(Boolean.class))).thenReturn(builder);
                    when(builder.start()).thenReturn(process);
                })) {
            service.parseTable("first.png");
            service.parseTable("second.png");

            assertEquals(1, builders.constructed().size(),
                    "连续 OCR 请求应复用同一个常驻 Python 进程");
            assertEquals("auto", builders.constructed().get(0).environment().get("OCR_DEVICE"),
                    "Java 应把 application.yaml 中的 OCR 设备配置传给 Python");
        }
    }
}
