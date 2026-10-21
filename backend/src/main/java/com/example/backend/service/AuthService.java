package com.example.backend.service;

import com.example.backend.domain.User;
import com.example.backend.dto.AuthRequest;
import com.example.backend.dto.AuthResponse;
import com.example.backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;

    @Value("${app.jwt.secret:infra-test-2026-blue-green-stateless-jwt-secret-key-minimum-256-bits}")
    private String jwtSecret;

    private static final long EXPIRATION_MS = 1000L * 60 * 60 * 24 * 7; // 7일

    @Transactional
    public AuthResponse signup(AuthRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디(닉네임)입니다: " + username);
        }

        User user = User.builder()
            .username(username)
            .passwordHash(hashPassword(request.password()))
            .build();
        User saved = userRepository.save(user);
        log.info("New user signed up: {}", saved.getUsername());

        String token = generateJwtToken(saved.getUsername());
        return new AuthResponse(saved.getId(), saved.getUsername(), token);
    }

    public AuthResponse login(AuthRequest request) {
        String username = request.username().trim();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디이거나 비밀번호가 일치하지 않습니다."));

        String hashedInput = hashPassword(request.password());
        if (!MessageDigest.isEqual(
                user.getPasswordHash().getBytes(StandardCharsets.UTF_8),
                hashedInput.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("존재하지 않는 아이디이거나 비밀번호가 일치하지 않습니다.");
        }

        log.info("User logged in: {}", user.getUsername());
        String token = generateJwtToken(user.getUsername());
        return new AuthResponse(user.getId(), user.getUsername(), token);
    }

    public AuthResponse getCurrentUser(String authHeader) {
        String username = extractUsernameFromHeader(authHeader);
        if (username == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new SecurityException("유효하지 않은 사용자입니다. 다시 로그인해주세요."));
        return new AuthResponse(user.getId(), user.getUsername(), extractRawToken(authHeader));
    }

    public String extractUsernameFromHeader(String authHeader) {
        String token = extractRawToken(authHeader);
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractRawToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return authHeader.trim();
    }

    public String generateJwtToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);
        return Jwts.builder()
            .subject(username)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(getSigningKey())
            .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("infra-salt-2026:" + rawPassword).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }
}
