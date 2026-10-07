package edu.scau.scauarchiveinsight.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AIStreamCompletionTest {
    @Test @SuppressWarnings("unchecked")
    void forwardsDoneBeforeCompletionAndIgnoresLaterEvents() throws Exception {
        var client=new AIAssistantClient();
        var http=mock(HttpClient.class);
        HttpResponse<InputStream> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(new ByteArrayInputStream((
            "data: {\"type\":\"token\",\"content\":\"answer\"}\n\n"+
            "data: {\"type\": \"done\"}\n\n"+
            "data: {\"type\":\"token\",\"content\":\"late\"}\n\n").getBytes(StandardCharsets.UTF_8)));
        when(http.send(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<InputStream>>any())).thenReturn(response);
        ReflectionTestUtils.setField(client,"httpClient",http);
        var emitter=mock(SseEmitter.class);
        var completed=new CountDownLatch(1);
        doAnswer(invocation->{completed.countDown();return null;}).when(emitter).complete();
        client.chatStream("test",List.of(),emitter);
        assertTrue(completed.await(5,TimeUnit.SECONDS));
        var events=org.mockito.ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter,times(2)).send(events.capture());
        assertTrue(events.getAllValues().get(1).build().stream().anyMatch(data->String.valueOf(data.getData()).contains("\"done\"")));
        verify(emitter,never()).completeWithError(any());
    }
}
