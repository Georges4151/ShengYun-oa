package com.ceams.common.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * {@link SensitiveEncryptor} 纯单元测试（无需数据库 / Spring 容器）。
 *
 * <p>覆盖：加解密往返、随机 IV 导致密文变化、null/blank 原样返回、篡改后解密失败。
 */
class SensitiveEncryptorTest {

    /** Base64 编码的 32 字节测试密钥（仅测试用）。 */
    private static final String TEST_SECRET = "Y2VhbXMtc2Vuc2l0aXZlLWRldi1rZXktMjAyNi0wMTI=";

    private final SensitiveEncryptor encryptor = new SensitiveEncryptor(TEST_SECRET);

    @Test
    void 加解密往返一致() {
        String plaintext = "13800138000";
        String ciphertext = encryptor.encrypt(plaintext);
        assertEquals(plaintext, encryptor.decrypt(ciphertext));
    }

    @Test
    void 相同明文两次加密产生不同密文() {
        String plaintext = "6222020200112233445";
        String first = encryptor.encrypt(plaintext);
        String second = encryptor.encrypt(plaintext);
        assertNotEquals(first, second, "随机 IV 应保证相同明文产生不同密文");
    }

    @Test
    void 空值与空白原样返回() {
        assertNull(encryptor.encrypt(null));
        assertEquals("", encryptor.encrypt(""));
        assertEquals("   ", encryptor.encrypt("   "));

        assertNull(encryptor.decrypt(null));
        assertEquals("", encryptor.decrypt(""));
        assertEquals("   ", encryptor.decrypt("   "));
    }

    @Test
    void 篡改密文后解密失败() {
        String ciphertext = encryptor.encrypt("13800138000");
        // 篡改 Base64 载荷中的一个字符（去掉版本前缀后翻转中间字符）
        String payload = ciphertext.substring("v1:".length());
        char[] chars = payload.toCharArray();
        int mid = chars.length / 2;
        chars[mid] = chars[mid] == 'A' ? 'B' : 'A';
        String tampered = "v1:" + new String(chars);

        assertThrows(IllegalStateException.class, () -> encryptor.decrypt(tampered));
    }

    @Test
    void 非法密文解密失败() {
        assertThrows(IllegalStateException.class, () -> encryptor.decrypt("not-a-valid-base64"));
    }
}
