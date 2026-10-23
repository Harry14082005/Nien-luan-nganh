package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_histories")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "message_history_id")
    private UUID messageHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;

    @PrePersist
    protected void onEdit() {
        if (this.editedAt == null) {
            this.editedAt = LocalDateTime.now();
        }
    }
}