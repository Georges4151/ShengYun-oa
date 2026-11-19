package com.ceams.module.doc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * OnlyOffice HTTP 客户端配置。
 *
 * <p>项目此前未注册 {@link RestTemplate}，这里按需提供唯一实例：连接超时 2s、读取超时 30s，
 * 用于健康探活与保存回调时的文档下载（不引入新依赖，复用 spring-web 自带实现）。
 */
@Configuration
public class OnlyOfficeHttpConfig {

    /** OnlyOffice 专用 RestTemplate（超时可配置，避免探活阻塞业务线程）。 */
    @Bean
    public RestTemplate onlyOfficeRestTemplate(OnlyOfficeProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(1, properties.getConnectTimeoutMillis()));
        factory.setReadTimeout(Math.max(1, properties.getReadTimeoutMillis()));
        return new RestTemplate(factory);
    }
}
