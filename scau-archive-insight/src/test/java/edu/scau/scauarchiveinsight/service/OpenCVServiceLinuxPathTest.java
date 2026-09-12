package edu.scau.scauarchiveinsight.service;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenCVServiceLinuxPathTest {

    @Test
    void commandUsesTheTrackedCaseSensitiveOpenCvDirectory() throws Exception {
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(
                "storage/enhance/result.jpg\n".getBytes(StandardCharsets.UTF_8)));
        when(process.waitFor()).thenReturn(0);
        AtomicReference<String[]> capturedCommand = new AtomicReference<>();

        try (MockedConstruction<ProcessBuilder> construction = Mockito.mockConstruction(
                ProcessBuilder.class,
                (builder, context) -> {
                    Object commandArgument = context.arguments().get(0);
                    assertThat(commandArgument).isInstanceOf(String[].class);
                    capturedCommand.set((String[]) commandArgument);
                    when(builder.start()).thenReturn(process);
                })) {
            String result = new OpenCVService().enhanceImage("storage/temp/input.jpg");

            assertThat(result).isEqualTo("storage/enhance/result.jpg");
            assertThat(construction.constructed()).hasSize(1);
            String[] command = capturedCommand.get();
            assertThat(command).isNotNull();
            assertThat(command[1].replace('\\', '/'))
                    .endsWith("/src/main/python/openCV/opencv.py");
        }
    }
}
