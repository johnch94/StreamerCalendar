package com.example.streamercalendar.controller;

import com.example.streamercalendar.dto.LoginRequest;
import com.example.streamercalendar.support.ImportSecurityConfig;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import jakarta.servlet.http.Cookie;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ImportSecurityConfig
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private MvcResult login(String username, String password, MockHttpSession session) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password, false))))
                .andReturn();
    }

    @Test
    void 올바른_계정으로_로그인하면_200과_로그인_상태를_반환하고_세션이_유지된다() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "secret1234", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("admin"));

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void 로그인하면_기존_세션_ID가_새로_발급된다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String before = session.getId();

        MvcResult result = login("admin", "secret1234", session);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getRequest().getSession(false).getId()).isNotEqualTo(before);
    }

    @Test
    void 비밀번호가_틀리면_401과_에러코드를_반환하고_로그인되지_않는다() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "wrong", false))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void 존재하지_않는_아이디도_같은_401_에러를_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("nobody", "secret1234", false))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void 아이디가_비어있으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("", "secret1234", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 로그인하지_않은_사용자가_상태를_조회하면_200과_비로그인_상태를_반환한다() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.username").doesNotExist());
    }

    @Test
    void 로그아웃하면_204를_반환하고_세션이_해제된다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        login("admin", "secret1234", session);

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(jsonPath("$.authenticated").value(false));
    }
    private MockHttpServletResponse loginWithRememberMe() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "secret1234", true))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
    }

    @Test
    void 로그인_상태_유지를_선택하면_14일짜리_remember_me_쿠키를_발급한다() throws Exception {
        MockHttpServletResponse response = loginWithRememberMe();

        Cookie cookie = response.getCookie("remember-me");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getMaxAge()).isEqualTo(14 * 24 * 60 * 60);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
    }

    @Test
    void 로그인_상태_유지를_선택하지_않으면_remember_me_쿠키를_발급하지_않는다() throws Exception {
        MvcResult result = login("admin", "secret1234", new MockHttpSession());

        assertThat(result.getResponse().getCookie("remember-me")).isNull();
    }

    @Test
    void 세션이_없어도_remember_me_쿠키가_있으면_로그인_상태로_인식한다() throws Exception {
        Cookie rememberMe = loginWithRememberMe().getCookie("remember-me");

        // 세션 없이 쿠키만 보냄 (브라우저를 닫았다 다시 연 상황)
        mockMvc.perform(get("/api/auth/me").cookie(rememberMe))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void 위조된_remember_me_쿠키는_무시하고_비로그인으로_처리한다() throws Exception {
        mockMvc.perform(get("/api/auth/me").cookie(new Cookie("remember-me", "YWRtaW46OTk5OTk5OTk5OTk5OTpTSEEyNTY6Zm9yZ2Vk")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void 로그아웃하면_remember_me_쿠키도_삭제한다() throws Exception {
        Cookie rememberMe = loginWithRememberMe().getCookie("remember-me");

        MockHttpServletResponse response = mockMvc.perform(post("/api/auth/logout").cookie(rememberMe))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse();

        assertThat(response.getCookie("remember-me")).isNotNull();
        assertThat(response.getCookie("remember-me").getMaxAge()).isZero();
    }
}
