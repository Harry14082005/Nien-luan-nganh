package com.nhom8.nien_luan_nganh.entity;

import java.util.UUID;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

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
    @Column(name = "history_id")
    private UUID historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(nullable = false)
    private String content;

    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;

    @PrePersist
    protected void onEdit() {
        this.editedAt = LocalDateTime.now();
    }

}
