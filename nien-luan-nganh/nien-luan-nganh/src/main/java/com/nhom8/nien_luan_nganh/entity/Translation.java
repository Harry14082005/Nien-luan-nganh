package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "translations", uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "target_language"}))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "trans_id")
    private UUID transId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "source_language", length = 20)
    private String sourceLanguage;

    @Column(name = "target_language", length = 20)
    private String targetLanguage;

    @Column(name = "translated_text", columnDefinition = "TEXT")
    private String translatedText;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
