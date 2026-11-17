package com.ceams.module.integration.client;

import java.util.List;

/**
 * 企业微信成员。
 *
 * @param userid 成员账号
 * @param name 姓名
 * @param mobile 手机号
 * @param email 邮箱
 * @param status 激活状态：1=已激活，2=已禁用，4=未激活，5=退出企业
 * @param departmentIds 所属部门 ID 列表
 */
public record WeComUser(
        String userid,
        String name,
        String mobile,
        String email,
        Integer status,
        List<Long> departmentIds) {}
