package com.example.streamercalendar.controller;

import com.example.streamercalendar.dto.StreamerCreateRequest;
import com.example.streamercalendar.dto.StreamerResponse;
import com.example.streamercalendar.exception.ResourceNotFoundException;
import com.example.streamercalendar.service.StreamerService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StreamerController.class)
class StreamerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StreamerService streamerService;

    @Test
    void 스트리머_목록을_조회하면_200과_리스트를_반환한다() throws Exception {
        given(streamerService.getStreamers()).willReturn(List.of(
                new StreamerResponse(1L, "예시스트리머", "https://image.example/1.png", OffsetDateTime.now())
        ));

        mockMvc.perform(get("/api/streamers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("예시스트리머"));
    }

    @Test
    void 이름을_포함해_등록하면_201과_생성된_스트리머를_반환한다() throws Exception {
        StreamerCreateRequest request = new StreamerCreateRequest("새 스트리머", null);
        StreamerResponse response = new StreamerResponse(1L, "새 스트리머", null, OffsetDateTime.now());
        given(streamerService.createStreamer(any())).willReturn(response);

        mockMvc.perform(post("/api/streamers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("새 스트리머"));
    }

    @Test
    void 이름이_비어있으면_400과_에러코드를_반환한다() throws Exception {
        StreamerCreateRequest request = new StreamerCreateRequest("", null);

        mockMvc.perform(post("/api/streamers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 존재하는_스트리머를_삭제하면_204를_반환한다() throws Exception {
        mockMvc.perform(delete("/api/streamers/{id}", 1L))
                .andExpect(status().isNoContent());

        then(streamerService).should().deleteStreamer(1L);
    }

    @Test
    void 존재하지_않는_스트리머를_삭제하면_404와_에러코드를_반환한다() throws Exception {
        willThrow(new ResourceNotFoundException("STREAMER_NOT_FOUND", "해당 스트리머를 찾을 수 없습니다."))
                .given(streamerService).deleteStreamer(999L);

        mockMvc.perform(delete("/api/streamers/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STREAMER_NOT_FOUND"));
    }
}
