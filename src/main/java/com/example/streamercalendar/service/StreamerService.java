package com.example.streamercalendar.service;

import com.example.streamercalendar.domain.Streamer;
import com.example.streamercalendar.dto.StreamerCreateRequest;
import com.example.streamercalendar.dto.StreamerResponse;
import com.example.streamercalendar.exception.ResourceNotFoundException;
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

    @Transactional
    public void deleteStreamer(Long id) {
        Streamer streamer = streamerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "STREAMER_NOT_FOUND", "해당 스트리머를 찾을 수 없습니다."));
        streamerRepository.delete(streamer);
    }
}
