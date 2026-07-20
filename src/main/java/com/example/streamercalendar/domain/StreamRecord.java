package com.example.streamercalendar.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "stream_record")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreamRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "streamer_id", nullable = false)
    private Streamer streamer;

    @Column(name = "broadcast_date", nullable = false)
    private LocalDate broadcastDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform;

    @Column(nullable = false)
    private String title;

    @Column(name = "vod_url")
    private String vodUrl;

    @Column(name = "youtube_url")
    private String youtubeUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public StreamRecord(Streamer streamer, LocalDate broadcastDate, Platform platform,
                         String title, String vodUrl, String youtubeUrl, Source source) {
        this.streamer = streamer;
        this.broadcastDate = broadcastDate;
        this.platform = platform;
        this.title = title;
        this.vodUrl = vodUrl;
        this.youtubeUrl = youtubeUrl;
        this.source = source;
    }

    public void update(LocalDate broadcastDate, Platform platform, String title,
                        String vodUrl, String youtubeUrl) {
        this.broadcastDate = broadcastDate;
        this.platform = platform;
        this.title = title;
        this.vodUrl = vodUrl;
        this.youtubeUrl = youtubeUrl;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
