package com.ceams.module.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 行政后勤配置（{@code ceams.admin.*}）。
 *
 * <p>沿用 HR 配置写法：{@code @Component + @ConfigurationProperties} 由组件扫描直接注册，
 * 无需额外的 {@code @EnableConfigurationProperties}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ceams.admin")
public class AdminProperties {

    /** 到期类预警窗口（天），用于车辆保险 / 年检等后续功能。 */
    private int alertDays = 30;

    /** 访客二维码核销有效期（小时），预留给后续访客模块。 */
    private int qrCheckinWindowHours = 24;
}
