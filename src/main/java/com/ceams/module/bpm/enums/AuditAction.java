package com.ceams.module.bpm.enums;

/** 审计动作（biz_audit_snapshot.action）。 */
public enum AuditAction {
    /** 发起流程。 */
    START,
    /** 同意。 */
    APPROVE,
    /** 驳回。 */
    REJECT,
    /** 撤回。 */
    WITHDRAW,
    /** 终止。 */
    TERMINATE,
    /** 改派。 */
    DELEGATE,
    /** 模板发布。 */
    TEMPLATE_PUBLISH,
    /** 业务单据手工调整（如月度工资核算单项调整）。 */
    ADJUST,
    /** 业务单据批量确认（如月度工资核算确认）。 */
    CONFIRM,
    /** 业务单据批量发布（如工资条发布）。 */
    PUBLISH,
    /** 资产报废。 */
    SCRAP,
    /** 库存入库 / 扣减。 */
    STOCK_IN,
    /** 资产盘点。 */
    INVENTORY,
    /** 文档上传。 */
    DOC_UPLOAD,
    /** 文档编辑（含重命名、移动、在线保存）。 */
    DOC_EDIT,
    /** 文档删除。 */
    DOC_DELETE,
    /** 文档授权（含取消授权）。 */
    DOC_GRANT,
    /** 文档版本回滚。 */
    DOC_ROLLBACK,
    /** 企业微信组织同步。 */
    WECOM_SYNC,
    /** 企业微信应用消息重投。 */
    WECOM_MESSAGE_RETRY
}
