package com.ceams.common.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 敏感字段统一加解密器（AES-256-GCM）。
 *
 * <p>密钥从配置 {@code ceams.sensitive.secret} 读取，为 Base64 编码的 32 字节（256 位）密钥。
 * 生产环境必须通过 {@code CEAMS_SENSITIVE_KEY} 环境变量覆盖，仓库内仅保留 dev 占位默认值。</p>
 *
 * <p>密文格式（带版本前缀，便于未来算法升级）：
 * {@code v1: + Base64( IV(12字节) || 密文 || GCM Tag(16字节) )}。
 * 每次加密使用随机 12 字节 IV，保证相同明文产生不同密文。</p>
 */
@Component
public class SensitiveEncryptor {

    /** GCM 推荐 IV 长度（字节）。 */
    private static final int IV_LENGTH = 12;

    /** GCM 认证标签长度（位）。 */
    private static final int TAG_BITS = 128;

    /** 密文版本前缀。 */
    private static final String PREFIX = "v1:";

    /** AES-GCM 算法。 */
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    /** 密钥长度（字节）。 */
    private static final int KEY_LENGTH = 32;

    /** 线程安全的随机源，仅用于生成 IV。 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 解密得到的对称密钥。 */
    private final SecretKey key;

    public SensitiveEncryptor(@Value("${ceams.sensitive.secret}") String secret) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        if (keyBytes.length != KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "ceams.sensitive.secret 必须是 Base64 编码的 32 字节密钥，实际解码长度=" + keyBytes.length);
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 加密明文。null / 空白字符串原样返回（避免对空值生成无意义密文）。
     *
     * @param plaintext 明文
     * @return 密文；null/blank 原样返回
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("敏感字段加密失败", e);
        }
    }

    /**
     * 解密密文。null / 空白字符串原样返回。
     *
     * <p>密文被篡改、版本不匹配或密钥不一致时抛出 {@link IllegalStateException}（GCM 认证失败）。</p>
     *
     * @param ciphertext 密文
     * @return 明文；null/blank 原样返回
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return ciphertext;
        }
        try {
            String payload = ciphertext;
            if (payload.startsWith(PREFIX)) {
                payload = payload.substring(PREFIX.length());
            }
            byte[] combined = Base64.getDecoder().decode(payload);
            if (combined.length < IV_LENGTH + TAG_BITS / 8) {
                throw new IllegalArgumentException("密文长度非法，疑似被截断或篡改");
            }
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("敏感字段解密失败（密文可能被篡改）", e);
        }
    }
}
