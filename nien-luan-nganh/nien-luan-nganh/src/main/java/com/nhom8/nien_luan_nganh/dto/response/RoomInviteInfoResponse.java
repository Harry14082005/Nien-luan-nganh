package com.nhom8.nien_luan_nganh.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomInviteInfoResponse {
    private UUID roomId;
    private String roomName;
    private String roomAvatarURL;
    private int memberCount;
    private boolean requireApproval;
    private boolean isAlreadyMember;
}

