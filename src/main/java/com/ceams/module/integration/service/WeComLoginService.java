package com.ceams.module.integration.service;

import com.ceams.module.integration.vo.WeComQrCodeVO;

/** 企业微信扫码登录服务。 */
public interface WeComLoginService {

    /**
     * 生成扫码登录地址与一次性 state。
     *
     * @param deviceId 发起扫码的设备指纹（可为空，随 state 存入 Redis）
     * @return 登录地址与 state
     * @throws com.ceams.common.exception.BusinessException 集成未启用（503）
     */
    WeComQrCodeVO qrCode(String deviceId);

    /**
     * 校验并消费 state，用授权码解析出本地用户。
     *
     * @param code 企业微信授权码
     * @param state 一次性 state
     * @return 已匹配的本地用户与 state 中记录的 deviceId
     * @throws com.ceams.common.exception.BusinessException state 过期 / 未绑定 / 授权失败（400），
     *     账号禁用（403），集成未启用（503）
     */
    WeComLoginResult resolveUser(String code, String state);
}
