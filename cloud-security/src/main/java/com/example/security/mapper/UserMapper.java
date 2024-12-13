package com.example.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.security.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT r.name FROM users u " +
            "JOIN user_roles ur ON u.id = ur.user_id " +
            "JOIN roles r ON ur.role_id = r.id " +
            "WHERE u.username = #{username}")
    Set<String> getUserRoles(String username);

    @Select("SELECT p.name FROM users u " +
            "JOIN user_roles ur ON u.id = ur.user_id " +
            "JOIN role_permissions rp ON ur.role_id = rp.role_id " +
            "JOIN permissions p ON rp.permission_id = p.id " +
            "WHERE u.username = #{username}")
    Set<String> getUserPermissions(String username);
} 