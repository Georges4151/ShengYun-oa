package com.ceams.module.seal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 印章使用审计（biz_electronic_seal_usage）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_electronic_seal_usage")
public class ElectronicSealUsage extends BaseEntity {

    /** 印章 ID（逻辑引用 biz_electronic_seal.id）。 */
    private Long sealId;

    /** 印章名（冗余）。 */
    private String sealName;

    /** 使用人 ID（逻辑引用 sys_user.id）。 */
    private Long userId;

    /** 使用人姓名（冗余）。 */
    private String userName;

    /** 使用事由。 */
    private String purpose;

    /** 文档类型（代码值）。 */
    private String documentType;

    /** 文档路径。 */
    private String documentPath;

    /** 使用时间。 */
    private LocalDateTime usageTime;

    /** IP 地址。 */
    private String ipAddress;
}
