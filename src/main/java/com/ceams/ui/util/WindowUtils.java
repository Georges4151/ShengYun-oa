package com.ceams.ui.util;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.net.URL;

public class WindowUtils {
    
    private static Image appIcon;
    
    static {
        try {
            URL iconUrl = WindowUtils.class.getResource("/images/app-icon.png");
            if (iconUrl != null) {
                appIcon = new Image(iconUrl.toExternalForm());
                System.out.println("应用图标加载成功");
            } else {
                System.err.println("图标文件不存在: /images/app-icon.png");
            }
        } catch (Exception e) {
            System.err.println("加载应用图标失败: " + e.getMessage());
        }
    }
    
    public static void setStageIcon(Stage stage) {
        if (appIcon != null) {
            stage.getIcons().add(appIcon);
        }
    }
    
    public static Image getAppIcon() {
        return appIcon;
    }
}
