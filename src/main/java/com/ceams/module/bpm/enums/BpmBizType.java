package com.ceams.module.bpm.enums;

import java.util.Locale;

/** BPM 业务类型（bpm_template.biz_type / bpm_instance.biz_type）。 */
public enum BpmBizType {
    /** 报销。 */
    EXPENSE,
    /** 项目变更。 */
    CHANGE_REQUEST,
    /** HR 转正。 */
    HR_REGULAR,
    /** HR 调岗。 */
    HR_TRANSFER,
    /** HR 离职。 */
    HR_RESIGN,
    /** HR 请假。 */
    HR_LEAVE,
    /** HR 补卡。 */
    HR_CHECKIN_SUPPLEMENT,
    /** HR 薪资变动（调整 / 晋升调薪）。 */
    HR_SALARY_CHANGE,
    /** HR 薪资更正。 */
    HR_SALARY_CORRECTION,
    /** HR 绩效考评。 */
    HR_PERFORMANCE,
    /** HR 晋升。 */
    HR_PROMOTION,
    /** 行政资产领用。 */
    ADMIN_ASSET_BORROW,
    /** 行政易耗品申领。 */
    ADMIN_MATERIAL_APPLY,
    /** 行政用车申请。 */
    ADMIN_VEHICLE_APPLY,
    /** 行政会议室预定。 */
    ADMIN_MEETING_APPLY,
    /** 行政报修。 */
    ADMIN_REPAIR,
    /** HR 劳动合同。 */
    HR_CONTRACT,
    /** 分包合同。 */
    SUBCONTRACT_CONTRACT;

    /** 编码转枚举，未知返回 null（调用方按参数非法处理）。 */
    public static BpmBizType from(String code) {
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
