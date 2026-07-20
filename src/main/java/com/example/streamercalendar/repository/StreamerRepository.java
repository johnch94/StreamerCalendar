package com.example.streamercalendar.repository;

import com.example.streamercalendar.domain.Streamer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamerRepository extends JpaRepository<Streamer, Long> {
}
