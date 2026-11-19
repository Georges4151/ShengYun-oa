package com.ceams.module.iam.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.iam.service.ProfileService;
import com.ceams.module.iam.vo.AvatarVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 个人中心接口：当前登录账号的头像设置与读取。 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /** 上传并替换当前账号头像。 */
    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AvatarVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        AvatarVO vo = new AvatarVO();
        vo.setAvatar(profileService.uploadAvatar(file));
        return ApiResponse.success(vo);
    }

    /** 清空当前账号头像。 */
    @DeleteMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> clearAvatar() {
        profileService.clearAvatar();
        return ApiResponse.success();
    }

    /** 读取头像图片（匿名可访问，文件名由雪花 ID 生成，便于 {@code img} 标签直接加载）。 */
    @GetMapping("/avatars/{name}")
    public ResponseEntity<byte[]> loadAvatar(@PathVariable String name) {
        ProfileService.AvatarFile avatar = profileService.loadAvatar(name);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(avatar.mediaType());
        headers.setContentDisposition(ContentDisposition.inline().filename(name).build());
        headers.setCacheControl("public, max-age=86400");
        return new ResponseEntity<>(avatar.bytes(), headers, HttpStatus.OK);
    }
}
