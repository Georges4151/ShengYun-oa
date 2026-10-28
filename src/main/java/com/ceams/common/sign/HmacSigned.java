package com.ceams.common.sign;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 高危接口 HMAC 签名标注。
 *
 * <p>标注在 Controller 方法或类上。命中该注解的处理器会由 {@link HmacSignatureFilter}
 * 强制校验 {@code X-Sign / X-Timestamp / X-Nonce} 三头，用于 seal.stamp、expense.approve、
 * payment.execute 等防篡改、防重放场景。</p>
 *
 * <p>方法级注解优先于类级注解；类级注解表示该 Controller 下所有方法都需要签名。</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HmacSigned {
}
