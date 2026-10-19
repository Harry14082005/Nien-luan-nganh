package com.nhom8.nien_luan_nganh.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Filter chặn trước mỗi HTTP Request (chỉ chạy 1 lần duy nhất trên mỗi request nhờ OncePerRequestFilter).
// Nhiệm vụ:
// 1. Bắt Header Authorization: "Bearer <token>".
// 2. Xác thực tính hợp lệ của token qua JwtService.
// 3. Nếu token hợp lệ, thiết lập đối tượng Authentication vào SecurityContextHolder để các Controller
//    hoặc @PreAuthorize có thể kiểm tra danh tính và quyền hạn của người dùng.
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter { // Dam bao bo lc chay dung 1 lan cho moi request

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Đọc giá trị header Authorization từ Request
        final String authHeader = request.getHeader("Authorization");

        // 2. Nếu không có header này hoặc định dạng không bắt đầu bằng "Bearer ", bỏ
        // qua filter
        // Request sẽ tiếp tục đi qua các filter tiếp theo (các endpoint công khai sẽ vào được bình thường,
        // còn các endpoint bảo mật sẽ bị Security chặn lại ở bước sau).
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Cắt bỏ tiền tố "Bearer " (7 ký tự) để lấy chuỗi token JWT nguyên bản
        final String jwt = authHeader.substring(7);

        try {
            // 3. Trích xuất Subject(userId) từ JWT
            final String subject = jwtService.extractSubject(jwt);

            // 4. Nếu có subject và hiện tại Request này chưa được xác thực trong
            // SecurityContextHolder
            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Nạp thông tin người dùng từ database (loadUserByUsername hỗ trợ tìm bằng
                // UUID)
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(subject);

                // 5. Kiểm tra token có đúng người dùng và chưa hết hạn không
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // Tạo đối tượng xác thực UsernamePasswordAuthenticationToken chứa thông tin
                    // user và roles
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Không cần lưu mật khẩu thô trong SecurityContext
                            userDetails.getAuthorities());//Ds cac quyen

                    // Đính kèm các thông tin phụ trợ của request (IP client, Session ID,...)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Đánh dấu người dùng này ĐÃ XÁC THỰC THÀNH CÔNG vào SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Ghi log cảnh báo nếu token bị giả mạo, hết hạn hoặc sai cấu trúc
            log.error("Không thể xác thực JWT: {}", e.getMessage());
        }

        // 6. Cho phép Request tiếp tục luồng xử lý đến Filter tiếp theo hoặc Controller
        filterChain.doFilter(request, response);
    }
}
