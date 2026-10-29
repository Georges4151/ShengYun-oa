package com.ceams.common.crypto;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.stereotype.Component;

/**
 * 敏感字段（String）加密 TypeHandler。
 *
 * <p>作为 Spring Bean 由容器注入 {@link SensitiveEncryptor}（构造注入），并通过 {@link
 * MybatisSensitiveTypeHandlerConfig} 显式登记到 MyBatis 的 TypeHandlerRegistry。 实体敏感字段通过
 * {@code @TableField(typeHandler = SensitiveTypeHandler.class)} 显式指定本 Handler。
 *
 * <p><strong>为何直接实现 {@link TypeHandler} 而不继承 {@code BaseTypeHandler}？</strong> MyBatis 的 {@code
 * BaseTypeHandler<T>} 继承自 {@code TypeReference<T>}。MyBatis-Plus 自动配置会收集 Spring 容器中的 {@code
 * TypeHandler} Bean 并调用 {@code TypeHandlerRegistry#register(TypeHandler)}， 该方法会对 {@code
 * TypeReference} 做泛型自动发现（此处解析为 {@code String.class}），从而把本 Handler 注册为 String 的<em>全局默认</em>
 * Handler，导致同一实体中所有未显式标注的 String 字段都被错误加解密。 因此这里直接实现 {@code TypeHandler<String>}，使自动收集仅登记实例（写入
 * {@code allTypeHandlersMap}）， 不会覆盖 String 全局默认 Handler，仅通过字段级 {@code @TableField(typeHandler=...)}
 * 精确路由。
 */
@Component
public class SensitiveTypeHandler implements TypeHandler<String> {

    private final SensitiveEncryptor encryptor;

    public SensitiveTypeHandler(SensitiveEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        if (parameter == null) {
            if (jdbcType == null) {
                ps.setNull(i, Types.VARCHAR);
            } else {
                ps.setNull(i, jdbcType.TYPE_CODE);
            }
        } else {
            ps.setString(i, encryptor.encrypt(parameter));
        }
    }

    @Override
    public String getResult(ResultSet rs, String columnName) throws SQLException {
        return decrypt(rs.getString(columnName));
    }

    @Override
    public String getResult(ResultSet rs, int columnIndex) throws SQLException {
        return decrypt(rs.getString(columnIndex));
    }

    @Override
    public String getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return decrypt(cs.getString(columnIndex));
    }

    private String decrypt(String value) {
        return encryptor.decrypt(value);
    }
}
