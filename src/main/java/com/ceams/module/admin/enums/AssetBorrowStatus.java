package com.ceams.module.admin.enums;

import java.util.Locale;

/**
 * 资产领用记录状态（admin_asset_borrow.status）。
 *
 * <p>提交领用后记录先落 {@link #APPROVING}（审批中，列类型 VARCHAR(20) 可承载），
 * 审批通过由回调推进为 {@link #BORROWING}，驳回 / 撤回 / 终止推进为 {@link #REJECTED}。
 */
public enum AssetBorrowStatus {
    /** 审批中（已提交 BPM、尚未产生终态）。 */
    APPROVING,
    /** 借出中（审批通过后由回调写入）。 */
    BORROWING,
    /** 已归还。 */
    RETURNED,
    /** 已驳回 / 撤回 / 终止。 */
    REJECTED;

    /** 编码解析，未知返回 null（调用方按参数非法处理）。 */
    public static AssetBorrowStatus from(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
