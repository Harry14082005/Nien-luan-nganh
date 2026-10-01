package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity 
@Table (name = "reminder_resource")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 

public class ReminderResource {

    @EmbeddedId
    private ReminderResourceId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("messageId")  
    @JoinColumn(name = "message_id")
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reminderId")  
    @JoinColumn(name = "reminder_id")
    private Reminder reminder;

    
    
}
