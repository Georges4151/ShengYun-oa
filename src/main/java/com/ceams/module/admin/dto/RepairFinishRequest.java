package com.ceams.module.admin.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 报修完工请求。 */
@Data
public class RepairFinishRequest {

    /** 处理说明。 */
    private String remark;

    /** 完工图片文件名列表（必填，1～6 张）。 */
    @Size(max = 6, message = "完工图片最多上传 6 张")
    private List<String> images;
}
