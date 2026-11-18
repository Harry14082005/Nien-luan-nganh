package com.nhom8.nien_luan_nganh.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestionResponse {
    private boolean found; // trả về có thể trả về gọi ý được hay không
    private Object data; // PollSuggestionData hoặc ReminderSuggestionData - Nội dung AI đề xuất
    /*
     * ví dụ
     * {
     * "found": true,
     * "data": {
     * "question": "Trưa nay ăn gì?",
     * "options": ["Lẩu", "Nướng", "Cơm tấm"]
     * }
     * }
     */
    private List<UUID> sourceMessageIds; // danh sách tin nhắn mà lấy để thực hiện việc gợi ý

    // phải gọi AiSuggestionResponse.PollSuggestionData.
    // có thể tạo lớp riêng, nhưng mà chỉ phục vụ này cho nên để chung trong đây
    // cũng được

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PollSuggestionData {
        private String question;
        private List<String> options;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReminderSuggestionData {
        private String title;
        private String description;
        private String startAt;
        private String endAt;
    }
}
