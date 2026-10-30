package com.nhom8.nien_luan_nganh.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class VotePollRequest {
    private List<UUID> optionIds;     //nếu trống thì bỏ phiếu

}
