package edu.scau.scauarchiveinsight.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KnowledgeServiceUrlSecurityTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void nonSuccessfulPythonResponseIsReturnedAsError() throws Exception {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/kb/process-url", exchange -> {
            byte[] body = "{\"detail\":\"禁止访问内网地址\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(422, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        String pythonBase = "http://127.0.0.1:" + server.getAddress().getPort();
        KnowledgeService service = new KnowledgeService(
                mock(JdbcTemplate.class),
                mock(KnowledgeFileStorage.class),
                HttpClient.newHttpClient(),
                pythonBase
        );

        Map<String, Object> result = service.processUrl("https://example.com", "example");

        assertThat(result).containsEntry("error", "禁止访问内网地址");
    }
}
