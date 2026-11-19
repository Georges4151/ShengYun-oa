package com.ceams.module.file.vo;

import lombok.Data;

/**
 * 文件存储分类统计视图对象（file 模块只读出参）。
 *
 * <p>用于描述上传目录下某一分类的占用情况：{@code 业务附件} 表示根目录直接文件， 其余条目为根目录下的一级子目录（如
 * {@code avatars} / {@code doc} / {@code tmp}）。
 */
@Data
public class DirectoryStatVO {

    /** 分类名称：根目录直接文件固定为「业务附件」，子目录取目录名。 */
    private String name;

    /** 该分类下的常规文件数量（不含目录）。 */
    private long fileCount;

    /** 该分类占用的字节数。 */
    private Long sizeBytes;

    public DirectoryStatVO() {}

    public DirectoryStatVO(String name, long fileCount, Long sizeBytes) {
        this.name = name;
        this.fileCount = fileCount;
        this.sizeBytes = sizeBytes;
    }
}
