export const ERR = {
  INVALID_CREDENTIALS: { status: 400, code: 1014, message: "Tên đăng nhập hoặc mật khẩu không chính xác" },
  USER_BANNED: { status: 403, code: 1015, message: "Tài khoản của bạn đã bị khóa bởi quản trị viên" },
  EMAIL_EXISTED: { status: 400, code: 1012, message: "Địa chỉ email đã được sử dụng" },
  PHONE_NUMBER_EXISTED: { status: 400, code: 1013, message: "Số điện thoại đã được đăng ký" },
  PASSWORD_NOT_MATCH: { status: 400, code: 1016, message: "Mật khẩu xác nhận không khớp" },
  CONTACT_REQUIRED: { status: 400, code: 1026, message: "Vui lòng nhập email hoặc số điện thoại để đăng ký" },
  UNAUTHENTICATED: { status: 401, code: 1002, message: "Chưa xác thực thông tin đăng nhập" },
};