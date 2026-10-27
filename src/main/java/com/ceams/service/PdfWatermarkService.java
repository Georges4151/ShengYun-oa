package com.ceams.service;

import java.io.File;
import java.io.IOException;

/**
 * PDF水印服务接口
 * 
 * 用于在服务端为PDF文件添加安全水印，
 * 支持审批过程中的文件预览保护。
 */
public interface PdfWatermarkService {

    /**
     * 为PDF文件添加水印并返回字节数组（用于预览响应）
     *
     * @param originalFilePath 原始PDF文件路径
     * @param username         当前查看用户名
     * @return 添加水印后的PDF字节数组
     * @throws IOException 文件读写异常
     */
    byte[] addWatermarkToPdf(String originalFilePath, String username) throws IOException;

    /**
     * 为PDF文件添加水印并保存到新文件
     *
     * @param originalFilePath 原始PDF文件路径
     * @param outputFilePath   输出文件路径
     * @param username         当前查看用户名
     * @return 水印处理后的文件
     * @throws IOException 文件读写异常
     */
    File addWatermarkToPdfFile(String originalFilePath, String outputFilePath, String username) throws IOException;

    /**
     * 清理过期的临时水印文件
     */
    void cleanupExpiredWatermarks();
}
