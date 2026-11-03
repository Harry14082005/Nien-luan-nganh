package com.nhom8.nien_luan_nganh.mapper;

import com.nhom8.nien_luan_nganh.dto.response.TranslationResponse;
import com.nhom8.nien_luan_nganh.entity.Translation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TranslationMapper {

    @Mapping(source = "message.messageId", target = "messageId")
    @Mapping(target = "fromCache", ignore = true)
    TranslationResponse toTranslationResponse(Translation translation);
}