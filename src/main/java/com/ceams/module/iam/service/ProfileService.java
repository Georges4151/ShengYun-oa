package com.ceams.module.iam.service;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人头像服务：上传、清空与读取当前账号头像。
 *
 * <p>头像文件本地落盘，库内 {@code sys_user.avatar} 仅保存相对访问地址； 读取接口按文件名直出字节流，供前端 {@code img} 标签直接加载。
 */
public interface ProfileService {

    /**
     * 上传当前登录用户的头像，返回新的头像访问地址。
     *
     * @param file 头像文件（jpg/jpeg/png/webp，且不超过 2MB）
     * @return 头像访问地址，形如 {@code /api/v1/profile/avatars/{文件名}}
     */
    String uploadAvatar(MultipartFile file);

    /** 清空当前登录用户的头像（置空 {@code sys_user.avatar}，并尽力删除旧文件）。 */
    void clearAvatar();

    /**
     * 读取头像文件内容。
     *
     * @param name 头像文件名（雪花 ID + 白名单扩展名）
     * @return 文件字节与对应媒体类型
     */
    AvatarFile loadAvatar(String name);

    /** 头像文件内容与媒体类型。 */
    record AvatarFile(byte[] bytes, MediaType mediaType) {}
}
