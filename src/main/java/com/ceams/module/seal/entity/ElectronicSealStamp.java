package com.ceams.module.seal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用印盖章记录（biz_electronic_seal_stamp）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_electronic_seal_stamp")
public class ElectronicSealStamp extends BaseEntity {

    /** 印章 ID（逻辑引用 biz_electronic_seal.id）。 */
    private Long sealId;

    /** 印章名（冗余）。 */
    private String sealName;

    /** 项目 ID（逻辑引用 biz_project.id）。 */
    private Long projectId;

    /** 项目名（冗余）。 */
    private String projectName;

    /** 合同名。 */
    private String contractName;

    /** 合同文件路径。 */
    private String contractPath;

    /** 盖章位置。 */
    private String stampPosition;

    /** 盖章图片（路径）。 */
    private String stampImage;

    /** 盖章时间。 */
    private LocalDateTime stampTime;

    /** 操作人 ID（逻辑引用 sys_user.id）。 */
    private Long userId;

    /** 操作人姓名（冗余）。 */
    private String userName;

    /** IP 地址。 */
    private String ipAddress;
}
