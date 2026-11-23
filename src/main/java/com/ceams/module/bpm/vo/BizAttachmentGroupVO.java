package com.ceams.module.bpm.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 审批单据附件分组（如「票据图片」「报修图片」「合同文件」）。 */
@Data
public class BizAttachmentGroupVO {

    /** 分组名称。 */
    private String groupName;

    /** 分组内附件列表（无附件时不返回该分组）。 */
    private List<BizAttachmentVO> items = new ArrayList<>();
}
