package com.example.streamercalendar.repository;

import com.example.streamercalendar.domain.Streamer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StreamerRepository extends JpaRepository<Streamer, Long> {

    // 엔티티를 읽지 않고 바로 DELETE (반환값: 삭제된 행 수, 0이면 없는 스트리머)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Streamer s where s.id = :id")
    int bulkDeleteById(@Param("id") Long id);
}
