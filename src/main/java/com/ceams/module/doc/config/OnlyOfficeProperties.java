package com.ceams.module.doc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OnlyOffice 在线编辑配置（{@code ceams.doc.onlyoffice.*}）。
 *
 * <p>沿用文档模块配置写法：{@code @Component + @ConfigurationProperties} 由组件扫描直接注册。
 *
 * <p>关键环境约束：{@code public-base-url} 是 Document Server 容器访问 CEAMS 的地址，必须是容器网络可达地址
 * （本地 compose 为 {@code http://app:9095}），<b>不能填 localhost</b>；否则 Document Server 无法拉取原件、
 * 也无法回调保存接口。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ceams.doc.onlyoffice")
public class OnlyOfficeProperties {

    /** 是否启用在线编辑；关闭时 editor-config 直接降级为 503，上传/下载/版本不受影响。 */
    private boolean enabled = false;

    /** Document Server 地址（服务端探活与回调来源校验用），默认本地 compose 服务名。 */
    private String documentServerUrl = "http://onlyoffice";

    /**
     * Document Server 的<b>浏览器</b>可达地址（前端据此加载 {@code api.js}）。
     *
     * <p>{@code document-server-url} 是容器内地址（如 {@code http://onlyoffice}），浏览器无法访问；
     * 本地 compose 中 Document Server 映射到宿主机 8081，故默认 {@code http://localhost:8081}。
     */
    private String browserDocumentServerUrl = "http://localhost:8081";

    /** CEAMS 对 Document Server 暴露的基地址，必须是容器可达地址（不能填 localhost）。 */
    private String publicBaseUrl = "http://app:9095";

    /** JWT 共享密钥（与 Document Server 的 JWT_SECRET 保持一致）；为空时在线编辑不可用。 */
    private String jwtSecret = "";

    /** 保存回调路径（与 Controller 映射一致）。 */
    private String callbackPath = "/api/v1/doc/onlyoffice/callback";

    /** 连接超时（毫秒），默认 2s。 */
    private int connectTimeoutMillis = 2000;

    /** 读取超时（毫秒），默认 30s（保存回调下载文档体）。 */
    private int readTimeoutMillis = 30000;
}
