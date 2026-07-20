package com.example.streamercalendar.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "streamer")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Streamer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "streamer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StreamRecord> streamRecords = new ArrayList<>();

    public Streamer(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
