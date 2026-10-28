package com.ceams.common.crypto;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 敏感字段（String）加密 TypeHandler。
 *
 * <p>作为 Spring Bean 由容器注入 {@link SensitiveEncryptor}（构造注入），并通过
 * {@link MybatisSensitiveTypeHandlerConfig} 显式注册到 MyBatis 的 TypeHandlerRegistry。
 * 实体敏感字段通过 {@code @TableField(typeHandler = SensitiveTypeHandler.class)} 显式指定本 Handler。</p>
 *
 * <p>注意：这里<strong>刻意不加 {@code @MappedTypes(String.class)}</strong>。因为 MyBatis-Plus
 * 会自动收集 Spring 容器中的 {@code TypeHandler} Bean，若标注 {@code @MappedTypes(String.class)}，
 * 会被注册为 String 类型的全局默认 Handler，导致所有未显式指定 TypeHandler 的 String 字段
 * 都被错误地加解密。故仅通过字段级 {@code @TableField(typeHandler=...)} 精确路由。</p>
 */
@Component
public class SensitiveTypeHandler extends BaseTypeHandler<String> {

    private final SensitiveEncryptor encryptor;

    public SensitiveTypeHandler(SensitiveEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, encryptor.encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return decrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return decrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return decrypt(cs.getString(columnIndex));
    }

    private String decrypt(String value) {
        return encryptor.decrypt(value);
    }
}
