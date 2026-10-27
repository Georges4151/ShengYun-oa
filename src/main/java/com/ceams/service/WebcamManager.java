package com.ceams.service;

import java.util.List;

public interface WebcamManager {
    /**
     * 获取所有可用摄像头名称列表
     * @return 摄像头名称列表
     */
    List<String> getAvailableWebcams();
    
    /**
     * 获取默认摄像头名称
     * @return 默认摄像头名称，未设置则返回空字符串
     */
    String getDefaultWebcam();
    
    /**
     * 设置默认摄像头
     * @param webcamName 摄像头名称
     */
    void setDefaultWebcam(String webcamName);
    
    /**
     * 测试摄像头连接
     * @param webcamName 摄像头名称
     * @return 连接成功返回true，否则返回false
     */
    boolean testWebcam(String webcamName);
    
    /**
     * 打开指定摄像头
     * @param webcamName 摄像头名称
     * @return 摄像头对象，打开失败返回null
     */
    Object openWebcam(String webcamName);
    
    /**
     * 关闭指定摄像头
     * @param webcam 摄像头对象
     */
    void closeWebcam(Object webcam);
}