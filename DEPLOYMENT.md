# 部署和上线指南

## 1. 后端服务部署

### 1.1 环境要求
- Java 17或更高版本
- Redis 6.0或更高版本
- MySQL 8.0或更高版本

### 1.2 配置修改
1. 修改`src/main/resources/application.properties`文件中的数据库连接信息：
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/ceams?useSSL=false&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

2. 修改Redis连接信息：
   ```properties
   spring.redis.host=localhost
   spring.redis.port=6379
   spring.redis.password=
   spring.redis.database=0
   ```

3. 修改邮件发送配置：
   ```properties
   spring.mail.host=smtp.qq.com
   spring.mail.port=587
   spring.mail.username=your_email@qq.com
   spring.mail.password=your_email_password
   spring.mail.properties.mail.smtp.auth=true
   spring.mail.properties.mail.smtp.starttls.enable=true
   spring.mail.properties.mail.smtp.starttls.required=true
   ```

### 1.3 启动服务
1. 构建项目：
   ```bash
   mvn clean package -DskipTests
   ```

2. 启动服务：
   ```bash
   java -jar target/ceams-1.0-SNAPSHOT.jar
   ```

3. 服务默认运行在`http://localhost:9095`

## 2. Android客户端部署

### 2.1 环境要求
- Android Studio
- Android 8.0或更高版本

### 2.2 配置修改
1. 修改`android-client/app/src/main/java/com/ceams/client/RetrofitClient.java`文件中的Base URL：
   ```java
   private static final String BASE_URL = "http://your_server_ip:9095";
   ```

### 2.3 构建和安装
1. 使用Android Studio打开`android-client`项目
2. 构建APK：
   - 点击`Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`
3. 安装APK到设备：
   - 找到生成的APK文件（通常在`android-client/app/build/outputs/apk/debug/`目录）
   - 将APK文件复制到Android设备并安装

## 3. 系统使用

### 3.1 登录
- 打开Android客户端
- 输入用户名和密码
- 点击登录按钮

### 3.2 审批操作
1. 在主界面点击"待审批列表"
2. 选择需要审批的票据
3. 查看票据详情和审批历史
4. 输入审批意见
5. 点击"批准"或"拒绝"按钮

### 3.3 通知系统
- 系统会自动发送邮件通知给相关人员
- 待审批通知：当有新的票据需要审批时
- 审批状态通知：当票据审批状态变更时

## 4. 安全注意事项

1. **数据安全**：
   - 所有API请求都需要JWT令牌验证
   - 敏感数据使用HTTPS传输
   - 密码使用BCrypt加密存储

2. **网络安全**：
   - 企业内部部署，建议使用VPN或内网访问
   - 配置防火墙，只允许特定IP访问

3. **权限管理**：
   - 严格的角色权限控制
   - 基于JWT的认证机制

## 5. 故障排除

### 5.1 后端服务启动失败
- 检查数据库连接是否正常
- 检查Redis服务是否运行
- 查看日志文件获取详细错误信息

### 5.2 Android客户端无法连接服务器
- 检查网络连接是否正常
- 检查服务器IP和端口是否正确
- 检查服务器是否运行

### 5.3 审批操作失败
- 检查用户权限是否足够
- 检查网络连接是否稳定
- 查看服务器日志获取详细错误信息

## 6. 维护和更新

1. **定期备份**：
   - 定期备份数据库
   - 定期备份配置文件

2. **更新系统**：
   - 后端服务：重新构建并部署
   - Android客户端：发布新的APK版本

3. **监控系统**：
   - 监控服务器运行状态
   - 监控API调用情况
   - 监控数据库性能

---

系统部署完成后，项目经理和经理可以通过Android客户端远程进行票据审批操作，提高审批效率，确保企业内部数据安全。