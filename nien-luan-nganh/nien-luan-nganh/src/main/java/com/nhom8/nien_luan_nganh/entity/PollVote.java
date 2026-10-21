package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "poll_votes", uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollVote {

//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    @Column(name = "poll_vote_id")
//    private UUID pollVoteId;

    @EmbeddedId
    private PollVoteId pollVoteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_option_id", nullable = false)
    @MapsId("pollOptionId")
    private PollOption pollOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @MapsId("userId")
    private User user;

    @Column(name = "voted_at", updatable = false)
    private LocalDateTime votedAt;

    @PrePersist
    protected void onCreate() {
        if (this.votedAt == null) {
            this.votedAt = LocalDateTime.now();
        }
    }
}