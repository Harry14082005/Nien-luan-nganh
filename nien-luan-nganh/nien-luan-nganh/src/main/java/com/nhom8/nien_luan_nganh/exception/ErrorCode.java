package com.nhom8.nien_luan_nganh.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // --- Lỗi chung hệ thống (1000 - 1009) ---
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi không xác định từ hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Mã khóa lỗi không hợp lệ", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1002, "Chưa xác thực thông tin đăng nhập", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1003, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    INVALID_INPUT(1004, "Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    NEW_PASSWORD_SAME_AS_OLD(1005, "Mật khẩu mới không được trùng với mật khẩu hiện tại", HttpStatus.BAD_REQUEST),


    // --- Phân hệ Người dùng & Xác thực (1010 - 10..) ---
    USER_NOT_EXISTED(1010, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    USER_EXISTED(1011, "Tên đăng nhập đã tồn tại trên hệ thống", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(1012, "Địa chỉ email đã được sử dụng", HttpStatus.BAD_REQUEST),
    PHONE_NUMBER_EXISTED(1013, "Số điện thoại đã được đăng ký", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1014, "Tên đăng nhập hoặc mật khẩu không chính xác", HttpStatus.BAD_REQUEST),
    USER_BANNED(1015, "Tài khoản của bạn đã bị khóa bởi quản trị viên", HttpStatus.FORBIDDEN),
    PASSWORD_NOT_MATCH(1016, "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),
    // Login & Register
    USERNAME_REQUIRED(1017, "Tên hiển thị không được để trống", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1018, "Tên hiển thị phải từ 2 đến 50 ký tự", HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED(1019, "Email không được để trống", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1020, "Email không đúng định dạng", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(1021, "Mật khẩu không được để trống", HttpStatus.BAD_REQUEST),
    PASSWORD_TOO_SHORT(1022, "Mật khẩu phải từ 6 ký tự trở lên", HttpStatus.BAD_REQUEST),
    CONFIRM_PASSWORD_REQUIRED(1023, "Mật khẩu xác nhận không được để trống", HttpStatus.BAD_REQUEST),
    IDENTIFIER_REQUIRED(1024, "Email hoặc số điện thoại không được để trống", HttpStatus.BAD_REQUEST),
    REFRESH_TOKEN_REQUIRED(1025, "Refresh token không được để trống", HttpStatus.BAD_REQUEST),
    PHONE_REQUIRED(1026, "Số điện thoại không được để trống", HttpStatus.BAD_REQUEST),
    PHONE_INVALID(1027, "Số điện thoại không đúng định dạng", HttpStatus.BAD_REQUEST),
    IDENTIFIER_INVALID(1028, "Email hoặc số điện thoại không đúng định dạng", HttpStatus.BAD_REQUEST),
    OLD_PASSWORD_INCORRECT(1029, "Mật khẩu hiện tại không chính xác", HttpStatus.BAD_REQUEST),

    // --- Phân hệ Phòng chat & Thành viên (1030 - 1049) ---
    ROOM_NOT_FOUND(1030, "Phòng chat không tồn tại", HttpStatus.NOT_FOUND),
    NOT_ROOM_MEMBER(1031, "Bạn không phải là thành viên trong phòng chat này", HttpStatus.FORBIDDEN),
    ALREADY_IN_ROOM(1032, "Người dùng đã tham gia phòng chat", HttpStatus.BAD_REQUEST),
    ROOM_MEMBER_PENDING(1033, "Yêu cầu tham gia nhóm của bạn đang chờ phê duyệt", HttpStatus.BAD_REQUEST),
    ROOM_MEMBER_NOT_FOUND(1034, "Không tìm thấy thông tin thành viên trong nhóm", HttpStatus.NOT_FOUND),
    NOT_ROOM_ADMIN(1035, "Hành động chỉ dành cho Trưởng nhóm hoặc Phó nhóm", HttpStatus.FORBIDDEN),

    // --- Phân hệ Tin nhắn & Dịch thuật (1050 - 1069) ---
    MESSAGE_NOT_FOUND(1050, "Tin nhắn không tồn tại", HttpStatus.NOT_FOUND),
    MESSAGE_REVOKED(1051, "Tin nhắn đã bị thu hồi", HttpStatus.BAD_REQUEST),
    CANNOT_EDIT_MESSAGE(1052, "Bạn chỉ có thể chỉnh sửa tin nhắn của chính mình", HttpStatus.FORBIDDEN),
    CANNOT_REVOKE_MESSAGE(1053, "Bạn không có quyền thu hồi tin nhắn này", HttpStatus.FORBIDDEN),
    TRANSLATION_FAILED(1054, "Dịch vụ AI không thể hoàn thành việc dịch tin nhắn", HttpStatus.BAD_GATEWAY),
    REACTION_INVALID(1055, "Loại cảm xúc tin nhắn không hợp lệ", HttpStatus.BAD_REQUEST),

    // --- Phân hệ Bình chọn (1070 - 1089) ---
    POLL_NOT_FOUND(1070, "Cuộc bình chọn không tồn tại", HttpStatus.NOT_FOUND),
    POLL_CLOSED(1071, "Cuộc bình chọn đã kết thúc hoặc bị đóng", HttpStatus.BAD_REQUEST),
    POLL_OPTION_NOT_FOUND(1072, "Lựa chọn bình chọn không tồn tại", HttpStatus.NOT_FOUND),
    ALREADY_VOTED(1073, "Bạn đã thực hiện bình chọn cho mục này rồi", HttpStatus.BAD_REQUEST),

    // --- Phân hệ Lịch hẹn / Nhắc nhở (1090 - 1109) ---
    REMINDER_NOT_FOUND(1090, "Lịch hẹn không tồn tại", HttpStatus.NOT_FOUND),
    INVALID_REMINDER_TIME(1091, "Thời gian bắt đầu phải trước thời gian kết thúc", HttpStatus.BAD_REQUEST),
    CANNOT_CANCEL_REMINDER(1092, "Bạn không có quyền hủy lịch hẹn này", HttpStatus.FORBIDDEN),
    REMINDER_ALREADY_COMPLETED(1093, "Lịch hẹn đã diễn ra hoàn tất, không thể chỉnh sửa", HttpStatus.BAD_REQUEST),

    // --- Phân hệ Quan hệ bạn bè (1110 - 1120) ---
    FRIENDSHIP_NOT_FOUND(1110, "Mối quan hệ bạn bè không tồn tại", HttpStatus.NOT_FOUND),
    FRIEND_REQUEST_EXISTED(1111, "Yêu cầu kết bạn đã được gửi trước đó", HttpStatus.BAD_REQUEST),
    CANNOT_ADD_YOURSELF(1112, "Không thể gửi yêu cầu kết bạn cho chính bản thân", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}