package com.ceams.service;

import java.util.Map;

public interface ConfigService {
    /**
     * 获取所有配置
     * @return 配置映射
     */
    Map<String, String> getAllConfigs();
    
    /**
     * 获取单个配置值
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getConfig(String key, String defaultValue);
    
    /**
     * 获取整数类型配置
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 整数配置值
     */
    int getIntConfig(String key, int defaultValue);
    
    /**
     * 获取布尔类型配置
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 布尔配置值
     */
    boolean getBooleanConfig(String key, boolean defaultValue);
    
    /**
     * 设置配置值
     * @param key 配置键
     * @param value 配置值
     */
    void setConfig(String key, String value);
    
    /**
     * 保存所有配置
     */
    void saveConfigs();
    
    /**
     * 重新加载配置
     */
    void reloadConfigs();
}