package com.ceams.module.bpm.vo;

import java.util.List;
import lombok.Data;

/** 发起前流程预览。 */
@Data
public class FlowPreviewVO {

    private String templateCode;
    private String templateName;
    private String bizType;
    private List<PreviewNodeVO> nodes;
}
