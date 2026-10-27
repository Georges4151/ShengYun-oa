/**
 * CEAMS 跨端统一基础类型。
 *
 * 跨端格式统一约束（与 ARCHITECTURE.md 第 5 节一致）：
 * - ID 一律字符串（后端雪花 ID 在 JSON 层序列化为字符串）
 * - 金额一律字符串（decimal(18,2) 序列化，避免浮点精度）
 * - 时间一律 ISO8601 字符串
 */

/** 全局唯一标识：DB bigint / JSON 字符串 */
export type Id = string

/** 金额：后端 BigDecimal -> JSON 字符串，如 "1280.50" */
export type Money = string

/** 时间：ISO8601 字符串，如 "2026-10-28T10:30:00" */
export type DateTime = string