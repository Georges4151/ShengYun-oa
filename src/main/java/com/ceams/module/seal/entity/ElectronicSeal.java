package com.ceams.module.seal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.seal.enums.SealStatus;
import com.ceams.module.seal.enums.StorageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 电子印章（biz_electronic_seal）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_electronic_seal")
public class ElectronicSeal extends BaseEntity {

    /** 印章名。 */
    private String sealName;

    /** 印章类型（代码值，如 公章/合同章/财务章）。 */
    private String sealType;

    /** 印章编码。 */
    private String sealCode;

    /** 描述。 */
    private String description;

    /** 状态：ACTIVE/DISABLED/ARCHIVED。 */
    private SealStatus status = SealStatus.ACTIVE;

    /** 印章图片（路径）。 */
    private String sealImage;

    /** 存储类型：FILE/USB_DRIVE/CLOUD。 */
    private StorageType storageType;

    /** 印章文件 ID。 */
    private String sealFileId;

    /** USB 设备 ID。 */
    private String usbDriveId;

    /** 是否密码保护：true/false。 */
    private Boolean passwordProtected = Boolean.FALSE;

    /** 最后使用时间。 */
    private LocalDateTime lastUsedTime;

    /** 使用次数。 */
    private Integer usageCount = 0;
}
