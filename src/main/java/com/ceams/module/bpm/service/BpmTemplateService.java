package com.ceams.module.bpm.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.dto.TemplateCreateRequest;
import com.ceams.module.bpm.dto.TemplatePublishRequest;
import com.ceams.module.bpm.dto.TemplateUpdateRequest;
import com.ceams.module.bpm.enums.TemplateStatus;
import com.ceams.module.bpm.vo.TemplateVO;
import com.ceams.module.bpm.vo.TemplateVersionVO;
import java.util.List;

/** 流程模板服务。 */
public interface BpmTemplateService {

    /** 分页查询模板。 */
    PageResult<TemplateVO> page(
            long page, long size, String bizType, TemplateStatus status, String keyword);

    /** 创建模板（初始为停用，发布首个版本后启用）。 */
    String create(TemplateCreateRequest request);

    /** 更新模板元数据（不影响历史版本与在途实例）。 */
    void update(Long id, TemplateUpdateRequest request);

    /** 模板详情（含当前版本）。 */
    TemplateVO detail(Long id);

    /** 版本列表（按版本号倒序）。 */
    List<TemplateVersionVO> versions(Long templateId);

    /** 发布新版本：仅新实例生效，历史实例锁定原版本。 */
    Long publish(Long templateId, TemplatePublishRequest request);

    /** 启用模板（要求已存在发布版本）。 */
    void enable(Long id);

    /** 停用模板（不影响在途实例）。 */
    void disable(Long id);
}
