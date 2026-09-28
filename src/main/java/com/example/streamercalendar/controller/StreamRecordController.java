package com.example.streamercalendar.controller;

import com.example.streamercalendar.domain.Platform;
import com.example.streamercalendar.dto.StreamRecordRequest;
import com.example.streamercalendar.dto.StreamRecordResponse;
import com.example.streamercalendar.service.StreamRecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/streams")
@RequiredArgsConstructor
public class StreamRecordController {

    private final StreamRecordService streamRecordService;

    @GetMapping
    public ResponseEntity<List<StreamRecordResponse>> getStreamRecords(
            @RequestParam(required = false) Long streamerId,
            @RequestParam(required = false) Platform platform,
            // 범위를 벗어나면 LocalDate 생성 단계에서 500이 나므로 컨트롤러에서 400으로 막는다
            @RequestParam(required = false) @Min(1) @Max(9999) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month
    ) {
        return ResponseEntity.ok(streamRecordService.getStreamRecords(streamerId, platform, year, month));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StreamRecordResponse> getStreamRecord(@PathVariable Long id) {
        return ResponseEntity.ok(streamRecordService.getStreamRecord(id));
    }

    @PostMapping
    public ResponseEntity<StreamRecordResponse> createStreamRecord(@Valid @RequestBody StreamRecordRequest request) {
        StreamRecordResponse response = streamRecordService.createStreamRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StreamRecordResponse> updateStreamRecord(
            @PathVariable Long id,
            @Valid @RequestBody StreamRecordRequest request
    ) {
        return ResponseEntity.ok(streamRecordService.updateStreamRecord(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStreamRecord(@PathVariable Long id) {
        streamRecordService.deleteStreamRecord(id);
        return ResponseEntity.noContent().build();
    }
}
