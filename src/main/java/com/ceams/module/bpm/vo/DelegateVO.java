package com.ceams.module.bpm.vo;

import com.ceams.module.bpm.enums.DelegateStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 用户代理视图。 */
@Data
public class DelegateVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String userName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long delegateId;
    private String delegateName;
    private String bizType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private DelegateStatus status;
    private String reason;

    /** 当前是否处于代理时段（由服务端按当前时间计算）。 */
    private Boolean effective;
    private LocalDateTime createTime;
}
