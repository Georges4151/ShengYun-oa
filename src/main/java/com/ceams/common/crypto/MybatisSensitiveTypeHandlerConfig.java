package com.ceams.common.crypto;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 敏感字段 TypeHandler 注册配置。
 *
 * <p>MyBatis-Plus 的自动配置会收集 Spring 容器中的 {@code TypeHandler} Bean，但依赖
 * {@code @MappedTypes} 注解才能映射到具体 Java 类型；一旦标注 {@code @MappedTypes(String.class)}
 * 会覆盖 String 的全局默认 Handler。因此这里通过 {@link ConfigurationCustomizer} 显式把
 * {@link SensitiveTypeHandler} 实例注册进 {@code allTypeHandlersMap}（仅登记实例、不映射默认类型），
 * 实体字段再通过 {@code @TableField(typeHandler = SensitiveTypeHandler.class)} 精确引用。</p>
 *
 * <p><strong>读写路径差异</strong>：字段级 {@code typeHandler} 在<em>写入</em>（ParameterMapping）
 * 时始终生效；但在<em>读取</em>（ResultMapping）时，MyBatis-Plus 只有在实体开启
 * {@code @TableName(autoResultMap = true)} 时才会为 {@code selectById} 等单表查询构建 resultMap
 * 并携带该 typeHandler，否则会走 resultType 自动映射而忽略 typeHandler。因此<strong>凡含敏感字段的
 * 实体必须同时标注 {@code @TableName(autoResultMap = true)}</strong>，否则写库为密文、读回仍是密文。</p>
 */
@Configuration
public class MybatisSensitiveTypeHandlerConfig {

    @Bean
    public ConfigurationCustomizer sensitiveTypeHandlerCustomizer(SensitiveTypeHandler sensitiveTypeHandler) {
        return configuration -> configuration.getTypeHandlerRegistry()
                .register((Class<String>) null, sensitiveTypeHandler);
    }
}
