package com.ceams.module.doc.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.entity.DocFileVersion;
import com.ceams.module.doc.enums.DocFileStatus;
import com.ceams.module.doc.enums.DocVersionSource;
import com.ceams.module.doc.enums.LibraryType;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.mapper.DocFileVersionMapper;
import com.ceams.module.doc.support.DocAccessResolver;
import com.ceams.module.doc.support.DocStorageService;
import com.ceams.module.doc.vo.DocFileVO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** 文档版本服务单元测试：回滚生成新版本、目标版本不存在。 */
@ExtendWith(MockitoExtension.class)
class DocVersionServiceImplTest {

    private static final Long USER_ID = 100L;

    private static final Long DIR_ID = 11L;

    private static final Long FILE_ID = 21L;

    private static final String TARGET_KEY = "doc/202601/1.docx";

    private static final String NEW_KEY = "doc/202611/2.docx";

    @Mock private DocFileMapper fileMapper;

    @Mock private DocDirectoryMapper dirMapper;

    @Mock private DocFileVersionMapper versionMapper;

    @Mock private DocAccessResolver accessResolver;

    @Mock private BpmSecurityGuard securityGuard;

    @Mock private BpmAuditService bpmAuditService;

    @Mock private DocStorageService storageService;

    @TempDir private Path tempDir;

    private DocVersionServiceImpl versionService;

    @BeforeEach
    void setUp() {
        versionService =
                new DocVersionServiceImpl(
                        fileMapper,
                        dirMapper,
                        versionMapper,
                        accessResolver,
                        securityGuard,
                        bpmAuditService,
                        storageService);
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
    void rollbackShouldCreateNewVersionWithRollbackSource() throws IOException {
        when(fileMapper.selectById(FILE_ID)).thenReturn(file());
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory());
        when(securityGuard.hasPermission("doc:file:edit")).thenReturn(true);
        when(versionMapper.selectOne(any())).thenReturn(targetVersion());
        Path targetFile = tempDir.resolve("v1.docx");
        Files.writeString(targetFile, "version-one", StandardCharsets.UTF_8);
        when(storageService.resolve(TARGET_KEY)).thenReturn(targetFile);
        when(storageService.write(any(), eq("docx"))).thenReturn(NEW_KEY);
        when(versionMapper.insert(any(DocFileVersion.class))).thenReturn(1);

        DocFileVO vo = versionService.rollback(FILE_ID, 1);

        ArgumentCaptor<DocFileVersion> captor = ArgumentCaptor.forClass(DocFileVersion.class);
        verify(versionMapper).insert(captor.capture());
        DocFileVersion created = captor.getValue();
        assertThat(created.getVersionNo()).isEqualTo(2);
        assertThat(created.getSource()).isEqualTo(DocVersionSource.ROLLBACK);
        assertThat(created.getStorageKey()).isEqualTo(NEW_KEY);
        assertThat(created.getChangeNote()).isEqualTo("回滚到 v1");
        assertThat(created.getUploadedBy()).isEqualTo(USER_ID);
        assertThat(vo.getCurrentVersionNo()).isEqualTo(2);
        verify(fileMapper).update(any(), any());
        verify(bpmAuditService)
                .record(anyString(), eq(FILE_ID), any(), any(), any(), any(), any());
    }

    @Test
    void rollbackShouldRejectMissingVersion() {
        when(fileMapper.selectById(FILE_ID)).thenReturn(file());
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory());
        when(securityGuard.hasPermission("doc:file:edit")).thenReturn(true);
        when(versionMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> versionService.rollback(FILE_ID, 9))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("目标版本不存在");
        verify(versionMapper, org.mockito.Mockito.never()).insert(any(DocFileVersion.class));
    }

    private DocFile file() {
        DocFile file = new DocFile();
        file.setId(FILE_ID);
        file.setDirId(DIR_ID);
        file.setName("报告.docx");
        file.setExt("docx");
        file.setSize(100L);
        file.setChecksum("a".repeat(64));
        file.setStorageKey(TARGET_KEY);
        file.setCurrentVersionNo(1);
        file.setStatus(DocFileStatus.NORMAL);
        file.setUploadedBy(USER_ID);
        file.setDownloadCount(0);
        return file;
    }

    private DocFileVersion targetVersion() {
        DocFileVersion version = new DocFileVersion();
        version.setFileId(FILE_ID);
        version.setVersionNo(1);
        version.setStorageKey(TARGET_KEY);
        version.setSize(100L);
        version.setChecksum("b".repeat(64));
        version.setUploadedBy(USER_ID);
        version.setSource(DocVersionSource.UPLOAD);
        return version;
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
