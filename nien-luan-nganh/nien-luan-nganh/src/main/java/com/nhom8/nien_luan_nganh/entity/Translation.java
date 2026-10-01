package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "translations")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class Translation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "trans_id")
    private UUID transID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "source_language", length = 20, nullable = false)
    private String sourceLanguage;

    @Column(name = "target_language", length = 20, nullable = false)
    private String targetLanguage;

    @Column(name = "translated_text", columnDefinition = "TEXT", nullable = false)
    private String translatedText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
