package com.loadtest.auth.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loadtest.auth.domain.RefreshToken;
import com.loadtest.auth.domain.User;
import com.loadtest.auth.dto.LoginRequest;
import com.loadtest.auth.dto.RegisterRequest;
import com.loadtest.auth.dto.TokenResponse;
import com.loadtest.auth.exception.DuplicateResourceException;
import com.loadtest.auth.repository.RefreshTokenRepository;
import com.loadtest.auth.repository.UserRepository;

import io.jsonwebtoken.Claims;

@Service
public class AuthService {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);
        return generateTokenPair(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return generateTokenPair(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        String hash = hashToken(refreshTokenValue);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("Refresh token expired");
        }
        User user = stored.getUser();
        String newAccessToken = jwtService.generateAccessToken(user.getId().toString(), user.getUsername());
        return new TokenResponse(newAccessToken, refreshTokenValue);
    }

    public void logout(String accessToken) {
        try {
            Claims claims = jwtService.validateToken(accessToken);
            long ttlSeconds = claims.getExpiration().toInstant().getEpochSecond() - Instant.now().getEpochSecond();
            if (ttlSeconds > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + accessToken, "1", Duration.ofSeconds(ttlSeconds));
            }
        } catch (Exception ignored) {
            // Token already invalid — nothing to blacklist
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    private TokenResponse generateTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId().toString(), user.getUsername());
        String refreshTokenValue = jwtService.generateRefreshToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(refreshTokenValue));
        refreshToken.setExpiresAt(Instant.now().plus(jwtService.getRefreshTokenTtlDays(), ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshTokenValue);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
