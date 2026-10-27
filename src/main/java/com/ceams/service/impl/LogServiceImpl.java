package com.ceams.service.impl;

import com.ceams.model.entity.OperationLog;
import com.ceams.service.LogService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LogServiceImpl implements LogService {
    
    @Override
    public void saveLog(OperationLog log) {
        // 这里可以实现日志持久化，比如保存到数据库或文件
        // 目前先打印到控制台
        System.out.println("[LOG] " + log.getCreatedAt() + " - " + log.getOperationDescription());
    }
    
    @Override
    public void saveBackupLog(Integer userId, String backupFileName, boolean success, String message) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setOperationType("BACKUP");
        log.setOperationDescription("备份操作: " + (success ? "成功" : "失败") + " - " + backupFileName + " - " + message);
        log.setTargetType("BACKUP_FILE");
        log.setCreatedAt(new Date());
        
        saveLog(log);
    }
}