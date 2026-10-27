package com.ceams.service.impl;

import com.ceams.service.ConfigService;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Service
public class ConfigServiceImpl implements ConfigService {
    
    private static final String CONFIG_FILE_PATH = "src/main/resources/system-settings.properties";
    private final Map<String, String> configs = new HashMap<>();
    
    public ConfigServiceImpl() {
        reloadConfigs();
    }
    
    @Override
    public Map<String, String> getAllConfigs() {
        return new HashMap<>(configs);
    }
    
    @Override
    public String getConfig(String key, String defaultValue) {
        return configs.getOrDefault(key, defaultValue);
    }
    
    @Override
    public int getIntConfig(String key, int defaultValue) {
        String value = configs.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    @Override
    public boolean getBooleanConfig(String key, boolean defaultValue) {
        String value = configs.get(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
    
    @Override
    public void setConfig(String key, String value) {
        configs.put(key, value);
    }
    
    @Override
    public void saveConfigs() {
        Properties properties = new Properties();
        properties.putAll(configs);
        
        try (OutputStream output = new FileOutputStream(CONFIG_FILE_PATH)) {
            properties.store(output, "System Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void reloadConfigs() {
        Properties properties = new Properties();
        
        // 创建默认配置文件如果不存在
        File configFile = new File(CONFIG_FILE_PATH);
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                // 设置默认配置
                setDefaultConfigs();
                saveConfigs();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        try (InputStream input = new FileInputStream(CONFIG_FILE_PATH)) {
            properties.load(input);
            configs.clear();
            properties.forEach((key, value) -> {
                configs.put((String) key, (String) value);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void setDefaultConfigs() {
        // 基础设置
        configs.put("ui.theme", "light");
        configs.put("ui.language", "zh_CN");
        
        // 通知设置
        configs.put("notification.enabled", "true");
        configs.put("notification.sound", "true");
        
        // 摄像头设置
        configs.put("camera.default", "");
        
        // 备份设置 - 按天为单位
        configs.put("backup.auto.enabled", "false");
        configs.put("backup.interval.days", "7"); // 按天为单位
        configs.put("backup.path", "backup/");
        configs.put("backup.keep.days", "30"); // 按天为单位
        
        // 分页设置
        configs.put("pagination.page.size", "10"); // 每页默认条数
    }
}