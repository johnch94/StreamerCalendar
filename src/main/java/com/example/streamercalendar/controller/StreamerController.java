package com.example.streamercalendar.controller;

import com.example.streamercalendar.dto.StreamerCreateRequest;
import com.example.streamercalendar.dto.StreamerResponse;
import com.example.streamercalendar.service.StreamerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/streamers")
@RequiredArgsConstructor
public class StreamerController {

    private final StreamerService streamerService;

    @GetMapping
    public ResponseEntity<List<StreamerResponse>> getStreamers() {
        return ResponseEntity.ok(streamerService.getStreamers());
    }

    @PostMapping
    public ResponseEntity<StreamerResponse> createStreamer(@Valid @RequestBody StreamerCreateRequest request) {
        StreamerResponse response = streamerService.createStreamer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStreamer(@PathVariable Long id) {
        streamerService.deleteStreamer(id);
        return ResponseEntity.noContent().build();
    }
}
