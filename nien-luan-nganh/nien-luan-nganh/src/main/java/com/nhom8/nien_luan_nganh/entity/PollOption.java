package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "poll_options")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(name = "option_text", nullable = false)
    private String optionText;
}
