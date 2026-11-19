package com.ceams.module.doc.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.doc.dto.FileRenameRequest;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.enums.DocFileStatus;
import com.ceams.module.doc.enums.LibraryType;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.service.DocLockService;
import com.ceams.module.doc.support.DocAccessResolver;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** 文档文件服务单元测试：非法文件名、同目录重名、删除审计。 */
@ExtendWith(MockitoExtension.class)
class DocFileServiceImplTest {

    private static final Long USER_ID = 100L;

    private static final Long DIR_ID = 11L;

    private static final Long FILE_ID = 21L;

    @Mock private DocFileMapper fileMapper;

    @Mock private DocDirectoryMapper dirMapper;

    @Mock private DocAccessResolver accessResolver;

    @Mock private BpmSecurityGuard securityGuard;

    @Mock private BpmAuditService bpmAuditService;

    @Mock private DocLockService docLockService;

    private DocFileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        fileService =
                new DocFileServiceImpl(
                        fileMapper,
                        dirMapper,
                        accessResolver,
                        securityGuard,
                        bpmAuditService,
                        docLockService);
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
    void renameShouldRejectIllegalFileName() {
        when(fileMapper.selectById(FILE_ID)).thenReturn(file("报表.xlsx"));
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory());
        when(securityGuard.hasPermission(anyString())).thenReturn(true);

        FileRenameRequest request = new FileRenameRequest();
        request.setName("bad/name.xlsx");

        assertThatThrownBy(() -> fileService.rename(FILE_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法字符");
    }

    @Test
    void renameShouldRejectDuplicateNameInSameDirectory() {
        when(fileMapper.selectById(FILE_ID)).thenReturn(file("报表.xlsx"));
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory());
        when(securityGuard.hasPermission(anyString())).thenReturn(true);
        when(fileMapper.selectCount(any())).thenReturn(1L);

        FileRenameRequest request = new FileRenameRequest();
        request.setName("汇总.xlsx");

        assertThatThrownBy(() -> fileService.rename(FILE_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("同名文件");
    }

    @Test
    void deleteShouldWriteDocDeleteAudit() {
        when(fileMapper.selectById(FILE_ID)).thenReturn(file("报表.xlsx"));
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory());
        when(securityGuard.hasPermission(anyString())).thenReturn(true);

        fileService.delete(FILE_ID);

        verify(fileMapper).deleteById(FILE_ID);
        ArgumentCaptor<AuditAction> actionCaptor = ArgumentCaptor.forClass(AuditAction.class);
        verify(bpmAuditService, times(1))
                .record(
                        eq("DOC_FILE"),
                        eq(FILE_ID),
                        actionCaptor.capture(),
                        any(),
                        isNull(),
                        isNull(),
                        any());
        assertThat(actionCaptor.getValue()).isEqualTo(AuditAction.DOC_DELETE);
    }

    @Test
    void deleteShouldReleaseEditLockBeforeSoftDelete() {
        DocFile locked = file("报表.xlsx");
        locked.setStatus(DocFileStatus.LOCKED);
        locked.setLockUserId(USER_ID);
        when(fileMapper.selectById(FILE_ID)).thenReturn(locked);
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory());
        when(securityGuard.hasPermission(anyString())).thenReturn(true);

        fileService.delete(FILE_ID);

        // 删除已加锁文件：先释放编辑锁（幂等清理 Redis key + DB 锁定标记），再软删除并写审计
        verify(docLockService, times(1)).forceUnlock(FILE_ID);
        verify(fileMapper, times(1)).deleteById(FILE_ID);
        verify(bpmAuditService, times(1))
                .record(
                        eq("DOC_FILE"),
                        eq(FILE_ID),
                        eq(AuditAction.DOC_DELETE),
                        any(),
                        isNull(),
                        isNull(),
                        any());
    }

    private DocFile file(String name) {
        DocFile file = new DocFile();
        file.setId(FILE_ID);
        file.setDirId(DIR_ID);
        file.setName(name);
        file.setExt("xlsx");
        file.setSize(1024L);
        file.setCurrentVersionNo(1);
        file.setStatus(DocFileStatus.NORMAL);
        file.setUploadedBy(USER_ID);
        file.setDownloadCount(0);
        return file;
    }

    private DocDirectory directory() {
        DocDirectory dir = new DocDirectory();
        dir.setId(DIR_ID);
        dir.setName("P2-Docs");
        dir.setLibraryType(LibraryType.PERSONAL);
        dir.setOwnerUserId(USER_ID);
        dir.setPath("/11/");
        return dir;
    }
}
