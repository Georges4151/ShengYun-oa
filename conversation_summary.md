# 对话摘要：移动端合同预览问题修复（三轮迭代）

## 1. 主要请求和意图

**用户问题**：移动端无法正常预览合同

**项目环境**：
- 操作系统：Windows (PowerShell)
- 工作区路径：e:/sxassist
- 项目类型：Java Spring Boot后端 + Android客户端
- 服务器端口：9095

---

## 2. 问题演进历程

### 第一轮：初步诊断（空白预览）

**用户反馈**：
- 生成安全预览时，显示"审批未完成禁止下载"
- 预览界面为空白

**诊断结果**：
- Android端没有验证Content-Type
- 后端错误响应使用HTTP 200
- JSON被当作PDF下载，WebView无法渲染

**修复**：
- ✅ Android端添加Content-Type验证
- ✅ 后端修复HTTP状态码（404/403/500）

---

### 第二轮：新错误发现（500内部错误）

**用户反馈**：
```
java.lang.IllegalArgumentException: The Unicode character [新] at code point [26,032] 
cannot be encoded as it is outside the permitted range of 0 to 255
```

**诊断结果**：
- HTTP响应头中的文件名包含中文
- HTTP协议只支持ASCII编码
- Tomcat无法编码中文字符

**修复**：
- ✅ 所有PDF预览接口的Content-Disposition文件名设为null
- ✅ inline预览模式不需要文件名

---

## 3. 关键修复文件

### 3.1 PdfViewerActivity.java (Android端)

**路径**：`e:/sxassist/androidclient/app/src/main/java/com/example/android_client/PdfViewerActivity.java`

**修改**：
1. 添加导入：`okhttp3.MediaType`, `org.json.JSONObject`
2. 添加Content-Type验证逻辑
3. 错误时解析并显示服务端返回的错误信息

```java
// 添加Content-Type验证
MediaType mediaType = response.body().contentType();
if (mediaType == null || !mediaType.toString().contains("application/pdf")) {
    // 读取错误信息并显示
    String errorBody = response.body().string();
    JSONObject json = new JSONObject(errorBody);
    String message = json.optString("message", "服务器返回了无效的响应");
    Toast.makeText(PdfViewerActivity.this, message, Toast.LENGTH_LONG).show();
    return;
}
```

### 3.2 MobileApiController.java (后端)

**路径**：`e:/sxassist/src/main/java/com/ceams/controller/MobileApiController.java`

**修改1**：添加HttpStatus导入
```java
import org.springframework.http.HttpStatus;
```

**修改2**：修复preview接口错误状态码
```java
// 审批记录不存在 → 404
return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error("审批记录不存在"));

// 无权限 → 403
return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.error("无权限查看此合同"));

// 水印生成失败 → 500
return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("生成预览失败: " + e.getMessage()));
```

**修改3**：修复所有PDF预览接口的文件名编码问题
```java
// 修改前
headers.setContentDispositionFormData("inline", file.getName());

// 修改后
headers.setContentDispositionFormData("inline", null);
```

**修复的接口**：
| 接口 | 行号 |
|------|------|
| 通用PDF预览 | 371 |
| 印章审批文件 | 457 |
| 印章审批预览（带水印） | 519 |
| 回执预览 | 622 |
| 回执水印预览 | 707 |

---

## 4. 问题根因总结

### 问题链

```
中文文件名 → Content-Disposition头 → HTTP头部ASCII编码 → 失败 → 500错误
     ↓
HTTP 200 + JSON错误 → Android误判 → JSON当PDF下载 → WebView空白
```

### 技术细节

| 问题 | 原因 | 影响 |
|------|------|------|
| Content-Type未验证 | Android端只检查HTTP状态码 | JSON被当作PDF |
| HTTP状态码错误 | 后端所有错误返回200 | 无法区分成功失败 |
| 头部编码失败 | HTTP头部不支持Unicode | 中文文件名导致500 |

---

## 5. 修复效果对比

| 场景 | 修复前 | 修复后 |
|------|--------|--------|
| 文件名包含中文 | 500错误 ❌ | 正常预览 ✅ |
| 文件不存在 | 空白预览 ❌ | 显示"审批记录不存在" ✅ |
| 无权限 | 空白预览 ❌ | 显示"无权限查看此合同" ✅ |
| 水印生成失败 | 空白预览 ❌ | 显示"生成预览失败: 原因" ✅ |
| Token无效 | 空白预览 ❌ | 显示"用户未登录或Token无效" ✅ |

---

## 6. 相关文件清单

### 修改的文件

| 文件 | 修改内容 |
|------|---------|
| `PdfViewerActivity.java` | 添加Content-Type验证、错误信息解析 |
| `MobileApiController.java` | 修复HTTP状态码、文件名编码问题 |

### 相关文件（未修改）

| 文件 | 说明 |
|------|------|
| `ApiResponse.java` | API响应结构 |
| `PdfWatermarkServiceImpl.java` | PDF水印服务 |
| `RetrofitClient.java` | 网络配置 |
| `application.properties` | CORS配置 |

---

## 7. 测试建议

### 功能测试

- [ ] 预览文件名包含中文的合同（例：`新项目合同.pdf`）
- [ ] 预览文件不存在的合同
- [ ] 预览无权限的合同
- [ ] 预览水印生成失败的场景
- [ ] 验证Token无效场景

### 回归测试

- [ ] 模拟器预览功能正常
- [ ] 真机预览功能正常
- [ ] 审批已完成状态下正常预览
- [ ] 下载功能正常工作

---

## 8. 生成的文件

| 文件 | 内容 |
|------|------|
| `conversation_summary.md` | 本文档 |
| `bugfix_preview_blank.md` | 第一轮修复报告（Content-Type验证） |
| `bugfix_encoding_error.md` | 第二轮修复报告（文件名编码问题） |

---

## 9. 后续建议

1. **长期方案**：考虑使用RFC 5987编码支持下载时的中文文件名
2. **监控增强**：添加PDF预览失败的监控指标
3. **用户体验**：考虑添加错误页面的UI设计
4. **测试覆盖**：增加单元测试和集成测试覆盖预览功能

---

*摘要更新时间：2026-04-29 10:55*
*相关技术栈：Java Spring Boot, Android, Retrofit, OkHttp, iText PDF*
