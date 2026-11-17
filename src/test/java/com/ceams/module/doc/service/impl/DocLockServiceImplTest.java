package com.ceams.module.doc.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.doc.config.DocProperties;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.enums.DocFileStatus;
import com.ceams.module.doc.enums.LibraryType;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.support.DocAccessResolver;
import com.ceams.module.doc.vo.DocLockVO;
import com.ceams.module.iam.mapper.UserMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** 文档编辑锁单元测试：首次加锁、他人持锁 409、他人释放 409、Redis 无 key 时校准。 */
@ExtendWith(MockitoExtension.class)
class DocLockServiceImplTest {

    private static final Long USER_ID = 100L;

    private static final Long OTHER_USER_ID = 200L;

    private static final Long DIR_ID = 11L;

    private static final Long FILE_ID = 21L;

    private static final String LOCK_KEY = "doc:lock:" + FILE_ID;

    @Mock private StringRedisTemplate stringRedisTemplate;

    @Mock private ValueOperations<String, String> valueOperations;

    @Mock private DocFileMapper fileMapper;

    @Mock private DocDirectoryMapper dirMapper;

    @Mock private DocAccessResolver accessResolver;

    @Mock private BpmSecurityGuard securityGuard;

    @Mock private UserMapper userMapper;

    private DocLockServiceImpl lockService;

    @BeforeEach
    void setUp() {
        lockService =
                new DocLockServiceImpl(
                        stringRedisTemplate,
                        fileMapper,
                        dirMapper,
                        accessResolver,
                        securityGuard,
                        userMapper,
                        new DocProperties());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_STAFF"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void lockShouldSucceedForFirstUser() {
        when(fileMapper.selectById(FILE_ID)).thenReturn(file(DocFileStatus.NORMAL));
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory());
        when(securityGuard.hasPermission("doc:file:edit")).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);

        lockService.lock(FILE_ID);

        verify(stringRedisTemplate).opsForValue();
        verify(fileMapper).update(isNull(), any());
    }

    @Test
    void lockShouldRejectWhenHeldByOther() {
        when(fileMapper.selectById(FILE_ID)).thenReturn(file(DocFileStatus.LOCKED));
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory());
        when(securityGuard.hasPermission("doc:file:edit")).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(false);
        when(valueOperations.get(LOCK_KEY)).thenReturn(String.valueOf(OTHER_USER_ID));

        assertThatThrownBy(() -> lockService.lock(FILE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件正在被他人编辑")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
    }

    @Test
    void unlockShouldRejectWhenHeldByOther() {
        when(fileMapper.selectById(FILE_ID)).thenReturn(file(DocFileStatus.LOCKED));
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory());
        when(securityGuard.hasPermission("doc:file:edit")).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOCK_KEY)).thenReturn(String.valueOf(OTHER_USER_ID));

        assertThatThrownBy(() -> lockService.unlock(FILE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件正在被他人编辑");
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    void statusShouldCalibrateToUnlockedWhenRedisKeyMissing() {
        DocFile file = file(DocFileStatus.LOCKED);
        file.setLockUserId(OTHER_USER_ID);
        when(fileMapper.selectById(FILE_ID)).thenReturn(file);
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(LOCK_KEY)).thenReturn(null);

        DocLockVO vo = lockService.status(FILE_ID);

        assertThat(vo.isLocked()).isFalse();
        assertThat(vo.getOwnerId()).isNull();
        // 数据库锁标记已回写校准
        verify(fileMapper).update(isNull(), any());
        verify(userMapper, never()).selectById(any());
    }

    private DocFile file(DocFileStatus status) {
        DocFile file = new DocFile();
        file.setId(FILE_ID);
        file.setDirId(DIR_ID);
        file.setName("报告.docx");
        file.setExt("docx");
        file.setSize(1024L);
        file.setCurrentVersionNo(1);
        file.setStatus(status);
        file.setUploadedBy(USER_ID);
        file.setDownloadCount(0);
        return file;
    }

    private DocDirectory directory() {
        DocDirectory dir = new DocDirectory();
        dir.setId(DIR_ID);
        dir.setName("P3-Upload");
        dir.setLibraryType(LibraryType.PERSONAL);
        dir.setOwnerUserId(USER_ID);
        dir.setPath("/11/");
        return dir;
    }
}
