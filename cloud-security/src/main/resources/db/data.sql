USE cloud_security;

-- 插入角色
INSERT INTO roles (name, description) VALUES
('ADMIN', '系统管理员'),
('USER', '普通用户');

-- 插入权限
INSERT INTO permissions (name, description) VALUES
('user:read', '读取用户信息'),
('user:write', '修改用户信息'),
('user:delete', '删除用户'),
('system:read', '读取系统信息'),
('system:write', '修改系统设置');

-- 插入测试用户 (密码都是 'password')
INSERT INTO users (username, password, email, enabled) VALUES
('admin', '$2a$10$EqWWNXxgTqwvR.7ghBpFruYJ0K4ZB9yY5VWBz5YmYHQdFG3HGtknW', 'admin@example.com', true),
('user', '$2a$10$EqWWNXxgTqwvR.7ghBpFruYJ0K4ZB9yY5VWBz5YmYHQdFG3HGtknW', 'user@example.com', true);

-- 分配角色给用户
INSERT INTO user_roles (user_id, role_id) VALUES
((SELECT id FROM users WHERE username = 'admin'), (SELECT id FROM roles WHERE name = 'ADMIN')),
((SELECT id FROM users WHERE username = 'admin'), (SELECT id FROM roles WHERE name = 'USER')),
((SELECT id FROM users WHERE username = 'user'), (SELECT id FROM roles WHERE name = 'USER'));

-- 分配权限给角色
INSERT INTO role_permissions (role_id, permission_id) VALUES
((SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM permissions WHERE name = 'user:read')),
((SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM permissions WHERE name = 'user:write')),
((SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM permissions WHERE name = 'user:delete')),
((SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM permissions WHERE name = 'system:read')),
((SELECT id FROM roles WHERE name = 'ADMIN'), (SELECT id FROM permissions WHERE name = 'system:write')),
((SELECT id FROM roles WHERE name = 'USER'), (SELECT id FROM permissions WHERE name = 'user:read')),
((SELECT id FROM roles WHERE name = 'USER'), (SELECT id FROM permissions WHERE name = 'user:write')); 