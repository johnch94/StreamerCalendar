package com.example.streamercalendar.controller;

import com.example.streamercalendar.domain.Platform;
import com.example.streamercalendar.domain.Source;
import com.example.streamercalendar.dto.StreamRecordRequest;
import com.example.streamercalendar.dto.StreamRecordResponse;
import com.example.streamercalendar.exception.ResourceNotFoundException;
import com.example.streamercalendar.service.StreamRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StreamRecordController.class)
class StreamRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StreamRecordService streamRecordService;

    private StreamRecordResponse sampleResponse(Long id) {
        return new StreamRecordResponse(
                id, 1L, "예시스트리머",
                LocalDate.of(2026, 7, 15), Platform.CHZZK,
                "롤 방송 1부",
                "https://chzzk.naver.com/vod",
                "https://youtube.com/watch?v=abc",
                Source.MANUAL,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    @Test
    void 필터를_적용해_방송기록을_조회하면_200과_리스트를_반환한다() throws Exception {
        given(streamRecordService.getStreamRecords(1L, Platform.CHZZK, 2026, 7))
                .willReturn(List.of(sampleResponse(10L)));

        mockMvc.perform(get("/api/streams")
                        .param("streamerId", "1")
                        .param("platform", "CHZZK")
                        .param("year", "2026")
                        .param("month", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].platform").value("CHZZK"));
    }

    @Test
    void 필터없이_조회하면_200을_반환한다() throws Exception {
        given(streamRecordService.getStreamRecords(null, null, null, null))
                .willReturn(List.of(sampleResponse(10L)));

        mockMvc.perform(get("/api/streams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void 존재하는_방송기록을_상세조회하면_200을_반환한다() throws Exception {
        given(streamRecordService.getStreamRecord(10L)).willReturn(sampleResponse(10L));

        mockMvc.perform(get("/api/streams/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("롤 방송 1부"));
    }

    @Test
    void 존재하지_않는_방송기록을_조회하면_404와_에러코드를_반환한다() throws Exception {
        given(streamRecordService.getStreamRecord(999L))
                .willThrow(new ResourceNotFoundException("STREAM_NOT_FOUND", "해당 방송 기록을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/streams/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STREAM_NOT_FOUND"));
    }

    @Test
    void 필수값을_모두_채워_등록하면_201을_반환한다() throws Exception {
        StreamRecordRequest request = new StreamRecordRequest(
                1L, LocalDate.of(2026, 7, 15), Platform.CHZZK,
                "롤 방송 1부", "https://chzzk.naver.com/vod", "https://youtube.com/watch?v=abc"
        );
        given(streamRecordService.createStreamRecord(any())).willReturn(sampleResponse(10L));

        mockMvc.perform(post("/api/streams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void 필수값이_누락되면_400을_반환한다() throws Exception {
        String invalidJson = """
                {"broadcastDate":"2026-07-15","platform":"CHZZK","title":"제목"}
                """;

        mockMvc.perform(post("/api/streams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 방송기록을_수정하면_200을_반환한다() throws Exception {
        StreamRecordRequest request = new StreamRecordRequest(
                1L, LocalDate.of(2026, 7, 16), Platform.CHZZK,
                "수정된 제목", null, null
        );
        given(streamRecordService.updateStreamRecord(eq(10L), any())).willReturn(sampleResponse(10L));

        mockMvc.perform(put("/api/streams/{id}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void 존재하는_방송기록을_삭제하면_204를_반환한다() throws Exception {
        mockMvc.perform(delete("/api/streams/{id}", 10L))
                .andExpect(status().isNoContent());

        then(streamRecordService).should().deleteStreamRecord(10L);
    }
}
