package com.ceams.module.ai.client;

/**
 * Ollama 流式响应末行的 token 用量。
 *
 * @param promptTokens 提示词 token 数（{@code prompt_eval_count}）
 * @param completionTokens 生成 token 数（{@code eval_count}）
 */
public record OllamaUsage(int promptTokens, int completionTokens) {}
