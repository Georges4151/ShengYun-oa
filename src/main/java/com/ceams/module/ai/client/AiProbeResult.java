package com.ceams.module.ai.client;

/**
 * 探活结果：失败时携带面向管理员的中文提示，不向上抛异常。
 *
 * @param success 是否成功
 * @param message 中文提示
 */
public record AiProbeResult(boolean success, String message) {

    /** 成功结果。 */
    public static AiProbeResult ok() {
        return new AiProbeResult(true, "连接成功，模型可用");
    }

    /** 失败结果。 */
    public static AiProbeResult fail(String message) {
        return new AiProbeResult(false, message);
    }
}
