package com.ceams.module.doc.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.ceams.common.exception.BusinessException;
import com.ceams.module.doc.mapper.DocFileMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 文档存储服务单元测试：分片合并顺序与清理、越界路径拒绝。 */
class DocStorageServiceTest {

    private static final String UPLOAD_ID = "up-merge-1";

    @TempDir private Path tempDir;

    private DocStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new DocStorageService(mock(DocFileMapper.class));
        storageService.setUploadDir(tempDir.toString());
    }

    @Test
    void mergeShouldConcatPartsInOrderAndCleanup() throws IOException {
        writePart(0, "AAA");
        writePart(1, "BBB");
        writePart(2, "CCC");

        String key = storageService.merge(UPLOAD_ID, 3, "txt");

        assertThat(key).startsWith("doc/").endsWith(".txt");
        Path merged = storageService.resolve(key);
        assertThat(Files.readString(merged, StandardCharsets.UTF_8)).isEqualTo("AAABBBCCC");
        // 合并后临时目录已清理
        assertThat(Files.exists(storageService.chunkDir(UPLOAD_ID))).isFalse();
    }

    @Test
    void resolveShouldRejectTraversal() {
        assertThatThrownBy(() -> storageService.resolve("../evil.txt"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法存储键");
        assertThatThrownBy(() -> storageService.resolve("/etc/passwd"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法存储键");
    }

    /** 写入指定序号的测试分片。 */
    private void writePart(int index, String content) throws IOException {
        Path part = storageService.partPath(UPLOAD_ID, index);
        Files.createDirectories(part.getParent());
        Files.writeString(part, content, StandardCharsets.UTF_8);
    }
}
