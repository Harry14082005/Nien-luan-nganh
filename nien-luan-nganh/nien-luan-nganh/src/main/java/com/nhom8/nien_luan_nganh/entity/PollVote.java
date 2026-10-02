package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name="poll_option")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollVote{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "poll_vote_id")
    private UUID pollVoteId;

    @Column(name = "voted_at")
    private LocalDateTime votedAt;

    @ManyToOne
    @JoinColumn(name = "poll_option_id")
    private PollOption pollOption;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    //chưa thêm bên user

    @ManyToOne
    @JoinColumn(name = "poll_id")
    private Poll poll;

}