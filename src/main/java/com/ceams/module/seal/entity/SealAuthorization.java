package com.ceams.module.seal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 印章授权（biz_seal_authorization）：管理员将指定印章授权给用户，在有效期内可使用。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_seal_authorization")
public class SealAuthorization extends BaseEntity {

    /** 印章 ID。 */
    private Long sealId;

    /** 被授权用户 ID。 */
    private Long userId;

    /** 授权开始时间。 */
    private LocalDateTime startTime;

    /** 授权结束时间。 */
    private LocalDateTime endTime;

    /** 是否启用：true=启用。 */
    private Boolean enabled = Boolean.TRUE;

    /** 备注。 */
    private String remark;
}
