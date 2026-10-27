package com.ceams.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.util.ResourceBundle;

public class FXAboutController implements Initializable {

    @FXML
    private Label appNameLabel;

    @FXML
    private Label appVersionLabel;

    @FXML
    private Label appDescriptionLabel;

    @FXML
    private TextArea techStackTextArea;

    @FXML
    private TextArea featuresTextArea;

    @FXML
    private Label contactLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 直接使用硬编码的中文，避免编码问题
        String appName = "盛韵建筑账目管理系统";
        String appVersion = "1.0.1";
        String appDescription = "建筑企业设计的账目管理系统";

        // 设置应用信息
        appNameLabel.setText("系统名称: " + appName);
        appVersionLabel.setText("版本号: " + appVersion);
        appDescriptionLabel.setText("描述: " + appDescription);

        // 设置技术栈信息
        String techStack = "后端技术:\n" +
                "- Java 17\n" +
                "- Spring Boot 3.2.2\n" +
                "- MyBatis 3.0.3\n" +
                "- MySQL 8.0+\n" +
                "- Spring Security\n\n" +
                "前端技术:\n" +
                "- JavaFX 17.0.10\n\n" +
                "其他技术:\n" +
                "- JasperReports 6.19.1 (报表生成)\n" +
                "- Tesseract OCR 5.8.0 (票据识别)\n" +
                "- Lombok (减少样板代码)";
        techStackTextArea.setText(techStack);

        // 设置功能模块信息
        String features = "功能模块:\n" +
                "1. 项目管理 - 项目信息录入、编辑、删除，进度跟踪\n" +
                "2. 材料管理 - 材料信息管理，采购记录，库存管理\n" +
                "3. 分包管理 - 分包商信息管理，合同管理，进度款支付\n" +
                "4. 账目管理 - 收支记录管理，分类管理，统计分析\n" +
                "5. 票据管理 - 票据上传和存储，OCR识别，核查验证\n" +
                "6. 报表生成 - 财务报表，项目成本报表，材料使用报表\n" +
                "7. 系统管理 - 用户管理，权限管理，数据库备份";
        featuresTextArea.setText(features);

        // 设置联系方式
        String contact = "开发团队: 张贺然\n" +
                "联系邮箱: 13283252332@163.com\n" +
                "技术支持: 13283252332";
        contactLabel.setText(contact);
    }
}