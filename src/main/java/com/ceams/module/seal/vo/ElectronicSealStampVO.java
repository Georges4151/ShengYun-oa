package com.ceams.module.seal.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用印盖章记录视图对象。
 */
@Data
public class ElectronicSealStampVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sealId;

    private String sealName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    private String projectName;

    private String contractName;

    private String contractPath;

    private String stampPosition;

    private String stampImage;

    private LocalDateTime stampTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String userName;

    private String ipAddress;

    private LocalDateTime createTime;
}
