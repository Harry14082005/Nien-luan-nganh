package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
<<<<<<< HEAD
@Table(name = "poll_options")
=======
@Table(name="poll_options")
>>>>>>> fbdb5e85ab9add93784081c0956ce19e7236f639
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOption {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "poll_option_id")
    private UUID pollOptionId;

    @Column(name = "option_text")
    private String optionText;

    @ManyToOne
    @JoinColumn(name = "poll_id")
    private Poll poll;

    @OneToMany(mappedBy = "pollOption")
    @Builder.Default
    private List<PollVote> pollVotes = new ArrayList<>();
}
