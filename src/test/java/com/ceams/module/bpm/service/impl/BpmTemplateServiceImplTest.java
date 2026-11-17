package com.ceams.module.bpm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.dto.TemplateCreateRequest;
import com.ceams.module.bpm.dto.TemplatePublishRequest;
import com.ceams.module.bpm.entity.BpmTemplate;
import com.ceams.module.bpm.entity.BpmTemplateVersion;
import com.ceams.module.bpm.enums.TemplateStatus;
import com.ceams.module.bpm.enums.TemplateVersionStatus;
import com.ceams.module.bpm.mapper.BpmTemplateMapper;
import com.ceams.module.bpm.mapper.BpmTemplateVersionMapper;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.support.BpmSchema;
import com.ceams.module.bpm.support.BpmSchemaParser;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 流程模板服务单元测试：编码唯一、发布归档旧版本并启用模板。 */
@ExtendWith(MockitoExtension.class)
class BpmTemplateServiceImplTest {

    @Mock private BpmTemplateMapper templateMapper;
    @Mock private BpmTemplateVersionMapper versionMapper;
    @Mock private BpmSchemaParser schemaParser;
    @Mock private BpmAuditService auditService;
    @Mock private BpmSecurityGuard securityGuard;

    private BpmTemplateServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new BpmTemplateServiceImpl(
                        templateMapper, versionMapper, schemaParser, auditService, securityGuard);
    }

    @Test
    void 模板编码重复时拒绝创建() {
        when(templateMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setCode("EXPENSE_FLOW");
        request.setName("报销审批");
        request.setBizType("EXPENSE");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_DUPLICATE_CODE);
        verify(templateMapper, never()).insert(any(BpmTemplate.class));
    }

    @Test
    void 发布新版本归档旧版本并启用模板() {
        BpmTemplate template = new BpmTemplate();
        template.setId(1L);
        template.setCode("EXPENSE_FLOW");
        template.setBizType("EXPENSE");
        template.setStatus(TemplateStatus.DISABLED);
        when(templateMapper.selectById(1L)).thenReturn(template);
        BpmTemplateVersion old = new BpmTemplateVersion();
        old.setId(21L);
        old.setTemplateId(1L);
        old.setVersionNo(1);
        old.setStatus(TemplateVersionStatus.PUBLISHED);
        when(versionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(old));
        when(schemaParser.parse(any())).thenReturn(new BpmSchema());
        doAnswer(
                        invocation -> {
                            BpmTemplateVersion version = invocation.getArgument(0);
                            version.setId(22L);
                            return 1;
                        })
                .when(versionMapper)
                .insert(any(BpmTemplateVersion.class));

        TemplatePublishRequest request = new TemplatePublishRequest();
        request.setSchemaJson("{\"nodes\":[]}");
        Long versionId = service.publish(1L, request);

        assertThat(versionId).isEqualTo(22L);
        assertThat(old.getStatus()).isEqualTo(TemplateVersionStatus.ARCHIVED);
        ArgumentCaptor<BpmTemplateVersion> captor =
                ArgumentCaptor.forClass(BpmTemplateVersion.class);
        verify(versionMapper).insert(captor.capture());
        assertThat(captor.getValue().getVersionNo()).isEqualTo(2);
        assertThat(captor.getValue().getStatus()).isEqualTo(TemplateVersionStatus.PUBLISHED);
        assertThat(template.getStatus()).isEqualTo(TemplateStatus.ENABLED);
        assertThat(template.getCurrentVersionId()).isEqualTo(22L);
        verify(auditService).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 停用模板只改状态不影响在途实例() {
        BpmTemplate template = new BpmTemplate();
        template.setId(1L);
        template.setCode("EXPENSE_FLOW");
        template.setBizType("EXPENSE");
        template.setStatus(TemplateStatus.ENABLED);
        when(templateMapper.selectById(1L)).thenReturn(template);

        service.disable(1L);

        assertThat(template.getStatus()).isEqualTo(TemplateStatus.DISABLED);
        verify(templateMapper).updateById(template);
        verify(securityGuard).requireAdminOrPermission("bpm:template:manage");
    }

    @Test
    void 未发布模板不可启用() {
        BpmTemplate template = new BpmTemplate();
        template.setId(1L);
        template.setCode("EXPENSE_FLOW");
        template.setBizType("EXPENSE");
        template.setStatus(TemplateStatus.DISABLED);
        when(templateMapper.selectById(1L)).thenReturn(template);

        assertThatThrownBy(() -> service.enable(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BPM_TEMPLATE_NOT_ENABLED);
    }

    @Test
    void 模板不存在时更新返回未找到() {
        lenient().when(templateMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(
                        () ->
                                service.update(
                                        99L, new com.ceams.module.bpm.dto.TemplateUpdateRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_NOT_FOUND);
    }
}
