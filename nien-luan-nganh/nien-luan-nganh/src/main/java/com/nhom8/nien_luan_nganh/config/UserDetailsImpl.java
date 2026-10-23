package com.nhom8.nien_luan_nganh.config;

import com.nhom8.nien_luan_nganh.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

//"Vỏ bọc" cho Entity User, cung cấp thông tin xthuc, pquyen va status hoat dong cua account cho Spring Security framework

@Getter
public class UserDetailsImpl implements UserDetails {

    // Thực thể User gốc lưu trong database
    private final User user;

    public UserDetailsImpl(User user) {
        this.user = user;
    }

    public UUID getUserId() {
        return user.getUserId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getPhoneNumber() {
        return user.getPhoneNumber();
    }

    // Cung cấp danh sách quyền cho Spring Security.
    // Tự động gắn tiền tố "ROLE_" (ví dụ: ROLE_USER, ROLE_ADMIN) để tương thích với
    // @PreAuthorize("hasRole('ADMIN')").

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user.getRole() == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    // Lấy mật khẩu đã được mã hóa BCrypt của người dùng để Spring Security đối soát
    // khi đăng nhập.
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    // Lấy tên định danh duy nhất của người dùng trong hệ thống Spring Security.
    // Ta chọn trả về UUID dạng String (thay vì email hoặc sđt) vì UUID luôn tồn tại
    // và không bao giờ đổi.
    @Override
    public String getUsername() {
        return user.getUserId() != null ? user.getUserId().toString() : "";
    }


    @Override
    public boolean isAccountNonExpired() {
        return true; // Tài khoản không bao giờ hết hạn
    }

    //Neu tai khoan bị khóa Spring Security sẽ ném ngoại lệ LockedException
    @Override
    public boolean isAccountNonLocked() {
        return user.getIsBanned() == null || !user.getIsBanned();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Kiểm tra MK hết hạn chưa, mặc định là chưa hết
    }

    @Override
    public boolean isEnabled() {
        return true; // Default la true, để isAccountNonLocked() quyết.
    }
}
