package com.ceams.module.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 刷新令牌（sys_refresh_token）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_refresh_token")
public class SysRefreshToken extends BaseEntity {

    /** 用户 ID。 */
    private Long userId;

    /** 令牌唯一标识（JTI）。 */
    private String jti;

    /** refresh token 哈希。 */
    private String tokenHash;

    /** 设备指纹。 */
    private String deviceId;

    /** 过期时间。 */
    private LocalDateTime expiresAt;

    /** 是否已吊销：0 有效，1 已吊销。 */
    private Integer revoked;

    /** 轮换后替换本令牌的 jti。 */
    private String replacedByJti;

    /** 吊销时间。 */
    private LocalDateTime revokedAt;
}
