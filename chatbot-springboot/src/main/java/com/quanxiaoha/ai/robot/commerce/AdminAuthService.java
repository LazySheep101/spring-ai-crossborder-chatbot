package com.quanxiaoha.ai.robot.commerce;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quanxiaoha.ai.robot.domain.dos.AdminUserDO;
import com.quanxiaoha.ai.robot.domain.mapper.AdminUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 商家登录：BCrypt 校验密码，签发 HMAC JWT。
 */
@Service
public class AdminAuthService {

    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private AdminJwtService adminJwtService;

    public record LoginResult(String token, String username, String displayName, String role) {
    }

    public LoginResult login(String username, String password) {
        AdminUserDO user = adminUserMapper.selectOne(Wrappers.<AdminUserDO>lambdaQuery()
                .eq(AdminUserDO::getUsername, username).last("limit 1"));
        if (user == null || !matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!isBcrypt(user.getPassword())) {
            user.setPassword(BCrypt.hashpw(password));
            adminUserMapper.updateById(user);
        }
        String token = adminJwtService.issue(user.getUsername());
        return new LoginResult(token, user.getUsername(), user.getDisplayName(), user.getRole());
    }

    public AdminUserDO requireUser(String token) {
        String username = adminJwtService.requireUsername(token);
        AdminUserDO user = adminUserMapper.selectOne(Wrappers.<AdminUserDO>lambdaQuery()
                .eq(AdminUserDO::getUsername, username).last("limit 1"));
        if (user == null) {
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }
        return user;
    }

    public AdminUserDO requireSupervisor(String token) {
        AdminUserDO user = requireUser(token);
        if (!"supervisor".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("需要 supervisor 角色才能审批");
        }
        return user;
    }

    public static String hashPassword(String raw) {
        return BCrypt.hashpw(raw);
    }

    public void migratePlaintextPasswords() {
        List<AdminUserDO> users = adminUserMapper.selectList(Wrappers.emptyWrapper());
        for (AdminUserDO u : users) {
            if (u.getPassword() != null && !isBcrypt(u.getPassword())) {
                u.setPassword(BCrypt.hashpw(u.getPassword()));
                adminUserMapper.updateById(u);
            }
        }
    }

    private static boolean matches(String raw, String stored) {
        if (!StringUtils.hasText(stored)) {
            return false;
        }
        if (isBcrypt(stored)) {
            return BCrypt.checkpw(raw, stored);
        }
        return stored.equals(raw);
    }

    private static boolean isBcrypt(String stored) {
        return stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"));
    }
}
