package com.example.android_client.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.android_client.model.LoginResponse;
import com.example.android_client.model.User;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Token管理器
 * 使用EncryptedSharedPreferences安全存储JWT令牌和用户信息
 */
public class TokenManager {

    private static final String PREF_FILE_NAME = "secure_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_TOKEN_TYPE = "token_type";
    private static final String KEY_EXPIRES_IN = "expires_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_ROLE = "role";
    private static final String KEY_EMAIL = "email";

    private static TokenManager instance;
    private final EncryptedSharedPreferences encryptedPrefs;

    private TokenManager(Context context) throws GeneralSecurityException, IOException {
        // 创建MasterKey用于加密
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        // 创建加密的SharedPreferences
        encryptedPrefs = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                context,
                PREF_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    /**
     * 获取TokenManager单例实例
     */
    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            try {
                instance = new TokenManager(context.getApplicationContext());
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize TokenManager", e);
            }
        }
        return instance;
    }

    /**
     * 保存登录响应数据
     */
    public void saveLoginResponse(LoginResponse response) {
        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.putString(KEY_TOKEN, response.getToken());
        editor.putString(KEY_TOKEN_TYPE, "Bearer");

        // 保存用户信息
        if (response.getUserId() != null) {
            editor.putLong(KEY_USER_ID, response.getUserId());
        }
        if (response.getUsername() != null) {
            editor.putString(KEY_USERNAME, response.getUsername());
        }
        if (response.getName() != null) {
            editor.putString(KEY_FULL_NAME, response.getName());
        }
        if (response.getRole() != null) {
            editor.putString(KEY_ROLE, response.getRole());
        }

        editor.apply();
    }

    /**
     * 获取存储的令牌
     */
    public String getToken() {
        return encryptedPrefs.getString(KEY_TOKEN, null);
    }

    /**
     * 获取令牌类型
     */
    public String getTokenType() {
        return encryptedPrefs.getString(KEY_TOKEN_TYPE, "Bearer");
    }

    /**
     * 获取完整的Authorization头值
     */
    public String getAuthorizationHeader() {
        String token = getToken();
        if (token == null) {
            return null;
        }
        return getTokenType() + " " + token;
    }

    /**
     * 获取当前用户ID
     */
    public Long getUserId() {
        long userId = encryptedPrefs.getLong(KEY_USER_ID, -1);
        return userId;
    }

    /**
     * 获取当前用户名
     */
    public String getUsername() {
        return encryptedPrefs.getString(KEY_USERNAME, null);
    }

    /**
     * 获取当前用户全名
     */
    public String getFullName() {
        return encryptedPrefs.getString(KEY_FULL_NAME, null);
    }

    /**
     * 获取当前用户角色
     */
    public String getRole() {
        return encryptedPrefs.getString(KEY_ROLE, null);
    }

    /**
     * 获取当前用户邮箱
     */
    public String getEmail() {
        return encryptedPrefs.getString(KEY_EMAIL, null);
    }

    /**
     * 检查用户是否已登录
     */
    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    /**
     * 检查用户是否有审批权限
     */
    public boolean hasApprovalPermission() {
        String role = getRole();
        if (role == null) {
            return false;
        }
        String upperRole = role.toUpperCase();
        // 支持新的角色常量
        return "MANAGER".equals(upperRole) ||
                "PROJECT_MANAGER".equals(upperRole) ||
                "ADMIN".equals(upperRole) ||
                "FINANCE".equals(upperRole);
    }

    /**
     * 清除所有存储的数据（退出登录）
     */
    public void clear() {
        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * 清除Token（用于Token过期场景）
     */
    public void clearToken() {
        clear();
    }

    /**
     * 获取当前用户信息
     */
    public User getCurrentUser() {
        if (!isLoggedIn()) {
            return null;
        }

        User user = new User();
        user.setId(getUserId());
        user.setUsername(getUsername());
        user.setFullName(getFullName());
        user.setRole(getRole());
        user.setEmail(getEmail());
        return user;
    }
}
