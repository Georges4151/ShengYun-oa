package com.ceams.module.bpm.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批单据附件视图（业务详情附件分组中的单个文件）。
 *
 * <p>{@code name} 为文件模块返回的服务端存储名，前端需原样回传给受控下载接口；
 * {@code fileName} 为展示名（优先取业务表记录的原始文件名）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BizAttachmentVO {

    /** 服务端存储文件名（文件服务 name，受控下载时使用）。 */
    private String name;

    /** 展示文件名（原始文件名；缺失时回退为存储名）。 */
    private String fileName;

    /** 文件种类：IMAGE / PDF / OFFICE / OTHER。 */
    private String kind;
}
