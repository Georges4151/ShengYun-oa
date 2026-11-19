package com.ceams.module.admin.dto;

import com.ceams.module.admin.enums.RepairCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 报修提交请求。 */
@Data
public class RepairCreateRequest {

    /** 报修位置。 */
    @NotBlank(message = "报修位置不能为空")
    private String location;

    /** 报修分类。 */
    @NotNull(message = "报修分类不能为空")
    private RepairCategory category;

    /** 故障描述。 */
    @NotBlank(message = "故障描述不能为空")
    @Size(max = 1000, message = "故障描述不能超过 1000 字")
    private String description;

    /** 图片文件名列表（必填，1～6 张）。 */
    @NotEmpty(message = "请至少上传 1 张现场图片")
    @Size(max = 6, message = "图片最多上传 6 张")
    private List<String> images;
}
