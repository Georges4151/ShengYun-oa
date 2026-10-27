# 建筑企业账目管理系统

## 项目概述

建筑企业账目管理系统是一个基于Java开发的Windows桌面应用，专为建筑企业设计，用于管理项目、材料、分包商、账目和票据等核心业务。系统集成了OCR票据识别功能，支持报表生成和数据库备份，提供了完整的用户认证和授权机制。

## 技术栈

### 后端技术
- Java 17
- Spring Boot 3.2.2
- MyBatis-Plus 3.5.5
- MySQL 8.0+
- Spring Security

### 前端技术
- JavaFX 17.0.10

### 其他技术
- JasperReports 6.19.1 (报表生成)
- Tesseract OCR 5.8.0 (票据识别)
- Lombok (减少样板代码)
- BCrypt (密码加密)

## 功能模块

### 1. 项目管理
- 项目信息录入、编辑、删除
- 项目进度跟踪
- 项目变更管理

### 2. 材料管理
- 材料信息管理
- 材料采购记录
- 材料库存管理

### 3. 分包管理
- 分包商信息管理
- 分包合同管理
- 进度款支付管理

### 4. 账目管理
- 收支记录管理
- 账目分类管理
- 账目统计分析

### 5. 票据管理
- 票据上传和存储
- OCR票据识别
- 票据核查和验证

### 6. 报表生成
- 财务报表
- 项目成本报表
- 材料使用报表

### 7. 系统管理
- 用户管理
- 权限管理
- 操作日志
- 数据库备份和恢复

## 安装步骤

### 1. 环境准备
- JDK 17 或更高版本
- MySQL 8.0 或更高版本
- Maven 3.6 或更高版本

### 2. 数据库配置
1. 创建数据库：
   ```sql
   CREATE DATABASE ceams DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. 创建数据库用户：
   ```sql
   CREATE USER 'ceams_user'@'localhost' IDENTIFIED BY 'ceams_password';
   GRANT ALL PRIVILEGES ON ceams.* TO 'ceams_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

3. 执行数据库脚本：
   ```sql
   SOURCE database.sql;
   ```

### 3. 项目配置
1. 修改 `src/main/resources/application.properties` 文件，配置数据库连接：
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/ceams?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
   spring.datasource.username=ceams_user
   spring.datasource.password=ceams_password
   ```

2. 配置其他参数（可选）：
   ```properties
   # 备份配置
   ceams.backup.path=backup
   ceams.backup.keep-days=30
   
   # OCR配置
   ceams.ocr.data-path=tessdata
   ceams.ocr.language=chi_sim
   ```

### 4. 项目构建
```bash
mvn clean package
```

## 使用说明

### 1. 启动应用

#### 方式一：使用Maven运行
```bash
mvn javafx:run
```

#### 方式二：使用JAR文件运行
```bash
java -jar target/ceams-1.0-SNAPSHOT.jar
```

### 2. 登录系统
- 默认用户名：admin
- 默认密码：admin123

### 3. 主要功能使用

#### 项目管理
1. 点击左侧菜单的"项目管理"
2. 点击"新增"按钮添加新项目
3. 填写项目信息，点击"保存"
4. 可以在列表中编辑或删除项目

#### 材料管理
1. 点击左侧菜单的"材料管理"
2. 点击"新增"按钮添加新材料
3. 填写材料信息，点击"保存"
4. 可以在列表中编辑或删除材料

#### 分包管理
1. 点击左侧菜单的"分包管理"
2. 点击"新增"按钮添加新分包商
3. 填写分包商信息，点击"保存"
4. 可以在列表中编辑或删除分包商

#### 账目管理
1. 点击左侧菜单的"账目管理"
2. 点击"新增"按钮添加新账目记录
3. 填写账目信息，点击"保存"
4. 可以在列表中查看、编辑或删除账目记录

#### 票据管理
1. 点击左侧菜单的"票据管理"
2. 点击"上传"按钮选择票据图片
3. 系统会自动识别票据信息
4. 核对识别结果，点击"保存"

#### 报表生成
1. 点击左侧菜单的"报表管理"
2. 选择报表类型
3. 设置报表参数
4. 点击"生成"按钮生成报表

#### 系统管理
1. 点击左侧菜单的"系统管理"
2. 可以进行用户管理、权限管理、日志查看等操作
3. 点击"备份"按钮可以备份数据库

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── ceams/
│   │           ├── controller/      # Web控制器
│   │           ├── dao/             # 数据访问层
│   │           ├── model/           # 数据模型
│   │           │   └── entity/      # 实体类
│   │           ├── security/        # 安全模块
│   │           ├── service/         # 业务逻辑层
│   │           ├── ui/              # JavaFX界面
│   │           │   └── controller/  # JavaFX控制器
│   │           └── CeamsApplication.java  # Spring Boot启动类
│   └── resources/                   # 资源文件
│       ├── fxml/                    # JavaFX FXML文件
│       ├── mapper/                  # MyBatis映射文件
│       └── application.properties   # 配置文件
└── test/                            # 测试代码
```

## 配置文件说明

### application.properties

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| spring.datasource.url | 数据库连接URL | jdbc:mysql://localhost:3306/ceams |
| spring.datasource.username | 数据库用户名 | ceams_user |
| spring.datasource.password | 数据库密码 | ceams_password |
| mybatis-plus.mapper-locations | MyBatis映射文件路径 | classpath:mapper/*.xml |
| spring.security.user.name | 默认管理员用户名 | admin |
| spring.security.user.password | 默认管理员密码 | admin123 |
| ceams.backup.path | 备份文件存储路径 | backup |
| ceams.backup.keep-days | 备份文件保留天数 | 30 |
| ceams.ocr.data-path | OCR数据文件路径 | tessdata |
| ceams.ocr.language | OCR识别语言 | chi_sim |
| ceams.receipt.path | 票据存储路径 | receipts |

## 开发说明

### 代码规范
- 遵循Java编码规范
- 使用Lombok减少样板代码
- 服务层接口和实现分离
- 控制器使用RESTful风格

### 数据库设计
- 采用三范式设计
- 表名使用小写字母，单词之间用下划线分隔
- 主键使用自增ID
- 外键约束确保数据完整性

### 安全设计
- 密码使用BCrypt加密存储
- 所有敏感操作记录日志
- 基于角色的访问控制

## 部署说明

### 开发环境部署
1. 克隆代码到本地
2. 配置数据库
3. 使用IDE运行CeamsApplication.java

### 生产环境部署
1. 构建JAR文件：`mvn clean package`
2. 将JAR文件复制到服务器
3. 创建启动脚本：
   ```bash
   #!/bin/bash
   java -jar ceams-1.0-SNAPSHOT.jar
   ```
4. 配置系统服务（可选）
5. 启动应用

## 注意事项

1. 首次运行前请确保已正确配置数据库连接
2. OCR功能需要下载对应的语言数据包
3. 建议定期备份数据库
4. 生产环境中请修改默认管理员密码
5. 确保服务器有足够的磁盘空间存储票据和备份文件

## 更新日志

### 版本 1.0.0 (2026-01-22)
- 初始版本发布
- 实现所有核心功能模块
- 支持OCR票据识别
- 支持报表生成
- 支持数据库备份和恢复

## 许可证

MIT License

## 联系方式

- 开发团队：建筑企业管理系统开发组
- 联系邮箱：support@ceams.com
- 技术支持：010-12345678

## 致谢

感谢所有为项目做出贡献的开发者和测试人员！
