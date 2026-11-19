package com.ceams.common.crypto;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 敏感字段 TypeHandler 注册配置。
 *
 * <p>MyBatis-Plus 的自动配置会收集 Spring 容器中的 {@code TypeHandler} Bean 并调用 {@code
 * TypeHandlerRegistry#register(TypeHandler)}；该方法会对 {@code TypeReference} 做泛型自动发现。 由于 {@code
 * BaseTypeHandler<String>} 继承 {@code TypeReference<String>}，一旦 {@link SensitiveTypeHandler} 继承
 * {@code BaseTypeHandler}，就会被自动注册为 String 的全局默认 Handler，覆盖默认的 {@code StringTypeHandler}，导致实体中所有
 * String 字段被错误加解密。因此 {@link SensitiveTypeHandler} 已改为直接实现 {@code TypeHandler<String>} 规避该问题，本配置再显式把
 * 其实例登记进 {@code allTypeHandlersMap}（仅登记实例、不映射默认类型），实体字段通过 {@code @TableField(typeHandler =
 * SensitiveTypeHandler.class)} 精确引用。
 *
 * <p><strong>读写路径差异</strong>：字段级 {@code typeHandler} 在<em>写入</em>（ParameterMapping）
 * 时始终生效；但在<em>读取</em>（ResultMapping）时，MyBatis-Plus 只有在实体开启 {@code @TableName(autoResultMap = true)}
 * 时才会为 {@code selectById} 等单表查询构建 resultMap 并携带该 typeHandler，否则会走 resultType 自动映射而忽略
 * typeHandler。因此<strong>凡含敏感字段的 实体必须同时标注 {@code @TableName(autoResultMap =
 * true)}</strong>，否则写库为密文、读回仍是密文。
 */
@Configuration
public class MybatisSensitiveTypeHandlerConfig {

    @Bean
    public ConfigurationCustomizer sensitiveTypeHandlerCustomizer(
            SensitiveTypeHandler sensitiveTypeHandler) {
        return configuration ->
                configuration
                        .getTypeHandlerRegistry()
                        .register((Class<String>) null, sensitiveTypeHandler);
    }
}
