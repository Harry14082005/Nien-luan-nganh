package com.nhom8.nien_luan_nganh.config;

import com.nhom8.nien_luan_nganh.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

//Service quản lý toàn bộ vòng đời của JSON Web Token (JWT).
//Sử dụng thư viện io.jsonwebtoken (JJWT)
//- Tạo Access Token ngắn hạn và Refresh Token dài hạn.
//- Giải mã (Parse) và trích xuất Claims từ chuỗi Token.
//- Xác thực tính toàn vẹn (Signature) và kiểm tra hạn sử dụng (Expiration).
@Service
public class JwtService {

    // Khóa bí mật dùng để ký và xác thực chữ ký điện tử HMAC-SHA256 (ít nhất 256 bits)
    @Value("${jwt.secret}")
    private String secretKey;

    // Thời gian sống của Access Token tính bằng mili-giây (ví dụ: 86400000 ms = 24
    // giờ)
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // Thời gian sống của Refresh Token tính bằng mili-giây (ví dụ: 604800000 ms = 7
    // ngày)
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // Tạo khóa bảo mật SecretKey từ chuỗi cấu hình bí mật bằng thuật toán HMAC-SHA.

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // ExtraClaims nhối thêm thông tin role, username, email, phone để khi Vue.js
    // giải mã thì nó có sẵn hong cần gọi thêm API

    public String generateAccessToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (user.getRole() != null) {
            extraClaims.put("role", user.getRole().name());
        }
        extraClaims.put("username", user.getUsername());
        extraClaims.put("email", user.getEmail());
        extraClaims.put("phoneNumber", user.getPhoneNumber());

        return buildToken(extraClaims, user.getUserId().toString(), jwtExpiration);
    }

    // Tạo chuỗi Refresh Token dài hạn dùng để lấy Access Token mới khi token cũ hết
    public String generateRefreshToken(User user) {
        return buildToken(new HashMap<>(), user.getUserId().toString(), refreshExpiration);
    }

    // Phương thức dùng chung để đóng gói và ký chuỗi JWT theo chuẩn JJWT 0.12.6.
    // -> ghep Header Payload Signature -> header.payload.signature

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // Trích xuất Subject (ở hệ thống này chính là userId) từ JWT Token.

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    //Hàm này tự ép kiểu về UUID chuẩn của Java, giúp tầng Controller/Filter lấy được ID dạng object ngay lập tức.
    public UUID extractUserId(String token) {
        String subject = extractSubject(token);
        return subject != null ? UUID.fromString(subject) : null;
    }

    // Trích xuất thời điểm hết hạn (Expiration date) của Token.
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Hàm generic trích xuất một trường thông tin bất kỳ (Claim) trong Payload bằng
    // Claims Resolver function.

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Giải mã toàn bộ Claims trong Payload của token bằng SecretKey.
    // Nếu token bị giả mạo hoặc sai chữ ký, hàm này sẽ ném ngoại lệ JwtException.
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Kiểm tra xem Token đã hết hạn sử dụng hay chưa.
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Kiểm tra tính hợp lệ toàn diện của Token:
    // 1. Subject của token phải khớp với Username (userId) của UserDetails.
    // 2. Token chưa bị hết hạn.
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractSubject(token);
        return (username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
