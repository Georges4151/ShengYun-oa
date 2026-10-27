package com.ceams.client;

import com.ceams.model.entity.User;

/**
 * 客户端会话管理类
 * 用于存储当前登录用户信息
 * 
 * @author Soko Coder
 * @version 1.0
 */
public class ClientSession {
    
    /**
     * 单例实例
     */
    private static volatile ClientSession instance;
    
    /**
     * 当前登录用户
     */
    private User currentUser;
    
    /**
     * JWT令牌
     */
    private String jwtToken;
    
    /**
     * 私有构造函数
     */
    private ClientSession() {
    }
    
    /**
     * 获取单例实例
     * 
     * @return ClientSession实例
     */
    public static ClientSession getInstance() {
        if (instance == null) {
            synchronized (ClientSession.class) {
                if (instance == null) {
                    instance = new ClientSession();
                }
            }
        }
        return instance;
    }
    
    /**
     * 设置当前用户
     * 
     * @param user 用户对象
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    /**
     * 获取当前用户
     * 
     * @return 当前用户对象
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * 设置JWT令牌
     * 
     * @param token JWT令牌
     */
    public void setJwtToken(String token) {
        this.jwtToken = token;
    }
    
    /**
     * 获取JWT令牌
     * 
     * @return JWT令牌
     */
    public String getJwtToken() {
        return jwtToken;
    }
    
    /**
     * 清除会话信息
     */
    public void clear() {
        this.currentUser = null;
        this.jwtToken = null;
    }
    
    /**
     * 检查是否已登录
     * 
     * @return true表示已登录，false表示未登录
     */
    public boolean isLoggedIn() {
        return currentUser != null && jwtToken != null && !jwtToken.isEmpty();
    }
}
