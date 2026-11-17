package com.ceams.module.doc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文档知识库配置（{@code ceams.doc.*}）。
 *
 * <p>沿用行政后勤配置写法：{@code @Component + @ConfigurationProperties} 由组件扫描直接注册，
 * 无需额外的 {@code @EnableConfigurationProperties}。OnlyOffice 相关配置见 {@link OnlyOfficeProperties}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ceams.doc")
public class DocProperties {

    /** 单文件上传上限（字节），默认 200MB。 */
    private long maxFileSize = 209715200L;

    /** 分片大小（字节），默认 5MB。 */
    private int chunkSize = 5242880;

    /** 编辑锁有效期（分钟），默认 30。 */
    private int lockTtlMinutes = 30;

    /** 一次性下载 token 有效期（秒），默认 300。 */
    private int downloadTokenTtlSeconds = 300;

    /** 分片临时目录（含 Redis meta）保留时长（小时），默认 24。 */
    private int uploadTempTtlHours = 24;
}
