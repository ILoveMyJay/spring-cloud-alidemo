package com.example.api.controller;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/profile")
    public Map<String, Object> getUserProfile(@RequestHeader("Authorization") String token) {
        // 从token中获取用户信息（token已经在网关层验证过了）
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", "当前登录用户");
        profile.put("email", "user@example.com");
        return profile;
    }

    @PutMapping("/profile")
    public Map<String, String> updateUserProfile(@RequestBody Map<String, String> updates) {
        // 模拟更新用户资料
        return Map.of("message", "Profile updated successfully");
    }
} 