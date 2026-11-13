package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.dto.request.CreateReminderRequest;
import com.nhom8.nien_luan_nganh.dto.request.RsvpReminderRequest;
import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.ReminderResponse;
import com.nhom8.nien_luan_nganh.service.ReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({ "/api/v1/rooms/{roomId}/reminders", "/api/v1/room/{roomId}/reminders" })
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    /**
     * STT 46: Tạo lịch hẹn cá nhân hoặc nhóm
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReminderResponse>> createReminder(
            Authentication auth,
            @PathVariable UUID roomId,
            @Valid @RequestBody CreateReminderRequest request) {
        UUID currentUserId = UUID.fromString(auth.getName());
        ReminderResponse response = reminderService.createReminder(roomId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo lịch hẹn thành công"));
    }

    /**
     * STT 45: Lấy danh sách lịch hẹn trong phòng (cho Drawer/Widget)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReminderResponse>>> getRoomReminders(
            Authentication auth,
            @PathVariable UUID roomId) {
        UUID currentUserId = UUID.fromString(auth.getName());
        List<ReminderResponse> response = reminderService.getRoomReminders(roomId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách lịch hẹn thành công"));
    }

    /**
     * STT 47: Xem chi tiết 1 lịch hẹn
     */
    @GetMapping("/{reminderId}")
    public ResponseEntity<ApiResponse<ReminderResponse>> getReminderDetail(
            Authentication auth,
            @PathVariable UUID roomId,
            @PathVariable UUID reminderId) {
        UUID currentUserId = UUID.fromString(auth.getName());
        ReminderResponse response = reminderService.getReminderDetail(roomId, reminderId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin lịch hẹn thành công"));
    }

    /**
     * STT 49: Phản hồi lịch hẹn (RSVP: ACCEPTED / DECLINED)
     * Hỗ trợ cả PUT và PATCH để Frontend dễ dàng tương thích
     */
    @RequestMapping(value = "/{reminderId}/rsvp", method = { RequestMethod.PUT, RequestMethod.PATCH })
    public ResponseEntity<ApiResponse<ReminderResponse>> respondRsvp(
            Authentication auth,
            @PathVariable UUID roomId,
            @PathVariable UUID reminderId,
            @Valid @RequestBody RsvpReminderRequest request) {
        UUID currentUserId = UUID.fromString(auth.getName());
        ReminderResponse response = reminderService.respondRsvp(roomId, reminderId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật phản hồi lịch hẹn thành công"));
    }

    /**
     * STT 48: Hủy lịch hẹn
     * Hỗ trợ cả DELETE và PUT /{reminderId}/cancel theo chuẩn RESTful
     */
    @RequestMapping(value = { "/{reminderId}", "/{reminderId}/cancel" }, method = { RequestMethod.DELETE,
            RequestMethod.PUT })
    public ResponseEntity<ApiResponse<ReminderResponse>> cancelReminder(
            Authentication auth,
            @PathVariable UUID roomId,
            @PathVariable UUID reminderId) {
        UUID currentUserId = UUID.fromString(auth.getName());
        ReminderResponse response = reminderService.cancelReminder(roomId, reminderId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Hủy lịch hẹn thành công"));
    }
}