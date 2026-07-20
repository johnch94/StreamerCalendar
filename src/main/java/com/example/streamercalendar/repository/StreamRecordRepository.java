package com.example.streamercalendar.repository;

import com.example.streamercalendar.domain.StreamRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StreamRecordRepository extends JpaRepository<StreamRecord, Long>,
        JpaSpecificationExecutor<StreamRecord> {
}
