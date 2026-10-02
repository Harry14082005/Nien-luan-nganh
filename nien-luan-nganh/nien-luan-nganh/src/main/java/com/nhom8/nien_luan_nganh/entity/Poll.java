package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="poll")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poll{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "poll_id")
    private UUID pollId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "question")
    private String question;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "poll")
    private List<PollResource> pollResources = new ArrayList<>();

    @OneToMany(mappedBy = "poll")
    private List<PollOption> pollOptions = new ArrayList<>();

    @OneToMany(mappedBy = "poll")
    private List<PollVote> pollVotes = new ArrayList<>();

}