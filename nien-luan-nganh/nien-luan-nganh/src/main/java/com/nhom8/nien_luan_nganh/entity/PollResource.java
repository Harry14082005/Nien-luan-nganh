package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

public class PollResource {
    @EmbeddedId
    private PollResourceId pollResourceId;


    @ManyToOne
    @MapsId("messageId")
    @JoinColumn(name = "message_id")
    Message message;

    @ManyToOne
    @MapsId("pollId")
    @JoinColumn(name = "poll_id")
    Poll poll;

}
