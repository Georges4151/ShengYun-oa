package com.ceams.service;

import com.ceams.model.entity.User;
import com.ceams.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserSessionService {

    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Integer getCurrentUserId() {
        // 优先从Spring Security上下文获取用户ID（用于JWT认证）
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) auth.getPrincipal()).getUserId();
        }
        return currentUser != null ? currentUser.getUserId() : null;
    }

    public String getCurrentUserName() {
        // 优先从Spring Security上下文获取用户名
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) auth.getPrincipal()).getUsername();
        }
        return currentUser != null ? currentUser.getName() : null;
    }

    public String getCurrentUserRole() {
        // 优先从Spring Security上下文获取角色
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) auth.getPrincipal()).getRole();
        }
        return currentUser != null ? currentUser.getRole() : null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void clearSession() {
        this.currentUser = null;
    }

    public boolean hasApprovalPermission() {
        if (currentUser == null) {
            return false;
        }
        String role = currentUser.getRole();
        if (role == null) {
            return false;
        }
        String roleLower = role.toLowerCase();
        return "manager".equals(roleLower) || "admin".equals(roleLower);
    }

    public boolean isManager() {
        if (currentUser == null) {
            return false;
        }
        String role = currentUser.getRole();
        if (role == null) {
            return false;
        }
        return "manager".equalsIgnoreCase(role);
    }

    public boolean isAdmin() {
        if (currentUser == null) {
            return false;
        }
        String role = currentUser.getRole();
        if (role == null) {
            return false;
        }
        return "admin".equalsIgnoreCase(role);
    }

    /**
     * 检查用户是否有备份管理权限
     * 仅 MANAGER 和 ADMIN 角色可以执行备份和恢复操作
     * 
     * @return true表示有权限
     */
    public boolean hasBackupPermission() {
        if (currentUser == null) {
            return false;
        }
        String role = currentUser.getRole();
        if (role == null) {
            return false;
        }
        String roleLower = role.toLowerCase();
        return "manager".equals(roleLower) || "admin".equals(roleLower);
    }
}
