package com.ceams.ui;

import com.ceams.CeamsApplication;
import com.ceams.service.ConfigService;
import com.ceams.ui.shortcut.ShortcutManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.URLEncoder;

public class CeamsUI extends Application {

    private static ConfigurableApplicationContext springContext;
    private static ShortcutManager shortcutManager;

    @Override
    public void init() {
        // 使用CeamsApplication中已经启动的Spring上下文
        springContext = CeamsApplication.getContext();
        // 初始化快捷键管理器
        shortcutManager = ShortcutManager.getInstance();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 加载现代化登录界面
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-modern.fxml"));
        // 设置Spring上下文为FXMLLoader的控制器工厂
        loader.setControllerFactory(springContext::getBean);
        Parent root = loader.load();

        Scene scene = new Scene(root);
        primaryStage.setTitle("盛韵建筑账目管理系统");

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

        // 注册全局快捷键监听器
        scene.addEventFilter(KeyEvent.KEY_PRESSED, shortcutManager.getGlobalKeyHandler());

        // 加载字号配置并应用到场景
        applyFontSize(root);

        primaryStage.show();
    }

    @Override
    public void stop() {
        // 关闭Spring上下文
        if (springContext != null) {
            springContext.close();
        }
    }

    /**
     * 应用字号配置到指定节点及其所有子节点
     */
    private void applyFontSize(Parent root) {
        try {
            // 从配置服务获取字号设置
            ConfigService configService = springContext.getBean(ConfigService.class);
            String fontSizeConfig = configService.getConfig("ui.font.size", "中 (14px)");

            // 根据配置确定实际字号值
            String fontSize;
            switch (fontSizeConfig) {
                case "小 (12px)":
                    fontSize = "12px";
                    break;
                case "大 (16px)":
                    fontSize = "16px";
                    break;
                case "超大 (18px)":
                    fontSize = "18px";
                    break;
                default:
                    fontSize = "14px"; // 默认中等字号
            }

            // 应用字号样式到根节点
            root.setStyle("-fx-font-size: " + fontSize);

            // 递归应用到所有子节点
            applyFontSizeRecursive(root, fontSize);
        } catch (Exception e) {
            System.out.println("应用字号设置失败: " + e.getMessage());
        }
    }

    /**
     * 递归应用字号到所有子节点
     */
    private void applyFontSizeRecursive(Parent parent, String fontSize) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Parent) {
                Parent childParent = (Parent) node;
                // 为所有父节点设置字体大小
                childParent.setStyle("-fx-font-size: " + fontSize);

                // 为列表相关组件添加特殊处理
                if (childParent instanceof javafx.scene.control.ListView) {
                    childParent.setStyle("-fx-font-size: " + fontSize);
                } else if (childParent instanceof javafx.scene.control.TableView) {
                    childParent.setStyle("-fx-font-size: " + fontSize);
                } else if (childParent instanceof javafx.scene.control.TreeView) {
                    childParent.setStyle("-fx-font-size: " + fontSize);
                }

                applyFontSizeRecursive(childParent, fontSize);
            } else if (node instanceof Label) {
                Label label = (Label) node;
                label.setStyle("-fx-font-size: " + fontSize);
            } else if (node instanceof Button) {
                Button button = (Button) node;
                button.setStyle("-fx-font-size: " + fontSize);
            } else if (node instanceof javafx.scene.control.ListCell) {
                javafx.scene.control.ListCell listCell = (javafx.scene.control.ListCell) node;
                listCell.setStyle("-fx-font-size: " + fontSize);
            } else if (node instanceof javafx.scene.control.TableCell) {
                javafx.scene.control.TableCell tableCell = (javafx.scene.control.TableCell) node;
                tableCell.setStyle("-fx-font-size: " + fontSize);
            } else if (node instanceof javafx.scene.control.TreeCell) {
                javafx.scene.control.TreeCell treeCell = (javafx.scene.control.TreeCell) node;
                treeCell.setStyle("-fx-font-size: " + fontSize);
            } else if (node instanceof javafx.scene.control.Cell) {
                javafx.scene.control.Cell cell = (javafx.scene.control.Cell) node;
                cell.setStyle("-fx-font-size: " + fontSize);
            } else if (node instanceof javafx.scene.control.TextField) {
                javafx.scene.control.TextField textField = (javafx.scene.control.TextField) node;
                textField.setStyle("-fx-font-size: " + fontSize);
            } else if (node instanceof javafx.scene.control.ComboBox) {
                javafx.scene.control.ComboBox comboBox = (javafx.scene.control.ComboBox) node;
                comboBox.setStyle("-fx-font-size: " + fontSize);
            } else if (node instanceof javafx.scene.control.CheckBox) {
                javafx.scene.control.CheckBox checkBox = (javafx.scene.control.CheckBox) node;
                checkBox.setStyle("-fx-font-size: " + fontSize);
            } else if (node instanceof javafx.scene.control.RadioButton) {
                javafx.scene.control.RadioButton radioButton = (javafx.scene.control.RadioButton) node;
                radioButton.setStyle("-fx-font-size: " + fontSize);
            } else if (node instanceof javafx.scene.control.TextArea) {
                javafx.scene.control.TextArea textArea = (javafx.scene.control.TextArea) node;
                textArea.setStyle("-fx-font-size: " + fontSize);
            }
        }
    }

    public static ConfigurableApplicationContext getSpringContext() {
        return springContext;
    }
}
