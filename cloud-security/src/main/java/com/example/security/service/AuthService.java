package com.example.security.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.security.dto.AuthRequest;
import com.example.security.dto.AuthResponse;
import com.example.security.dto.RegisterRequest;
import com.example.security.entity.User;
import com.example.security.entity.UserRole;
import com.example.security.mapper.UserMapper;
import com.example.security.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    @Transactional
    public void register(RegisterRequest request) {
        log.info("Attempting to register user: {}", request.getUsername());
        
        // 检查用户名是否已存在
        if (userMapper.exists(new QueryWrapper<User>().eq("username", request.getUsername()))) {
            log.warn("Registration failed - username already exists: {}", request.getUsername());
            throw new IllegalStateException("Username already exists");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setEnabled(true);
        userMapper.insert(user);

        // 为新用户分配USER角色
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(2L);
        userRoleMapper.insert(userRole);
        
        log.info("Successfully registered user: {}", request.getUsername());
    }

    public AuthResponse authenticate(AuthRequest request) {
        log.info("Attempting to authenticate user: {}", request.getUsername());
        
        // 查找用户
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", request.getUsername()));
        if (user == null || !user.getEnabled()) {
            log.warn("Authentication failed - user not found or disabled: {}", request.getUsername());
            throw new IllegalStateException("User not found or disabled");
        }

        // 验证密码
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            log.warn("Authentication failed - invalid password for user: {}", request.getUsername());
            throw new IllegalStateException("Invalid password");
        }

        // 获取用户角色和权限
        Set<String> roles = userMapper.getUserRoles(user.getUsername());
        Set<String> permissions = userMapper.getUserPermissions(user.getUsername());
        log.debug("User roles: {}, permissions: {}", roles, permissions);

        // 生成token
        String accessToken = jwtService.generateAccessToken(user.getUsername(), roles, permissions);
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());
        log.info("Successfully authenticated user: {}", request.getUsername());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        log.info("Attempting to refresh token");
        
        // 验证refresh token
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            log.warn("Token refresh failed - invalid refresh token");
            throw new IllegalStateException("Invalid refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);
        log.debug("Refreshing token for user: {}", username);
        
        // 获取用户角色和权限
        Set<String> roles = userMapper.getUserRoles(username);
        Set<String> permissions = userMapper.getUserPermissions(username);

        // 生成新的access token和refresh token
        String newAccessToken = jwtService.generateAccessToken(username, roles, permissions);
        String newRefreshToken = jwtService.generateRefreshToken(username);
        log.info("Successfully refreshed token for user: {}", username);

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .build();
    }

    public void logout(String token) {
        String username = jwtService.extractUsername(token);
        log.info("Logging out user: {}", username);
        jwtService.invalidateTokens(username);
        log.info("Successfully logged out user: {}", username);
    }

    public Map<String, Object> validateToken(String token) {
        boolean isValid = jwtService.isAccessTokenValid(token);
        String username = jwtService.extractUsername(token);
        Set<String> roles = jwtService.extractRoles(token);
        log.debug("Token validation for user {}: {}", username, isValid);
        return Map.of("isValid", isValid, "username", username, "roles", roles);
    }
} 