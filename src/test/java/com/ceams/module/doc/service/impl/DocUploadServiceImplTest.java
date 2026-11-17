package com.ceams.module.doc.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.doc.config.DocProperties;
import com.ceams.module.doc.dto.UploadInitRequest;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.entity.DocFileVersion;
import com.ceams.module.doc.enums.LibraryType;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.mapper.DocFileVersionMapper;
import com.ceams.module.doc.support.DocAccessResolver;
import com.ceams.module.doc.support.DocStorageService;
import com.ceams.module.doc.vo.UploadInitVO;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** 文档分片上传服务单元测试：秒传、超限、同名、分片缺失、checksum 不符。 */
@ExtendWith(MockitoExtension.class)
class DocUploadServiceImplTest {

    private static final Long USER_ID = 100L;

    private static final Long DIR_ID = 11L;

    private static final Long FILE_ID = 21L;

    private static final String UPLOAD_ID = "up-0001";

    private static final String META_KEY = "doc:upload:" + UPLOAD_ID;

    @Mock private DocDirectoryMapper dirMapper;

    @Mock private DocFileMapper fileMapper;

    @Mock private DocFileVersionMapper versionMapper;

    @Mock private DocAccessResolver accessResolver;

    @Mock private DocStorageService storageService;

    @Mock private StringRedisTemplate stringRedisTemplate;

    @Mock private HashOperations<String, Object, Object> hashOperations;

    @Mock private BpmAuditService bpmAuditService;

    private DocProperties docProperties;

    private DocUploadServiceImpl uploadService;

    @BeforeEach
    void setUp() {
        docProperties = new DocProperties();
        uploadService =
                new DocUploadServiceImpl(
                        dirMapper,
                        fileMapper,
                        versionMapper,
                        accessResolver,
                        storageService,
                        stringRedisTemplate,
                        bpmAuditService,
                        docProperties);
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
    void initShouldReturnInstantWhenChecksumHit() {
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory());
        when(fileMapper.selectCount(any())).thenReturn(0L);
        DocFile existing = new DocFile();
        existing.setId(999L);
        existing.setStorageKey("doc/202601/999.docx");
        existing.setChecksum("a".repeat(64));
        when(fileMapper.selectOne(any())).thenReturn(existing);

        UploadInitVO vo = uploadService.init(request("a".repeat(64), 1024L));

        assertThat(vo.isInstant()).isTrue();
        assertThat(vo.getFileName()).isEqualTo("报告.docx");
        assertThat(vo.getChunkSize()).isEqualTo(5242880);
        verify(fileMapper).insert(any(DocFile.class));
        verify(versionMapper).insert(any(DocFileVersion.class));
        // 秒传不落盘
        verify(storageService, never()).saveChunk(anyString(), eq(0), any());
        verify(storageService, never()).merge(anyString(), eq(0), anyString());
    }

    @Test
    void initShouldRejectOversizeFile() {
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory());

        UploadInitRequest request = request(null, docProperties.getMaxFileSize() + 1);

        assertThatThrownBy(() -> uploadService.init(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件大小不能超过");
    }

    @Test
    void initShouldRejectDuplicateName() {
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory());
        when(fileMapper.selectCount(any())).thenReturn(1L);

        UploadInitRequest request = request(null, 1024L);

        assertThatThrownBy(() -> uploadService.init(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("同名文件已存在");
        verify(fileMapper, never()).insert(any(DocFile.class));
    }

    @Test
    void completeShouldRejectMissingChunks() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(META_KEY)).thenReturn(meta(3, 10L, null));
        when(storageService.listUploadedChunks(UPLOAD_ID)).thenReturn(List.of(0));

        assertThatThrownBy(() -> uploadService.complete(UPLOAD_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分片缺失");
        verify(storageService, never()).merge(anyString(), eq(3), any());
        verify(fileMapper, never()).insert(any(DocFile.class));
    }

    @Test
    void completeShouldRejectChecksumMismatch() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(META_KEY)).thenReturn(meta(1, 10L, "b".repeat(64)));
        when(storageService.listUploadedChunks(UPLOAD_ID)).thenReturn(List.of(0));
        when(storageService.merge(UPLOAD_ID, 1, "docx")).thenReturn("doc/202611/1.docx");
        when(storageService.resolve("doc/202611/1.docx")).thenReturn(Path.of("1.docx"));
        when(storageService.sha256(Path.of("1.docx"))).thenReturn("c".repeat(64));

        assertThatThrownBy(() -> uploadService.complete(UPLOAD_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SHA-256 不一致");
        verify(storageService).deleteQuietly("doc/202611/1.docx");
        verify(stringRedisTemplate, times(1)).delete(META_KEY);
        verify(fileMapper, never()).insert(any(DocFile.class));
        // 确保审计未被写入（未成功落库）
        verify(bpmAuditService, never()).record(anyString(), any(), any(), any(), any(), any(), any());
    }

    /** 上传批次 meta（Redis hash）。 */
    private Map<Object, Object> meta(int totalChunks, long size, String checksum) {
        Map<Object, Object> meta = new HashMap<>();
        meta.put("dirId", String.valueOf(DIR_ID));
        meta.put("fileName", "报告.docx");
        meta.put("size", String.valueOf(size));
        meta.put("checksum", checksum == null ? "" : checksum);
        meta.put("totalChunks", String.valueOf(totalChunks));
        meta.put("userId", String.valueOf(USER_ID));
        return meta;
    }

    private UploadInitRequest request(String checksum, long size) {
        UploadInitRequest request = new UploadInitRequest();
        request.setDirId(DIR_ID);
        request.setFileName("报告.docx");
        request.setSize(size);
        request.setChecksum(checksum);
        return request;
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
