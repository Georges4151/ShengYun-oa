package com.ceams.module.iam.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.service.ProfileService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/** {@link ProfileServiceImpl} 头像上传/读取/清空单元测试。 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    /** 单测登录用户 ID。 */
    private static final long USER_ID = 1001L;

    @Mock private UserMapper userMapper;

    @TempDir private Path tempDir;

    private ProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProfileServiceImpl(userMapper);
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        // SecurityUtils 从 principal 取 Long 作为用户 ID，此处构造已认证上下文
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 上传头像_非图片扩展名被拒() {
        MockMultipartFile file =
                new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> service.uploadAvatar(file))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(userMapper, never()).update(any(), any());
    }

    @Test
    void 上传头像_超过2MB被拒() {
        byte[] oversized = new byte[2 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", oversized);

        assertThatThrownBy(() -> service.uploadAvatar(file))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(userMapper, never()).update(any(), any());
    }

    @Test
    void 上传头像_正常落盘并回写地址() throws Exception {
        SysUser user = new SysUser();
        user.setId(USER_ID);
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(userMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        MockMultipartFile file =
                new MockMultipartFile("file", "Me.PNG", "image/png", new byte[] {9, 8, 7});

        String url = service.uploadAvatar(file);

        assertThat(url).matches("^/api/v1/profile/avatars/\\d+\\.png$");
        String name = url.substring("/api/v1/profile/avatars/".length());
        Path saved = tempDir.resolve("avatars").resolve(name);
        assertThat(Files.isRegularFile(saved)).isTrue();
        assertThat(Files.readAllBytes(saved)).containsExactly(9, 8, 7);
        verify(userMapper).update(isNull(), any(UpdateWrapper.class));
    }

    @Test
    void 读取头像_路径穿越被拒() {
        assertThatThrownBy(() -> service.loadAvatar("../evil.png"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
    }

    @Test
    void 读取头像_按扩展名返回媒体类型() throws Exception {
        Path dir = Files.createDirectories(tempDir.resolve("avatars"));
        Files.write(dir.resolve("2001.png"), new byte[] {4, 5, 6});

        ProfileService.AvatarFile avatar = service.loadAvatar("2001.png");

        assertThat(avatar.bytes()).containsExactly(4, 5, 6);
        assertThat(avatar.mediaType()).isEqualTo(MediaType.IMAGE_PNG);
    }

    @Test
    void 清空头像_置空并删除旧文件() throws Exception {
        Path dir = Files.createDirectories(tempDir.resolve("avatars"));
        Path oldFile = Files.write(dir.resolve("3001.png"), new byte[] {1});
        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setAvatar("/api/v1/profile/avatars/3001.png");
        when(userMapper.selectById(USER_ID)).thenReturn(user);
        when(userMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);

        service.clearAvatar();

        verify(userMapper).update(isNull(), any(UpdateWrapper.class));
        assertThat(Files.exists(oldFile)).isFalse();
    }
}
