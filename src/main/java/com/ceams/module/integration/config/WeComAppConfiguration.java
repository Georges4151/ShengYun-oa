package com.ceams.module.integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 企业微信应用客户端 HTTP 配置。
 *
 * <p>按 {@code ceams.wecom.timeout-millis} 统一设置连接与读取超时，避免企业微信接口异常时
 * 长时间占用组织同步线程。
 */
@Configuration
public class WeComAppConfiguration {

    /** 企业微信应用客户端 Bean 名称。 */
    public static final String REST_CLIENT_BEAN_NAME = "weComAppRestClient";

    /** 构建带超时的 RestClient。 */
    @Bean(name = REST_CLIENT_BEAN_NAME)
    public RestClient weComAppRestClient(
            RestClient.Builder restClientBuilder, WeComAppProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = Math.max(500, properties.getTimeoutMillis());
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return restClientBuilder.requestFactory(factory).build();
    }
}
