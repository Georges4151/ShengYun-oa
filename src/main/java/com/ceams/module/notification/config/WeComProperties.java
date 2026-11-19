package com.ceams.module.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 企业微信群机器人推送配置（{@code ceams.notification.wecom.*}）。
 *
 * <p>默认关闭：{@link #enabled} 为 false 或 {@link #webhookUrl} 为空时不发起任何外部请求， 审批主流程不受影响。
 */
@Data
@ConfigurationProperties(prefix = "ceams.notification.wecom")
public class WeComProperties {

    /** 是否启用企业微信群机器人推送。 */
    private boolean enabled = false;

    /** 群机器人 webhook 地址（需为 http/https）。 */
    private String webhookUrl = "";

    /** 单次请求的连接/读取超时（毫秒）。 */
    private int timeoutMillis = 3000;

    /** 单条消息正文最大长度（超出截断，避免企业微信消息体过大）。 */
    private int maxContentLength = 800;
}
