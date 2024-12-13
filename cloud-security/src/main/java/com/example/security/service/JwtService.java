package com.example.security.service;

import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String ACCESS_TOKEN_PREFIX = "access_token:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final RedisTemplate<String, String> redisTemplate;
    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateAccessToken(String username, Set<String> roles, Set<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        String token = generateToken(claims, username, accessTokenExpiration);
        
        // 将access token存入Redis
        redisTemplate.opsForValue().set(
            ACCESS_TOKEN_PREFIX + username,
            token,
            accessTokenExpiration,
            TimeUnit.SECONDS
        );
        
        return token;
    }

    public String generateRefreshToken(String username) {
        String token = generateToken(new HashMap<>(), username, refreshTokenExpiration);
        
        // 将refresh token存入Redis
        redisTemplate.opsForValue().set(
            REFRESH_TOKEN_PREFIX + username,
            token,
            refreshTokenExpiration,
            TimeUnit.SECONDS
        );
        
        return token;
    }

    public void invalidateTokens(String username) {
        // 删除Redis中的所有token
        redisTemplate.delete(ACCESS_TOKEN_PREFIX + username);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + username);
    }

    public boolean isAccessTokenValid(String token) {
        try {
            String username = extractUsername(token);
            if (StrUtil.isBlank(username)) {
                return false;
            }

            // 验证token是否过期
            Claims claims = extractAllClaims(token);
            if (claims.getExpiration().before(new Date())) {
                return false;
            }

            // 验证是否是access token
            String storedAccessToken = redisTemplate.opsForValue().get(ACCESS_TOKEN_PREFIX + username);
            return token.equals(storedAccessToken);
            
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            String username = extractUsername(token);
            if (StrUtil.isBlank(username)) {
                return false;
            }

            // 验证token是否过期
            Claims claims = extractAllClaims(token);
            if (claims.getExpiration().before(new Date())) {
                return false;
            }

            // 验证是否是refresh token
            String storedRefreshToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + username);
            return token.equals(storedRefreshToken);
            
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(String token) {
        try {
            Claims claims = extractAllClaims(token);
            List<String> roles = (List<String>) claims.get("roles");
            return roles != null ? new HashSet<>(roles) : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractPermissions(String token) {
        try {
            Claims claims = extractAllClaims(token);
            List<String> permissions = (List<String>) claims.get("permissions");
            return permissions != null ? new HashSet<>(permissions) : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private String generateToken(Map<String, Object> extraClaims, String username, long expiration) {
        return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
} 