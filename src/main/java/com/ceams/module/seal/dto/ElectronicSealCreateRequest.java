package com.ceams.module.seal.dto;

import com.ceams.module.seal.enums.StorageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新建电子印章。 */
@Data
public class ElectronicSealCreateRequest {

    @NotBlank(message = "印章名不能为空")
    @Size(max = 200, message = "印章名长度不能超过 200")
    private String sealName;

    @NotBlank(message = "印章类型不能为空")
    @Size(max = 50, message = "印章类型长度不能超过 50")
    private String sealType;

    @Size(max = 100, message = "印章编码长度不能超过 100")
    private String sealCode;

    private String description;

    @Size(max = 1000, message = "印章图片路径长度不能超过 1000")
    private String sealImage;

    private StorageType storageType;

    @Size(max = 255, message = "印章文件ID长度不能超过 255")
    private String sealFileId;

    @Size(max = 255, message = "USB设备ID长度不能超过 255")
    private String usbDriveId;

    /** 是否密码保护（缺省 false）。 */
    private Boolean passwordProtected;

    /** 印章密码（启用密码保护时必填，落库前 BCrypt 加密）。 */
    @Size(max = 72, message = "印章密码长度不能超过 72")
    private String password;
}
