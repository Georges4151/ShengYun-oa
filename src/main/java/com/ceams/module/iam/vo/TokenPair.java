package com.ceams.module.iam.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * access + refresh 令牌对。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenPair {

    private String accessToken;

    private String refreshToken;

    /** access 有效秒数。 */
    private long expiresIn;
}