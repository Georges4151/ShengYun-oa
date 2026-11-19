package com.ceams.module.seal.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 印章授权视图对象。 */
@Data
public class SealAuthorizationVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sealId;

    /** 印章名（回填）。 */
    private String sealName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 用户姓名（回填，姓名为空时回退登录账号）。 */
    private String userName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Boolean enabled;

    private String remark;

    private LocalDateTime createdAt;
}
