package com.example.streamercalendar.service;

import com.example.streamercalendar.domain.Streamer;
import com.example.streamercalendar.dto.StreamerCreateRequest;
import com.example.streamercalendar.dto.StreamerResponse;
import com.example.streamercalendar.exception.ResourceNotFoundException;
import com.example.streamercalendar.repository.StreamRecordRepository;
import com.example.streamercalendar.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StreamerService {

    private final StreamerRepository streamerRepository;
    private final StreamRecordRepository streamRecordRepository;

    public List<StreamerResponse> getStreamers() {
        return streamerRepository.findAll().stream()
                .map(StreamerResponse::from)
                .toList();
    }

    @Transactional
    public StreamerResponse createStreamer(StreamerCreateRequest request) {
        Streamer streamer = new Streamer(request.name(), request.profileImageUrl());
        return StreamerResponse.from(streamerRepository.save(streamer));
    }

    /**
     * 스트리머와 연관 방송 기록을 함께 삭제한다.
     * 엔티티 cascade(orphanRemoval)로 지우면 기록을 전부 읽은 뒤 건수만큼 DELETE가 나가므로,
     * 방송 기록 → 스트리머 순서로 DELETE 쿼리 2번에 끝낸다.
     */
    @Transactional
    public void deleteStreamer(Long id) {
        streamRecordRepository.deleteAllByStreamerId(id);
        if (streamerRepository.bulkDeleteById(id) == 0) {
            throw new ResourceNotFoundException("STREAMER_NOT_FOUND", "해당 스트리머를 찾을 수 없습니다.");
        }
    }
}
