package com.ceams.module.iam.vo;

import lombok.Data;

/** 头像操作结果视图对象。 */
@Data
public class AvatarVO {

    /** 头像访问地址（/api/v1/profile/avatars/{文件名}）。 */
    private String avatar;
}
