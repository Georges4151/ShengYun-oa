package com.ceams.module.integration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 企业微信应用集成配置（{@code ceams.wecom.*}）。
 *
 * <p>与群机器人配置 {@code ceams.notification.wecom.*} 相互独立：前者面向企业微信「自建应用」
 * （组织同步、扫码登录、应用消息），后者仅面向群机器人 webhook 推送。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ceams.wecom")
public class WeComAppProperties {

    /** 是否启用企业微信应用集成。 */
    private boolean enabled = false;

    /** 企业 ID（corpid）。 */
    private String corpId = "";

    /** 自建应用 AgentId。 */
    private String agentId = "";

    /** 自建应用 Secret。 */
    private String secret = "";

    /** 企业微信 API 基地址。 */
    private String apiBaseUrl = "https://qyapi.weixin.qq.com";

    /** 企业微信扫码登录基地址（P3 使用）。 */
    private String loginBaseUrl = "https://login.work.weixin.qq.com";

    /** 扫码登录回调地址（P3 使用）。 */
    private String redirectUri = "http://localhost:5173/login/wecom";

    /** 组织同步定时表达式，默认每天 02:00。 */
    private String syncCron = "0 0 2 * * ?";

    /** 应用消息发件箱调度间隔（毫秒，P4 使用）。 */
    private long dispatchIntervalMs = 30000L;

    /** 应用消息最大重试次数（P4 使用）。 */
    private int messageRetryMax = 5;

    /** 调用企业微信 API 的连接 / 读取超时（毫秒）。 */
    private int timeoutMillis = 5000;

    /**
     * 是否已具备调用企业微信应用接口的最低配置。
     *
     * @return 开关开启且 corpId、secret 均非空时返回 true
     */
    public boolean isConfigured() {
        return enabled && StringUtils.hasText(corpId) && StringUtils.hasText(secret);
    }
}
