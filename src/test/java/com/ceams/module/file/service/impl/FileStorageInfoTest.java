package com.ceams.module.file.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ceams.module.file.vo.DirectoryStatVO;
import com.ceams.module.file.vo.FileStorageInfoVO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link FileServiceImpl#storageInfo()} 只读存储信息单元测试。
 *
 * <p>不启动 Spring 上下文：临时目录通过 {@link ReflectionTestUtils} 注入，避免依赖 @Value 解析。
 */
class FileStorageInfoTest {

    @TempDir private Path tempDir;

    /** 被测服务。 */
    private FileServiceImpl service;

    /** 临时上传根目录：{@code {tempDir}/files}。 */
    private Path uploadRoot;

    @BeforeEach
    void setUp() {
        service = new FileServiceImpl(mock(JdbcTemplate.class));
        uploadRoot = tempDir.resolve("files");
        ReflectionTestUtils.setField(service, "uploadDir", uploadRoot.toString());
    }

    @Test
    void shouldReportStatisticsWhenDirectoryExists() throws IOException {
        Files.createDirectories(uploadRoot);
        // 根目录直接文件：10 + 30 = 40 字节，计入「业务附件」
        Files.write(uploadRoot.resolve("20260101.png"), new byte[10]);
        Files.write(uploadRoot.resolve("20260102.pdf"), new byte[30]);
        // 一级子目录 avatars：5 字节
        Path avatars = Files.createDirectories(uploadRoot.resolve("avatars"));
        Files.write(avatars.resolve("1001.png"), new byte[5]);
        // 一级子目录 doc：嵌套两层共 100 字节，验证子目录统计是递归的
        Path docMonth = Files.createDirectories(uploadRoot.resolve("doc").resolve("202611"));
        Files.write(docMonth.resolve("v1.docx"), new byte[60]);
        Files.write(docMonth.resolve("v2.docx"), new byte[40]);
        // 一级子目录 tmp：空目录
        Files.createDirectories(uploadRoot.resolve("tmp"));

        FileStorageInfoVO info = service.storageInfo();

        assertThat(info.getConfiguredPath()).isEqualTo(uploadRoot.toString());
        assertThat(info.getAbsolutePath())
                .isEqualTo(uploadRoot.toAbsolutePath().normalize().toString());
        assertThat(info.isExists()).isTrue();
        assertThat(info.isWritable()).isTrue();
        // 递归统计：2 + 1 + 2 = 5 个常规文件，40 + 5 + 100 = 145 字节
        assertThat(info.getFileCount()).isEqualTo(5);
        assertThat(info.getTotalSizeBytes()).isEqualTo(145L);
        assertThat(info.getDiskTotalBytes()).isNotNull();
        assertThat(info.getDiskUsableBytes()).isNotNull();

        assertThat(info.getDirectories()).extracting(DirectoryStatVO::getName)
                .containsExactly("doc", "业务附件", "avatars", "tmp");
        assertThat(info.getDirectories()).extracting(DirectoryStatVO::getSizeBytes)
                .containsExactly(100L, 40L, 5L, 0L);
        assertThat(info.getDirectories()).extracting(DirectoryStatVO::getFileCount)
                .containsExactly(2L, 2L, 1L, 0L);
    }

    @Test
    void shouldReportZeroWhenDirectoryMissing() {
        // 不创建 uploadRoot，直接调用
        FileStorageInfoVO info = service.storageInfo();

        assertThat(info.getConfiguredPath()).isEqualTo(uploadRoot.toString());
        assertThat(info.getAbsolutePath())
                .isEqualTo(uploadRoot.toAbsolutePath().normalize().toString());
        assertThat(info.isExists()).isFalse();
        assertThat(info.isWritable()).isFalse();
        assertThat(info.getFileCount()).isZero();
        assertThat(info.getTotalSizeBytes()).isZero();
        assertThat(info.getDirectories()).isEmpty();
    }
}
