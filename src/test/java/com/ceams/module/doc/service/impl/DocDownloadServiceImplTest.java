package com.ceams.module.doc.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.doc.config.DocProperties;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.enums.DocFileStatus;
import com.ceams.module.doc.enums.LibraryType;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.support.DocAccessResolver;
import com.ceams.module.doc.support.DocStorageService;
import com.ceams.module.doc.support.DocTokenSupport;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** 文档下载与预览单元测试：token 二次使用被拒、Office 预览拒绝。 */
@ExtendWith(MockitoExtension.class)
class DocDownloadServiceImplTest {

    private static final Long FILE_ID = 21L;

    private static final Long DIR_ID = 11L;

    private static final String STORAGE_KEY = "doc/202601/1.pdf";

    private static final String TOKEN = "download-token";

    @Mock private DocFileMapper fileMapper;

    @Mock private DocDirectoryMapper dirMapper;

    @Mock private DocAccessResolver accessResolver;

    @Mock private DocStorageService storageService;

    @Mock private DocTokenSupport tokenSupport;

    @Mock private StringRedisTemplate stringRedisTemplate;

    @Mock private ValueOperations<String, String> valueOperations;

    @TempDir private Path tempDir;

    private DocDownloadServiceImpl downloadService;

    @BeforeEach
    void setUp() {
        downloadService =
                new DocDownloadServiceImpl(
                        fileMapper,
                        dirMapper,
                        accessResolver,
                        storageService,
                        tokenSupport,
                        stringRedisTemplate,
                        new DocProperties());
    }

    @Test
    void downloadShouldRejectUsedToken() throws IOException {
        when(fileMapper.selectById(FILE_ID)).thenReturn(file("pdf"));
        Claims claims = mock(Claims.class);
        when(claims.getId()).thenReturn("jti-1");
        when(tokenSupport.parse(TOKEN)).thenReturn(claims);
        when(tokenSupport.isDocToken(claims)).thenReturn(true);
        when(tokenSupport.hasScope(claims, DocTokenSupport.SCOPE_DOWNLOAD)).thenReturn(true);
        when(tokenSupport.fileIdOf(claims)).thenReturn(FILE_ID);
        Path physical = tempDir.resolve("v1.pdf");
        Files.writeString(physical, "pdf-content", StandardCharsets.UTF_8);
        when(storageService.resolve(STORAGE_KEY)).thenReturn(physical);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                        eq("doc:token:used:jti-1"), eq("1"), any(Duration.class)))
                .thenReturn(false);

        HttpServletResponse response = mock(HttpServletResponse.class);

        assertThatThrownBy(() -> downloadService.download(FILE_ID, TOKEN, response))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("下载链接已使用")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
    }

    @Test
    void previewShouldRejectOfficeDocument() {
        when(fileMapper.selectById(FILE_ID)).thenReturn(file("docx"));
        when(dirMapper.selectById(DIR_ID)).thenReturn(directory());

        HttpServletResponse response = mock(HttpServletResponse.class);

        assertThatThrownBy(() -> downloadService.preview(FILE_ID, response))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("在线编辑")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
    }

    private DocFile file(String ext) {
        DocFile file = new DocFile();
        file.setId(FILE_ID);
        file.setDirId(DIR_ID);
        file.setName("报告." + ext);
        file.setExt(ext);
        file.setSize(1024L);
        file.setStorageKey("pdf".equals(ext) ? STORAGE_KEY : "doc/202601/1.docx");
        file.setChecksum("a".repeat(64));
        file.setCurrentVersionNo(1);
        file.setStatus(DocFileStatus.NORMAL);
        file.setUploadedBy(100L);
        file.setDownloadCount(0);
        return file;
    }

    private DocDirectory directory() {
        DocDirectory dir = new DocDirectory();
        dir.setId(DIR_ID);
        dir.setName("P3-Upload");
        dir.setLibraryType(LibraryType.PERSONAL);
        dir.setOwnerUserId(100L);
        dir.setPath("/11/");
        return dir;
    }
}
