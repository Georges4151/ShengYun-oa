package com.ceams.module.iam.vo;

import com.ceams.module.iam.enums.Platform;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 用户活跃会话视图对象。 */
@Data
public class UserSessionVO {

    /** 会话标识（refresh token jti）。 */
    private String sessionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private Platform platform;

    private String ipAddress;

    private LocalDateTime loginTime;

    private LocalDateTime lastActiveTime;
}
