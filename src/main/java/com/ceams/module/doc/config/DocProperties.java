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

    /** 文档知识库 RAG 配置（抽取 / 分块 / 检索）。 */
    private Rag rag = new Rag();

    /**
     * RAG 配置（{@code ceams.doc.rag.*}）。
     *
     * <p>关闭 {@link #enabled} 时索引任务直接置 SKIPPED，检索接口仍可调用（返回空结果），
     * 上传 / 下载 / 预览 / 在线编辑链路不受影响。
     */
    @Data
    public static class Rag {

        /** 是否启用抽取与索引（默认开启；关闭后不做任何文本抽取）。 */
        private boolean enabled = true;

        /** 分块目标长度（字符），默认 600。 */
        private int chunkSize = 600;

        /** 相邻分块重叠字符数，默认 100。 */
        private int chunkOverlap = 100;

        /** 单文件最大抽取字符数，超出后截断，默认 200 万。 */
        private int maxExtractChars = 2000000;

        /** 检索时召回的候选分块条数上限，默认 300。 */
        private int searchCandidateLimit = 300;

        /** 检索接口默认返回条数，默认 5。 */
        private int searchDefaultLimit = 5;

        /** 检索接口允许的最大返回条数，默认 10。 */
        private int maxSearchLimit = 10;
    }
}
