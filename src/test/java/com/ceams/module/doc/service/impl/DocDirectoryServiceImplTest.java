package com.ceams.module.doc.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.doc.dto.DirectoryCreateRequest;
import com.ceams.module.doc.dto.DirectoryMoveRequest;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.enums.LibraryType;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.support.DocAccessResolver;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** 文档目录服务单元测试：同级重名、非空删除、移动到子树。 */
@ExtendWith(MockitoExtension.class)
class DocDirectoryServiceImplTest {

    private static final Long USER_ID = 100L;

    private static final Long DIR_ID = 11L;

    private static final Long SUB_DIR_ID = 12L;

    @Mock private DocDirectoryMapper dirMapper;

    @Mock private DocFileMapper fileMapper;

    @Mock private DocAccessResolver accessResolver;

    @Mock private BpmSecurityGuard securityGuard;

    private DocDirectoryServiceImpl directoryService;

    @BeforeEach
    void setUp() {
        directoryService =
                new DocDirectoryServiceImpl(dirMapper, fileMapper, accessResolver, securityGuard);
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
    void createShouldRejectDuplicateNameInSameParent() {
        DirectoryCreateRequest request = new DirectoryCreateRequest();
        request.setName("P2-Docs");
        request.setLibraryType(LibraryType.PERSONAL);
        when(dirMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> directoryService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("同名目录");
    }

    @Test
    void deleteShouldRejectNonEmptyDirectory() {
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory(DIR_ID, "/11/", null));
        when(securityGuard.hasPermission(anyString())).thenReturn(true);
        when(dirMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> directoryService.delete(DIR_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("先清空");
    }

    @Test
    void moveShouldRejectTargetInsideSubtree() {
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory(DIR_ID, "/11/", null));
        when(dirMapper.selectById(SUB_DIR_ID))
                .thenReturn(directory(SUB_DIR_ID, "/11/12/", DIR_ID));
        when(securityGuard.hasPermission(anyString())).thenReturn(true);

        DirectoryMoveRequest request = new DirectoryMoveRequest();
        request.setParentId(SUB_DIR_ID);

        assertThatThrownBy(() -> directoryService.move(DIR_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自身或子目录");
    }

    private DocDirectory directory(Long id, String path, Long parentId) {
        DocDirectory dir = new DocDirectory();
        dir.setId(id);
        dir.setName("目录" + id);
        dir.setParentId(parentId);
        dir.setLibraryType(LibraryType.PERSONAL);
        dir.setOwnerUserId(USER_ID);
        dir.setPath(path);
        dir.setSortOrder(0);
        return dir;
    }
}
