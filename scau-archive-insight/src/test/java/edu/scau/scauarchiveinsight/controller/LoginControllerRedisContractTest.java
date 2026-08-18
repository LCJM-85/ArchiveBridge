package edu.scau.scauarchiveinsight.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.scau.scauarchiveinsight.dto.LoginDTO;
import edu.scau.scauarchiveinsight.pojo.SysUser;
import edu.scau.scauarchiveinsight.service.UserService;
import edu.scau.scauarchiveinsight.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginControllerRedisContractTest {

    private LoginController controller;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private UserService userService;
    private JwtUtils jwtUtils;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        controller = new LoginController();
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        userService = mock(UserService.class);
        jwtUtils = mock(JwtUtils.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(controller, "stringRedisTemplate", redisTemplate);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "jwtUtils", jwtUtils);
    }

    @Test
    void captchaReturnsUuidAndBase64AndStoresAnswerWithTtl() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(valueOperations.increment("scau:auth:captcha:req:127.0.0.1")).thenReturn(1L);

        controller.captcha(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).startsWith("application/json");
        Map<String, Object> body = new ObjectMapper().readValue(
                response.getContentAsString(), new TypeReference<>() {});
        assertThat(body.get("success")).isEqualTo(true);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data.get("uuid")).asString().hasSize(32);
        assertThat(data.get("imageBase64")).asString().startsWith("data:image/png;base64,");
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.startsWith("scau:auth:captcha:"),
                anyString(), eq(Duration.ofSeconds(120)));
    }

    @Test
    void loginConsumesCaptchaByUuidInsteadOfUsingSession() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("secret");
        dto.setCaptcha("a1b2");
        ReflectionTestUtils.setField(dto, "uuid", "captcha-uuid");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        when(valueOperations.get("scau:auth:login:fail:127.0.0.1:admin")).thenReturn(null);
        when(valueOperations.getAndDelete("scau:auth:captcha:captcha-uuid")).thenReturn("A1B2");
        when(userService.verifyUser("admin", "secret")).thenReturn(0);
        SysUser user = new SysUser();
        user.setRole("admin");
        when(userService.getUserByUsername("admin")).thenReturn(user);
        when(jwtUtils.generateToken("admin", "admin")).thenReturn("jwt-token");

        var response = controller.login(dto, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("token", "jwt-token");
        verify(valueOperations).getAndDelete("scau:auth:captcha:captcha-uuid");
        assertThat(request.getSession(false)).isNull();
    }
}
