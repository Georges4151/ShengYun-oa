package com.ceams.module.integration.vo;

import lombok.Data;

/** 企业微信扫码登录二维码。 */
@Data
public class WeComQrCodeVO {

    /** 企业微信扫码登录地址（前端直接跳转或渲染为二维码）。 */
    private String loginUrl;

    /** 一次性随机 state（32 位十六进制，5 分钟有效）。 */
    private String state;
}
