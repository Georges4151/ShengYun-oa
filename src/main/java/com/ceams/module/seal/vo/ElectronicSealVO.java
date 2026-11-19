package com.ceams.module.seal.vo;

import com.ceams.module.seal.enums.SealStatus;
import com.ceams.module.seal.enums.StorageType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 电子印章视图对象。 */
@Data
public class ElectronicSealVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String sealName;

    private String sealType;

    private String sealCode;

    private String description;

    private SealStatus status;

    private String sealImage;

    private StorageType storageType;

    private String sealFileId;

    private String usbDriveId;

    private Boolean passwordProtected;

    private LocalDateTime lastUsedTime;

    private Integer usageCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
