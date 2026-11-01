package com.ceams.module.file.vo;

import lombok.Data;

/**
 * 文件上传结果视图对象（file 模块，轻量应用能力，无实体、无 Mapper）。
 *
 * <p>字段为接口出参，不参与任何持久化关联，故无需 MapStruct，直接手写 setter（Lombok 生成）。
 */
@Data
public class FileVO {

    /** 服务端存储文件名（含后缀，雪花 ID + 白名单扩展名）。 */
    private String name;

    /** 相对访问路径（如 {@code /api/v1/files/123.jpg}），前端预览时自行拼 API_BASE_URL。 */
    private String url;

    /** 文件大小（字节）。 */
    private long size;

    /** 原始请求 content-type（可能为空）。 */
    private String contentType;
}