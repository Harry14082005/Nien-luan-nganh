package com.nhom8.nien_luan_nganh.config;

import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Nho anotation nay ma cac anotation o controller hay service moi có hiệu lực
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, UserDetailsServiceImpl userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    // Băm kèm salt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Kết hợp giữa UserDetailsService (để truy vấn database) và PasswordEncoder (để
    // so sánh mật khẩu).

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Cung cấp Bean AuthenticationManager từ cấu hình Spring Security.
    // Dùng trong AuthService để thực hiện đăng nhập và xác thực tài khoản.

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Cấu hình CORS (Cross-Origin Resource Sharing) cho phép Frontend Vue.js
    // chạy ở port 5173 có thể gửi request có kèm Header và Cookie/Token sang
    // Backend port 8080.

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Cho phép các domain nguồn từ application.properties
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        // Cho phép các phương thức HTTP
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // Cho phép mọi header (bao gồm Authorization, Content-Type,...)
        configuration.setAllowedHeaders(List.of("*"));
        // Cho phép gửi kèm thông tin định danh (Credentials)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Cấu hình chuỗi lọc bảo mật SecurityFilterChain:
    // Định nghĩa các quy tắc bảo mật áp dụng cho toàn bộ HTTP Request gửi đến
    // server.

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF vì hệ thống sử dụng REST API xác thực qua JWT token stateless, không
                // dùng Cookie Session
                .csrf(AbstractHttpConfigurer::disable)

                // Áp dụng cấu hình CORS đã định nghĩa ở trên
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Cấu hình Session Stateless: Không tạo HTTP Session trên server, mỗi request
                // đều độc lập qua JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Phân quyền cho các đường dẫn URL (Endpoints)
                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập công khai không cần đăng nhập đối với các API
                        // Authentication
                        .requestMatchers("/api/v1/auth/**", "/auth/**").permitAll()

                        // Cho phép truy cập công khai endpoint kết nối WebSocket Handshake
                        .requestMatchers("/ws/**").permitAll()

                        // Cho phép truy cập công khai các file upload tĩnh (ảnh đại diện, ảnh chat,
                        // file đính kèm)
                        .requestMatchers("/uploads/**").permitAll()

                        // Tất cả các API còn lại đều bắt buộc phải đăng nhập (gửi kèm Bearer JWT hợp
                        // lệ)
                        .anyRequest().authenticated())

                // Thiết lập AuthenticationProvider để kiểm tra tài khoản và mật khẩu
                .authenticationProvider(authenticationProvider())

                // Thêm JwtAuthenticationFilter đứng TRƯỚC UsernamePasswordAuthenticationFilter
                // để lọc token trước
                // Nhờ nó mà mỗi request sẽ được bốc tách và giải mã trước tiên
                // 1. Nếu user hợp lệ thì đã có sẵn info trong SecurityContextHolder trước khi
                // Spring Security chạy bước ktra quyền
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Xử lý tùy biến ngoại lệ bảo mật trả về định dạng ApiResponse JSON thống nhất
                .exceptionHandling(exceptions -> exceptions
                        // Khi người dùng chưa đăng nhập hoặc gửi token sai / hết hạn (HTTP 401
                        // Unauthorized)
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            String json = String.format("{\"code\":%d,\"message\":\"%s\",\"result\":null}",
                                    ErrorCode.UNAUTHENTICATED.getCode(),
                                    ErrorCode.UNAUTHENTICATED.getMessage());
                            response.getWriter().write(json);
                        })

                        // Khi người dùng đã đăng nhập nhưng không đủ quyền hạn thực hiện hành động
                        // (HTTP 403 Forbidden)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            String json = String.format("{\"code\":%d,\"message\":\"%s\",\"result\":null}",
                                    ErrorCode.UNAUTHORIZED.getCode(),
                                    ErrorCode.UNAUTHORIZED.getMessage());
                            response.getWriter().write(json);
                        }));

        return http.build();
    }
}
