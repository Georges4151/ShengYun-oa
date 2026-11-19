package com.ceams.module.doc.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.doc.config.OnlyOfficeProperties;
import io.jsonwebtoken.Claims;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** OnlyOffice JWT 签发 / 校验测试：长短密钥两条路径与空密钥降级。 */
class OnlyOfficeJwtSupportTest {

    @Test
    void signAndVerifyWithLongSecret() {
        OnlyOfficeProperties properties = properties("ceams-dev-secret-0123456789abcdef");
        OnlyOfficeJwtSupport support = new OnlyOfficeJwtSupport(properties);

        String token = support.signConfig(Map.of("document", Map.of("key", "1_1")));
        Claims claims = support.verify(token);

        assertThat(token.split("\\.")).hasSize(3);
        assertThat(claims.get("document", Map.class)).containsEntry("key", "1_1");
    }

    @Test
    void signAndVerifyWithShortSecret() {
        // 16 字节开发密钥：jjwt HS256 拒绝，走原始 HMAC 兼容路径
        OnlyOfficeJwtSupport support = new OnlyOfficeJwtSupport(properties("ceams-dev-secret"));

        String token = support.sign(Map.of("key", "21_1"));
        Claims claims = support.verify(token);

        assertThat(token.split("\\.")).hasSize(3);
        assertThat(claims.get("key", String.class)).isEqualTo("21_1");
    }

    @Test
    void verifyShouldRejectTamperedToken() {
        OnlyOfficeJwtSupport support = new OnlyOfficeJwtSupport(properties("ceams-dev-secret"));
        String token = support.sign(Map.of("key", "21_1"));
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> support.verify(tampered))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
    }

    @Test
    void signShouldRejectBlankSecret() {
        OnlyOfficeJwtSupport support = new OnlyOfficeJwtSupport(properties(""));

        assertThatThrownBy(() -> support.sign(Map.of("key", "1_1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OnlyOffice JWT 未配置")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SYSTEM_SERVICE_UNAVAILABLE);
    }

    private OnlyOfficeProperties properties(String secret) {
        OnlyOfficeProperties properties = new OnlyOfficeProperties();
        properties.setJwtSecret(secret);
        return properties;
    }
}
