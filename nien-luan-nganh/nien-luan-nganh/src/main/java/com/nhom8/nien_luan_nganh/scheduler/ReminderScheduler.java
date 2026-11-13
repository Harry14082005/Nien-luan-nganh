package com.nhom8.nien_luan_nganh.scheduler;

import com.nhom8.nien_luan_nganh.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler tự động chạy ngầm để quản lý vòng đời và trạng thái của Lịch hẹn (Reminders).
 * Quét cơ sở dữ liệu định kỳ để tự động đóng các sự kiện đã kết thúc và phát WebSocket cập nhật realtime.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderService reminderService;

    /**
     * Tự động quét và chuyển trạng thái các lịch hẹn đã kết thúc sang DONE mỗi 45 giây.
     * - Mốc hết hạn: endAt (nếu có) hoặc startAt (nếu không có endAt).
     * - Khi hết hạn: Tự động đổi status = DONE và phát WebSocket tới phòng chat tương ứng.
     */
    @Scheduled(fixedDelay = 45000, initialDelay = 15000)
    public void autoExpireReminders() {
        try {
            int count = reminderService.scanAndExpireReminders();
            if (count > 0) {
                log.info("⏰ [ReminderScheduler] Đã tự động cập nhật {} lịch hẹn hết hạn sang trạng thái DONE.", count);
            }
        } catch (Exception e) {
            log.error("❌ [ReminderScheduler] Lỗi khi quét lịch hẹn hết hạn: {}", e.getMessage(), e);
        }
    }
}

