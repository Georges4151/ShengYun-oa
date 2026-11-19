package com.ceams.module.file.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 文件存储信息视图对象（file 模块只读出参，供系统管理员在 Web 端查看上传目录位置与占用情况）。
 *
 * <p>字段均为只读出参，不参与持久化；字节数统一使用 {@link Long}，磁盘空间探测失败时为 {@code null}。
 */
@Data
public class FileStorageInfoVO {

    /** 配置中的原始路径（如 {@code ./data/files}，与 {@code ceams.file.upload-dir} 一致）。 */
    private String configuredPath;

    /** 解析后的绝对路径（{@code Path.toAbsolutePath().normalize().toString()}）。 */
    private String absolutePath;

    /** 目录是否存在（存在且为目录才为 true）。 */
    private boolean exists;

    /** 目录是否存在且可写（{@code Files.isWritable}）。 */
    private boolean writable;

    /** 递归统计的常规文件数量（不含目录，忽略符号链接与不可读文件）。 */
    private long fileCount;

    /** 递归统计的文件总字节数；目录不存在或配置非法时为 0。 */
    private Long totalSizeBytes = 0L;

    /** 目录所在分区可用字节数；探测失败时为 null。 */
    private Long diskUsableBytes;

    /** 目录所在分区总容量字节数；探测失败时为 null。 */
    private Long diskTotalBytes;

    /** 分类占用统计（「业务附件」= 根目录直接文件，其余为各一级子目录），按 sizeBytes 倒序。 */
    private List<DirectoryStatVO> directories = new ArrayList<>();
}
