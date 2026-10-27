package com.ceams.module.iam.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录 / 刷新响应负载。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private TokenPair tokenPair;

    private UserVO user;
}