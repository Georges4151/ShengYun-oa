package com.ceams.service.impl;

import com.ceams.service.ConfigService;
import com.ceams.service.WebcamManager;
import com.github.sarxos.webcam.Webcam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultWebcamManager implements WebcamManager {
    
    private static final String DEFAULT_WEBCAM_KEY = "camera.default";
    
    @Autowired
    private ConfigService configService;
    
    @Override
    public List<String> getAvailableWebcams() {
        List<String> webcamNames = new ArrayList<>();
        List<Webcam> webcams = Webcam.getWebcams();
        for (Webcam webcam : webcams) {
            webcamNames.add(webcam.getName());
        }
        return webcamNames;
    }
    
    @Override
    public String getDefaultWebcam() {
        return configService.getConfig(DEFAULT_WEBCAM_KEY, "");
    }
    
    @Override
    public void setDefaultWebcam(String webcamName) {
        configService.setConfig(DEFAULT_WEBCAM_KEY, webcamName);
        configService.saveConfigs();
    }
    
    @Override
    public boolean testWebcam(String webcamName) {
        Webcam webcam = getWebcamByName(webcamName);
        if (webcam == null) {
            return false;
        }
        
        try {
            webcam.open();
            webcam.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public Object openWebcam(String webcamName) {
        Webcam webcam = getWebcamByName(webcamName);
        if (webcam == null) {
            return null;
        }
        
        try {
            webcam.open();
            return webcam;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public void closeWebcam(Object webcam) {
        if (webcam instanceof Webcam) {
            Webcam webCam = (Webcam) webcam;
            if (webCam.isOpen()) {
                webCam.close();
            }
        }
    }
    
    /**
     * 根据名称获取Webcam对象
     * @param webcamName 摄像头名称
     * @return Webcam对象，未找到返回null
     */
    private Webcam getWebcamByName(String webcamName) {
        List<Webcam> webcams = Webcam.getWebcams();
        for (Webcam webcam : webcams) {
            if (webcam.getName().equals(webcamName)) {
                return webcam;
            }
        }
        return null;
    }
}