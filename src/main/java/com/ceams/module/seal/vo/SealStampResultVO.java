package com.ceams.module.seal.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/** 盖章结果。 */
@Data
public class SealStampResultVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long stampId;

    /** 盖章 PDF 下载路径。 */
    private String downloadPath;
}
