package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.entity.Message;
import com.nhom8.nien_luan_nganh.repository.MessageRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatContextService {

    private final MessageRepository messageRepository;

    @Getter
    @Builder
    public static class ContextMessageItem {
        private String code;         // m1, m2, m3...
        private UUID messageId;
        private String senderName;
        private String content;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class ContextResult {
        private List<ContextMessageItem> items;
        private Map<String, UUID> codeToMessageIdMap; // Map "m1" -> real UUID
        private String formattedContextText;          // "[m1] Nam: đi ăn lẩu..."
    }

    /**
     * Lấy ngữ cảnh tối đa 10 tin nhắn trước anchor (trong vòng 1 giờ, tổng <= 1500 ký tự)
     * kèm chính tin anchor ở cuối danh sách.
     */
    public ContextResult extractRecentContext(UUID roomId, Message anchor) {
        LocalDateTime anchorTime = anchor.getCreatedAt() != null ? anchor.getCreatedAt() : LocalDateTime.now();
        LocalDateTime oneHourBefore = anchorTime.minusHours(1);

        // Lấy tối đa 10 tin nhắn TEXT gần nhất trước anchor
        List<Message> beforeMessages = messageRepository.findRecentContextMessages(
                roomId,
                anchorTime,
                oneHourBefore,
                PageRequest.of(0, 10));

        // Sắp xếp theo thứ tự thời gian tăng dần (từ cũ đến mới)
        List<Message> ordered = new ArrayList<>(beforeMessages);
        Collections.reverse(ordered);

        // Thêm chính tin anchor vào cuối cùng
        ordered.add(anchor);

        // Cắt từ tin gần anchor ngược lên nếu vượt quá 1500 ký tự (không cắt giữa tin)
        int totalChars = 0;
        List<Message> budgeted = new ArrayList<>();
        for (int i = ordered.size() - 1; i >= 0; i--) {
            Message m = ordered.get(i);
            String content = m.getContent() != null ? m.getContent().trim() : "";
            if (content.length() > 300) {
                content = content.substring(0, 300);
            }
            if (totalChars + content.length() > 1500 && !budgeted.isEmpty()) {
                // Đã vượt ngân sách 1500 ký tự, dừng lấy thêm tin cũ
                break;
            }
            totalChars += content.length();
            budgeted.add(0, m);
        }

        // Đánh số ngắn m1, m2... để AI trích xuất chính xác mà không bịa UUID
        List<ContextMessageItem> items = new ArrayList<>();
        Map<String, UUID> codeMap = new HashMap<>();
        StringBuilder contextTextBuilder = new StringBuilder();

        for (int i = 0; i < budgeted.size(); i++) {
            Message m = budgeted.get(i);
            String code = "m" + (i + 1);
            String sender = m.getSender() != null && m.getSender().getUsername() != null
                    ? m.getSender().getUsername()
                    : "User";
            String content = m.getContent() != null ? m.getContent().trim() : "";
            if (content.length() > 300) {
                content = content.substring(0, 300);
            }

            ContextMessageItem item = ContextMessageItem.builder()
                    .code(code)
                    .messageId(m.getMessageId())
                    .senderName(sender)
                    .content(content)
                    .createdAt(m.getCreatedAt())
                    .build();

            items.add(item);
            codeMap.put(code, m.getMessageId());

            contextTextBuilder.append(String.format("[%s] %s: %s\n", code, sender, content));
        }

        return ContextResult.builder()
                .items(items)
                .codeToMessageIdMap(codeMap)
                .formattedContextText(contextTextBuilder.toString().trim())
                .build();
    }
}

