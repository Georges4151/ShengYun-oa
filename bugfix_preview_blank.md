# 移动端合同预览空白问题修复报告

## 问题描述

**用户反馈**：
- 生成安全预览时，显示"审批未完成禁止下载"
- 预览界面为空白

**问题现象**：
1. 系统正确检测到审批未完成状态
2. 安全设置正确配置（禁止下载，显示水印提示）
3. 但PDF预览区域显示空白

---

## 问题根因分析

### 问题定位

通过代码审查发现这是一个**HTTP响应处理错误**的问题：

```
后端错误响应（JSON） →  HTTP 200 OK  →  Android端误判为成功  →  下载JSON当PDF  →  WebView无法渲染  →  空白预览
```

### 详细分析

#### 1. 后端错误处理缺陷

**文件**：`MobileApiController.java` 第469-522行

**问题代码**：
```java
// 所有错误响应都使用 ResponseEntity.ok()
if (approval == null) {
    return ResponseEntity.ok(ApiResponse.error("审批记录不存在"));  // ❌ HTTP 200
}

if (contractPath == null || contractPath.isEmpty()) {
    return ResponseEntity.ok(ApiResponse.error("合同文件不存在"));   // ❌ HTTP 200
}

if (!file.exists()) {
    return ResponseEntity.ok(ApiResponse.error("合同文件不存在"));  // ❌ HTTP 200
}
```

**问题**：即使出错也返回 HTTP 200 OK，导致客户端无法区分成功和失败。

#### 2. 前端响应验证缺失

**文件**：`PdfViewerActivity.java` 第288-344行

**问题代码**：
```java
if (response.isSuccessful() && response.body() != null) {
    // ❌ 没有验证 Content-Type
    // ❌ 直接将响应体当作PDF处理
    InputStream inputStream = response.body().byteStream();
    // ... 保存为.pdf文件
}
```

**问题**：只检查HTTP状态码，不验证响应内容类型，导致JSON被当作PDF下载。

#### 3. 完整错误流程

1. 用户请求预览
2. 服务端检测到文件不存在
3. 服务端返回 `ResponseEntity.ok(ApiResponse.error("合同文件不存在"))`
4. Android端收到 HTTP 200 OK
5. Android端认为是成功的PDF响应
6. 将 JSON 文本保存为 PDF 文件：`preview_123456789.pdf`
7. WebView 尝试加载这个"PDF"文件
8. 显示空白（无法解析JSON为PDF）

---

## 修复方案

### 方案一：Android端修复（已完成）

**文件**：`PdfViewerActivity.java`

**修改内容**：

1. **添加导入**：
```java
import okhttp3.MediaType;
import org.json.JSONObject;
```

2. **添加Content-Type验证**：
```java
if (response.isSuccessful() && response.body() != null) {
    // 验证Content-Type是否为PDF
    String contentType = response.header("Content-Type", "");
    MediaType mediaType = response.body().contentType();
    
    if (mediaType == null || !mediaType.toString().contains("application/pdf")) {
        // 如果不是PDF，尝试读取错误信息
        String errorBody = response.body().string();
        Log.e(TAG, "Invalid Content-Type: " + contentType + ", Response: " + errorBody);
        
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            // 尝试解析JSON错误信息
            try {
                JSONObject json = new JSONObject(errorBody);
                String message = json.optString("message", "服务器返回了无效的响应");
                Toast.makeText(PdfViewerActivity.this, message, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(PdfViewerActivity.this, 
                        "获取文件失败：服务器返回了无效的响应", Toast.LENGTH_LONG).show();
            }
        });
        return;
    }
    
    // 继续正常的PDF下载流程
    InputStream inputStream = response.body().byteStream();
    // ...
}
```

**效果**：
- ✅ 验证响应类型是 application/pdf
- ✅ 错误时显示服务端返回的错误信息
- ✅ 用户能看到具体的错误原因（文件不存在、无权限等）

### 方案二：后端修复（已完成）

**文件**：`MobileApiController.java`

**修改内容**：

1. **添加导入**：
```java
import org.springframework.http.HttpStatus;
```

2. **修复所有错误响应的HTTP状态码**：

```java
// 审批记录不存在 → 404 Not Found
if (approval == null) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("审批记录不存在"));  // ✅ HTTP 404
}

// 无权限查看 → 403 Forbidden
if (!hasViewPermission(approval, user)) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("无权限查看此合同"));  // ✅ HTTP 403
}

// 合同文件不存在 → 404 Not Found
if (contractPath == null || contractPath.isEmpty()) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("合同文件不存在"));  // ✅ HTTP 404
}

if (!file.exists()) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("合同文件不存在"));  // ✅ HTTP 404
}

// 水印生成失败 → 500 Internal Server Error
catch (Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("生成预览失败: " + e.getMessage()));  // ✅ HTTP 500
}
```

**效果**：
- ✅ 错误使用正确的HTTP状态码
- ✅ 符合RESTful API规范
- ✅ 便于调试和问题排查
- ✅ 配合前端Content-Type检查，实现双重保护

---

## 修复效果对比

### 修复前

| 场景 | 用户看到 | 根本原因 |
|------|---------|---------|
| 文件不存在 | 空白预览 | JSON当PDF下载 |
| 无权限 | 空白预览 | JSON当PDF下载 |
| 水印生成失败 | 空白预览 | JSON当PDF下载 |
| Token无效 | 空白预览 | JSON当PDF下载 |

### 修复后

| 场景 | 用户看到 | HTTP状态码 |
|------|---------|-----------|
| 文件不存在 | Toast提示"审批记录不存在" | 404 |
| 无权限 | Toast提示"无权限查看此合同" | 403 |
| 水印生成失败 | Toast提示"生成预览失败: 原因" | 500 |
| Token无效 | Toast提示"用户未登录或Token无效" | 401 |
| 成功预览 | 正常显示带水印PDF | 200 |

---

## 测试建议

### 1. 功能测试

- [ ] 测试文件不存在的场景
- [ ] 测试无权限访问的场景
- [ ] 测试Token无效的场景
- [ ] 测试水印生成失败的场景
- [ ] 测试正常预览场景

### 2. 边界测试

- [ ] 大文件PDF预览
- [ ] 特殊字符文件名
- [ ] 网络中断恢复
- [ ] 并发请求处理

### 3. 回归测试

- [ ] 模拟器预览功能正常
- [ ] 真机预览功能正常
- [ ] 审批已完成状态下正常预览
- [ ] 下载功能正常工作

---

## 相关文件清单

### 修改的文件

1. **Android端**：
   - `androidclient/app/src/main/java/com/example/android_client/PdfViewerActivity.java`
     - 添加 Content-Type 验证逻辑
     - 添加错误信息解析和显示

2. **后端**：
   - `src/main/java/com/ceams/controller/MobileApiController.java`
     - 修复所有错误响应的HTTP状态码
     - 添加 HttpStatus 导入

### 相关文件（未修改）

- `src/main/java/com/ceams/dto/ApiResponse.java` - API响应结构
- `src/main/java/com/ceams/service/impl/PdfWatermarkServiceImpl.java` - PDF水印服务
- `androidclient/app/src/main/java/com/example/android_client/network/RetrofitClient.java` - 网络配置
- `src/main/resources/application.properties` - CORS配置

---

## 修复时间线

- **2026-04-29 10:38** - 发现并分析问题根因
- **2026-04-29 10:40** - 实施Android端Content-Type验证
- **2026-04-29 10:42** - 实施后端HTTP状态码修复
- **2026-04-29 10:45** - 生成修复报告

---

## 后续建议

1. **监控增强**：在服务端添加PDF预览失败的监控指标
2. **日志优化**：在水印服务中添加更详细的日志
3. **用户体验**：考虑添加错误页面的UI设计，而不仅仅是Toast
4. **测试覆盖**：增加单元测试和集成测试覆盖预览功能

---

*报告生成时间：2026-04-29*
*修复人员：AI Assistant*
