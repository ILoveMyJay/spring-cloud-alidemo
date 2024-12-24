package com.example.api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/users")
    public List<String> getAllUsers() {
        // 模拟获取所有用户列表
        return Arrays.asList("user1", "user2", "user3");
    }

    @PostMapping("/users/{username}/disable")
    public Map<String, String> disableUser(@PathVariable String username, @AuthenticationPrincipal UserDetails currentUser) {
        // 模拟禁用用户
        return Map.of("message", "User " + username + " has been disabled by " + currentUser.getUsername());
    }

    @GetMapping("/system/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getSystemStats() {
        // 模拟系统统计信息
        return Map.of(
            "totalUsers", 100,
            "activeUsers", 85,
            "systemLoad", 0.75
        );
    }
} 