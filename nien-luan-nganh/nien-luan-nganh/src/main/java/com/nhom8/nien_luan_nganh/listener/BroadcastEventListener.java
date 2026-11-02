package com.nhom8.nien_luan_nganh.listener;

import com.nhom8.nien_luan_nganh.dto.event.BroadcastEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener trung tâm xử lý phát sóng Realtime WebSocket cho toàn bộ hệ thống
 * (Bình chọn Poll, Nhắc hẹn Reminder, Thông báo,...).
 * Đảm bảo chỉ phát sóng sau khi Database đã commit Transaction thành công.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BroadcastEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // phải lưu dữ liệu trước rồi mới phát thông báo
    public void onBroadcastEvent(BroadcastEvent event) {
        log.info("[BROADCAST to {}]", event.getDestination());
        messagingTemplate.convertAndSend(event.getDestination(), event.getPayload());
        // convert: biến đổi payload, ví dụ PollResponse -> chuỗi Json
        // andSend: gửi dữ liệu tới destination

        log.info("[BROADCAST finish]");
    }
}