package com.ceams.module.doc.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.doc.config.OnlyOfficeProperties;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.entity.DocFileVersion;
import com.ceams.module.doc.enums.DocAction;
import com.ceams.module.doc.enums.DocVersionSource;
import com.ceams.module.doc.enums.LibraryType;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.mapper.DocFileVersionMapper;
import com.ceams.module.doc.service.DocIndexService;
import com.ceams.module.doc.service.DocLockService;
import com.ceams.module.doc.support.DocAccessResolver;
import com.ceams.module.doc.support.DocStorageService;
import com.ceams.module.doc.support.DocTokenSupport;
import com.ceams.module.doc.support.OnlyOfficeJwtSupport;
import com.ceams.module.doc.vo.DocLockVO;
import com.ceams.module.doc.vo.EditorConfigVO;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

/**
 * OnlyOffice 服务单元测试：view/edit 模式、未持锁 409、降级 503、配置返回、非法 JWT、
 * 同 checksum 跳过、新版本生成、非保存状态忽略。
 */
@ExtendWith(MockitoExtension.class)
class OnlyOfficeServiceImplTest {

    private static final Long USER_ID = 100L;

    private static final Long DIR_ID = 11L;

    private static final Long FILE_ID = 21L;

    private static final String HEALTH_URL = "http://onlyoffice/healthcheck";

    private static final String CALLBACK_URL = "http://onlyoffice/callback-file.docx";

    @Mock private DocFileMapper fileMapper;

    @Mock private DocDirectoryMapper dirMapper;

    @Mock private DocFileVersionMapper versionMapper;

    @Mock private DocIndexService docIndexService;

    @Mock private DocAccessResolver accessResolver;

    @Mock private DocLockService lockService;

    @Mock private DocStorageService storageService;

    @Mock private DocTokenSupport tokenSupport;

    @Mock private OnlyOfficeJwtSupport jwtSupport;

    @Mock private RestTemplate restTemplate;

    @Mock private StringRedisTemplate stringRedisTemplate;

    @Mock private ValueOperations<String, String> valueOperations;

    @Mock private BpmAuditService bpmAuditService;

    @Mock private UserMapper userMapper;

    private OnlyOfficeProperties properties;

    private OnlyOfficeServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new OnlyOfficeProperties();
        properties.setEnabled(true);
        properties.setDocumentServerUrl("http://onlyoffice");
        properties.setPublicBaseUrl("http://app:9095");
        properties.setJwtSecret("ceams-dev-secret");
        service =
                new OnlyOfficeServiceImpl(
                        fileMapper,
                        dirMapper,
                        versionMapper,
                        docIndexService,
                        accessResolver,
                        lockService,
                        storageService,
                        tokenSupport,
                        jwtSupport,
                        properties,
                        restTemplate,
                        stringRedisTemplate,
                        bpmAuditService,
                        userMapper,
                        new ObjectMapper());
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
    void buildEditorConfigShouldUseViewModeWithoutEditPermission() {
        mockHealthy();
        mockFileAndDir();
        when(accessResolver.resolve(eq(USER_ID), any(DocDirectory.class), any(DocFile.class)))
                .thenReturn(EnumSet.of(DocAction.PREVIEW, DocAction.DOWNLOAD));
        when(userMapper.selectById(USER_ID)).thenReturn(user());
        when(tokenSupport.signOnlyOfficeFile(FILE_ID, 1)).thenReturn("file-token");
        when(jwtSupport.signConfig(anyMap())).thenReturn("config-token");

        EditorConfigVO vo = service.buildEditorConfig(FILE_ID);

        assertThat(vo.getMode()).isEqualTo("view");
        Map<?, ?> permissions = permissionsOf(vo.getConfig());
        assertThat(permissions.get("edit")).isEqualTo(false);
        assertThat(vo.getToken()).isEqualTo("config-token");
    }

    @Test
    void buildEditorConfigShouldRejectEditWithoutLock() {
        mockHealthy();
        mockFileAndDir();
        when(accessResolver.resolve(eq(USER_ID), any(DocDirectory.class), any(DocFile.class)))
                .thenReturn(EnumSet.allOf(DocAction.class));
        DocLockVO lock = new DocLockVO();
        lock.setLocked(false);
        when(lockService.status(FILE_ID)).thenReturn(lock);

        assertThatThrownBy(() -> service.buildEditorConfig(FILE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请先获取编辑锁")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
    }

    @Test
    void buildEditorConfigShouldDegradeWhenDisabled() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> service.buildEditorConfig(FILE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("在线编辑暂不可用")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SYSTEM_SERVICE_UNAVAILABLE);
        verifyNoInteractions(restTemplate, fileMapper);
    }

    @Test
    void buildEditorConfigShouldReturnDocumentServerUrlKeyAndToken() {
        mockHealthy();
        mockFileAndDir();
        when(accessResolver.resolve(eq(USER_ID), any(DocDirectory.class), any(DocFile.class)))
                .thenReturn(EnumSet.allOf(DocAction.class));
        DocLockVO lock = new DocLockVO();
        lock.setLocked(true);
        lock.setOwnerId(USER_ID);
        when(lockService.status(FILE_ID)).thenReturn(lock);
        when(userMapper.selectById(USER_ID)).thenReturn(user());
        when(tokenSupport.signOnlyOfficeFile(FILE_ID, 1)).thenReturn("file-token");
        when(jwtSupport.signConfig(anyMap())).thenReturn("config-token");

        EditorConfigVO vo = service.buildEditorConfig(FILE_ID);

        assertThat(vo.getDocumentServerUrl()).isEqualTo("http://onlyoffice");
        assertThat(vo.getMode()).isEqualTo("edit");
        assertThat(vo.getFileId()).isEqualTo(FILE_ID);
        assertThat(vo.getToken()).isEqualTo("config-token");
        assertThat(documentOf(vo.getConfig()).get("key")).isEqualTo(FILE_ID + "_1");
        assertThat(documentOf(vo.getConfig()).get("url"))
                .asString()
                .startsWith("http://app:9095/api/v1/doc/onlyoffice/file?token=");
        assertThat(vo.getConfig().get("editorConfig")).isInstanceOf(Map.class);
        Map<?, ?> editorConfig = (Map<?, ?>) vo.getConfig().get("editorConfig");
        assertThat(editorConfig.get("callbackUrl"))
                .isEqualTo("http://app:9095/api/v1/doc/onlyoffice/callback");
        assertThat(editorConfig.get("lang")).isEqualTo("zh-CN");
    }

    @Test
    void callbackShouldRejectInvalidJwt() {
        when(jwtSupport.verify("bad-token"))
                .thenThrow(BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID, "bad"));

        Map<String, Integer> result =
                service.callback("Bearer bad-token", callbackBody(2, CALLBACK_URL));

        assertThat(result).containsEntry("error", 1);
        verifyNoInteractions(stringRedisTemplate, storageService, versionMapper);
    }

    @Test
    void callbackShouldOnlyUnlockWhenChecksumUnchanged() throws Exception {
        byte[] content = "same-content".getBytes(StandardCharsets.UTF_8);
        String checksum = sha256Hex(content);
        DocFile file = file();
        file.setChecksum(checksum);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(fileMapper.selectById(FILE_ID)).thenReturn(file);
        OnlyOfficeServiceImpl spyService = spy(service);
        doReturn(content).when(spyService).download(anyString());

        Map<String, Integer> result =
                spyService.callback("Bearer good-token", callbackBody(2, CALLBACK_URL));

        assertThat(result).containsEntry("error", 0);
        verify(versionMapper, never()).insert(any(DocFileVersion.class));
        verify(fileMapper, never()).update(isNull(), any());
        verify(lockService).forceUnlock(FILE_ID);
    }

    @Test
    void callbackShouldCreateNewVersionWhenChecksumChanged() throws Exception {
        byte[] content = "new-content".getBytes(StandardCharsets.UTF_8);
        DocFile file = file();
        file.setChecksum("old-checksum");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(fileMapper.selectById(FILE_ID)).thenReturn(file);
        when(storageService.write(content, "docx")).thenReturn("doc/202611/new.docx");
        OnlyOfficeServiceImpl spyService = spy(service);
        doReturn(content).when(spyService).download(anyString());

        Map<String, Integer> result =
                spyService.callback("Bearer good-token", callbackBody(2, CALLBACK_URL));

        assertThat(result).containsEntry("error", 0);
        ArgumentCaptor<DocFileVersion> captor = ArgumentCaptor.forClass(DocFileVersion.class);
        verify(versionMapper).insert(captor.capture());
        DocFileVersion version = captor.getValue();
        assertThat(version.getVersionNo()).isEqualTo(2);
        assertThat(version.getSource()).isEqualTo(DocVersionSource.ONLINE_EDIT);
        assertThat(version.getChangeNote()).isEqualTo("在线编辑保存");
        assertThat(version.getChecksum()).isEqualTo(sha256Hex(content));
        verify(fileMapper).update(isNull(), any());
        verify(lockService).forceUnlock(FILE_ID);
        verify(bpmAuditService)
                .record(
                        eq("DOC_FILE"),
                        eq(FILE_ID),
                        eq(AuditAction.DOC_EDIT),
                        any(),
                        any(),
                        isNull(),
                        eq(USER_ID));
    }

    @Test
    void callbackShouldIgnoreNonSaveStatus() {
        Map<String, Integer> result =
                service.callback("Bearer good-token", callbackBody(1, CALLBACK_URL));

        assertThat(result).containsEntry("error", 0);
        verifyNoInteractions(stringRedisTemplate, storageService, versionMapper, fileMapper);
    }

    // ------------------------------------------------------------------
    // 测试辅助
    // ------------------------------------------------------------------

    private void mockHealthy() {
        when(restTemplate.getForObject(HEALTH_URL, String.class)).thenReturn("true");
    }

    private void mockFileAndDir() {
        when(fileMapper.selectById(FILE_ID)).thenReturn(file());
        when(dirMapper.selectById(DIR_ID)).thenReturn(dir());
    }

    private String callbackBody(int status, String url) {
        return "{\"key\":\""
                + FILE_ID
                + "_1\",\"status\":"
                + status
                + ",\"url\":\""
                + url
                + "\",\"users\":[\"1\"]}";
    }

    private DocFile file() {
        DocFile file = new DocFile();
        file.setId(FILE_ID);
        file.setDirId(DIR_ID);
        file.setName("报告.docx");
        file.setExt("docx");
        file.setSize(1024L);
        file.setChecksum("checksum");
        file.setCurrentVersionNo(1);
        file.setUploadedBy(USER_ID);
        return file;
    }

    private DocDirectory dir() {
        DocDirectory dir = new DocDirectory();
        dir.setId(DIR_ID);
        dir.setName("P4-OnlyOffice");
        dir.setLibraryType(LibraryType.PERSONAL);
        dir.setOwnerUserId(USER_ID);
        dir.setPath("/11/");
        return dir;
    }

    private SysUser user() {
        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setName("张三");
        return user;
    }

    private Map<?, ?> documentOf(Map<String, Object> config) {
        return (Map<?, ?>) config.get("document");
    }

    private Map<?, ?> permissionsOf(Map<String, Object> config) {
        return (Map<?, ?>) documentOf(config).get("permissions");
    }

    private String sha256Hex(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }
}
