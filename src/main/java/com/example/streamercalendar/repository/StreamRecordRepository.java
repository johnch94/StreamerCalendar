package com.example.streamercalendar.repository;

import com.example.streamercalendar.domain.StreamRecord;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StreamRecordRepository extends JpaRepository<StreamRecord, Long>,
        JpaSpecificationExecutor<StreamRecord> {

    // 응답에 streamerName이 들어가므로 streamer를 join fetch로 함께 읽는다 (LAZY 로딩 N+1 방지)
    @Override
    @EntityGraph(attributePaths = "streamer")
    List<StreamRecord> findAll(Specification<StreamRecord> spec);

    @EntityGraph(attributePaths = "streamer")
    Optional<StreamRecord> findWithStreamerById(Long id);

    // 스트리머 삭제 시 방송 기록을 한 건씩 지우지 않고 DELETE 한 번으로 정리한다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from StreamRecord r where r.streamer.id = :streamerId")
    int deleteAllByStreamerId(@Param("streamerId") Long streamerId);
}
