package com.example.streamercalendar.service;

import com.example.streamercalendar.domain.Platform;
import com.example.streamercalendar.domain.Source;
import com.example.streamercalendar.domain.StreamRecord;
import com.example.streamercalendar.domain.Streamer;
import com.example.streamercalendar.dto.StreamRecordRequest;
import com.example.streamercalendar.dto.StreamRecordResponse;
import com.example.streamercalendar.exception.ResourceNotFoundException;
import com.example.streamercalendar.repository.StreamRecordRepository;
import com.example.streamercalendar.repository.StreamerRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StreamRecordService {

    private final StreamRecordRepository streamRecordRepository;
    private final StreamerRepository streamerRepository;

    public List<StreamRecordResponse> getStreamRecords(Long streamerId, Platform platform,
                                                         Integer year, Integer month) {
        Specification<StreamRecord> spec = buildSpecification(streamerId, platform, year, month);
        return streamRecordRepository.findAll(spec).stream()
                .map(StreamRecordResponse::from)
                .toList();
    }

    public StreamRecordResponse getStreamRecord(Long id) {
        return StreamRecordResponse.from(findRecordOrThrow(id));
    }

    @Transactional
    public StreamRecordResponse createStreamRecord(StreamRecordRequest request) {
        Streamer streamer = streamerRepository.findById(request.streamerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "STREAMER_NOT_FOUND", "해당 스트리머를 찾을 수 없습니다."));

        StreamRecord record = new StreamRecord(
                streamer,
                request.broadcastDate(),
                request.platform(),
                request.title(),
                request.vodUrl(),
                request.youtubeUrl(),
                Source.MANUAL
        );
        return StreamRecordResponse.from(streamRecordRepository.save(record));
    }

    @Transactional
    public StreamRecordResponse updateStreamRecord(Long id, StreamRecordRequest request) {
        StreamRecord record = findRecordOrThrow(id);

        if (!record.getStreamer().getId().equals(request.streamerId())) {
            Streamer streamer = streamerRepository.findById(request.streamerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "STREAMER_NOT_FOUND", "해당 스트리머를 찾을 수 없습니다."));
            record.setStreamer(streamer);
        }

        record.update(
                request.broadcastDate(),
                request.platform(),
                request.title(),
                request.vodUrl(),
                request.youtubeUrl()
        );
        return StreamRecordResponse.from(record);
    }

    @Transactional
    public void deleteStreamRecord(Long id) {
        StreamRecord record = findRecordOrThrow(id);
        streamRecordRepository.delete(record);
    }

    private StreamRecord findRecordOrThrow(Long id) {
        return streamRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "STREAM_NOT_FOUND", "해당 방송 기록을 찾을 수 없습니다."));
    }

    private Specification<StreamRecord> buildSpecification(Long streamerId, Platform platform,
                                                              Integer year, Integer month) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (streamerId != null) {
                predicates.add(cb.equal(root.get("streamer").get("id"), streamerId));
            }
            if (platform != null) {
                predicates.add(cb.equal(root.get("platform"), platform));
            }
            if (year != null && month != null) {
                LocalDate start = LocalDate.of(year, month, 1);
                LocalDate end = start.plusMonths(1);
                predicates.add(cb.and(
                        cb.greaterThanOrEqualTo(root.get("broadcastDate"), start),
                        cb.lessThan(root.get("broadcastDate"), end)
                ));
            } else if (year != null) {
                LocalDate start = LocalDate.of(year, 1, 1);
                LocalDate end = start.plusYears(1);
                predicates.add(cb.and(
                        cb.greaterThanOrEqualTo(root.get("broadcastDate"), start),
                        cb.lessThan(root.get("broadcastDate"), end)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
