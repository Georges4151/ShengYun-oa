package com.ceams.module.seal.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 印章使用审计视图对象。
 */
@Data
public class ElectronicSealUsageVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sealId;

    private String sealName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String userName;

    private String purpose;

    private String documentType;

    private String documentPath;

    private LocalDateTime usageTime;

    private String ipAddress;

    private LocalDateTime createTime;
}
