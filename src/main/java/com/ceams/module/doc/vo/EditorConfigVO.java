package com.ceams.module.doc.vo;

import java.util.Map;
import lombok.Data;

/** OnlyOffice 编辑器配置响应。 */
@Data
public class EditorConfigVO {

    /** Document Server 基地址（服务端探活与回调来源校验用，容器内地址）。 */
    private String documentServerUrl;

    /** Document Server 的浏览器可达地址（前端据此动态加载 api.js）。 */
    private String browserDocumentServerUrl;

    /** 编辑器 config（document / editorConfig / type）。 */
    private Map<String, Object> config;

    /** 整份 config 的 JWT 签名（OnlyOffice 8.1 要求 body.token）。 */
    private String token;

    /** 编辑模式：edit / view。 */
    private String mode;

    /** 文件 ID。 */
    private Long fileId;
}
