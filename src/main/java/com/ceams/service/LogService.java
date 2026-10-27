package com.ceams.service;

import com.ceams.model.entity.OperationLog;

public interface LogService {
    /**
     * 记录操作日志
     * @param log 操作日志对象
     */
    void saveLog(OperationLog log);
    
    /**
     * 记录备份日志
     * @param userId 用户ID
     * @param backupFileName 备份文件名
     * @param success 是否成功
     * @param message 日志消息
     */
    void saveBackupLog(Integer userId, String backupFileName, boolean success, String message);
}