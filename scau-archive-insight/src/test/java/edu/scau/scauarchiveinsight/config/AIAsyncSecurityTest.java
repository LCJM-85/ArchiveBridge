package edu.scau.scauarchiveinsight.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.filter.JwtAuthenticationFilter;
import edu.scau.scauarchiveinsight.util.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AIAsyncSecurityTest {
    @Configuration @EnableWebSecurity @EnableWebMvc
    @Import({SecurityConfig.class,JwtAuthenticationFilter.class,JsonAuthenticationEntryPoint.class,StreamController.class})
    static class TestConfig {
        @Bean JwtUtils jwtUtils(){var jwt=mock(JwtUtils.class);when(jwt.getUsernameFromToken("valid")).thenReturn("admin");when(jwt.validateToken("valid","admin")).thenReturn(true);return jwt;}
        @Bean ObjectMapper objectMapper(){return new ObjectMapper();}
    }
    @RestController static class StreamController {
        @GetMapping(value="/api/ai/test-stream",produces="text/event-stream")
        SseEmitter stream() throws Exception {
            var emitter=new SseEmitter();
            emitter.send(SseEmitter.event().data("{\"type\":\"done\"}"));
            emitter.complete();return emitter;
        }
    }
    @Test void authenticatedStreamCompletesTwiceWithoutAsyncAccessDenied() throws Exception {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("test",java.util.Map.of("jwt.expiration","3600","jwt.secret","test-only-32-character-secret-key")));
            context.register(TestConfig.class);context.refresh();
            MockMvc mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            mvc.perform(get("/api/ai/test-stream")).andExpect(status().isUnauthorized());
            for(int i=0;i<2;i++) {
                var result=mvc.perform(get("/api/ai/test-stream").header("Authorization","Bearer valid"))
                    .andExpect(request().asyncStarted()).andReturn();
                mvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("done")));
            }
        }
    }
}
