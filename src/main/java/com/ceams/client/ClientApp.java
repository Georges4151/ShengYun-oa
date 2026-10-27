package com.ceams.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 独立远程客户端启动类
 * 不依赖Spring Boot，通过HTTP API与后端服务器通信
 */
public class ClientApp extends Application {

    private static final String CONFIG_FILE = "client-config.properties";
    private static String serverUrl;
    private static RemoteApiClient apiClient;

    @Override
    public void init() throws Exception {
        // 加载配置文件
        loadConfig();
        
        // 初始化API客户端
        apiClient = RemoteApiClient.getInstance();
        apiClient.setBaseUrl(serverUrl);
        
        System.out.println("客户端启动，连接服务器: " + serverUrl);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 加载登录界面
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-modern.fxml"));
        
        // 手动创建控制器并注入API客户端
        loader.setControllerFactory(clazz -> {
            try {
                Object controller = clazz.getDeclaredConstructor().newInstance();
                // 如果控制器有setApiClient方法，注入API客户端
                try {
                    java.lang.reflect.Method method = clazz.getMethod("setApiClient", RemoteApiClient.class);
                    method.invoke(controller, apiClient);
                } catch (NoSuchMethodException e) {
                    // 控制器不需要API客户端，忽略
                }
                return controller;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create controller: " + clazz.getName(), e);
            }
        });
        
        Parent root = loader.load();

        Scene scene = new Scene(root);
        primaryStage.setTitle("盛韵建筑账目管理系统（远程版）");

        // 设置任务栏图标
        try {
            java.net.URL iconUrl = getClass().getResource("/images/app-icon.png");
            if (iconUrl != null) {
                javafx.scene.image.Image icon = new javafx.scene.image.Image(iconUrl.toExternalForm());
                primaryStage.getIcons().add(icon);
            } else {
                System.out.println("图标文件不存在，使用默认图标");
            }
        } catch (Exception e) {
            System.out.println("设置图标失败: " + e.getMessage());
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        System.out.println("客户端关闭");
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() throws IOException {
        Properties props = new Properties();
        
        // 尝试从classpath加载
        java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
        if (is != null) {
            props.load(is);
            is.close();
        } else {
            // 尝试从当前目录加载
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                props.load(fis);
            } catch (IOException e) {
                System.out.println("配置文件不存在，使用默认配置");
            }
        }
        
        // 读取服务器地址
        serverUrl = props.getProperty("server.url", "https://api.yourcompany.com");
    }

    /**
     * 获取API客户端实例
     */
    public static RemoteApiClient getApiClient() {
        return apiClient;
    }

    /**
     * 获取服务器地址
     */
    public static String getServerUrl() {
        return serverUrl;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
