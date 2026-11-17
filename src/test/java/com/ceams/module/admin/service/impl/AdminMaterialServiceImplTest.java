package com.ceams.module.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.module.admin.dto.MaterialApplyRequest;
import com.ceams.module.admin.dto.MaterialStockInRequest;
import com.ceams.module.admin.entity.AdminMaterial;
import com.ceams.module.admin.entity.AdminMaterialApply;
import com.ceams.module.admin.enums.MaterialApplyStatus;
import com.ceams.module.admin.enums.MaterialStatus;
import com.ceams.module.admin.mapper.AdminMaterialApplyMapper;
import com.ceams.module.admin.mapper.AdminMaterialMapper;
import com.ceams.module.admin.vo.MaterialVO;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.iam.mapper.UserMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 易耗品服务单元测试：入库、申领、发放、低库存预警排序。 */
@ExtendWith(MockitoExtension.class)
class AdminMaterialServiceImplTest {

    private static final Long MATERIAL_ID = 3001L;
    private static final Long APPLY_ID = 4001L;

    @Mock private AdminMaterialMapper materialMapper;
    @Mock private AdminMaterialApplyMapper applyMapper;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private BpmAuditService bpmAuditService;
    @Mock private UserMapper userMapper;

    private AdminMaterialServiceImpl materialService;

    @BeforeEach
    void setUp() {
        materialService =
                new AdminMaterialServiceImpl(
                        materialMapper, applyMapper, bpmInstanceService, bpmAuditService, userMapper);
    }

    @Test
    void stockInShouldAccumulateStockAndWriteAudit() {
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(material("10", "5"));
        when(materialMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        MaterialStockInRequest request = new MaterialStockInRequest();
        request.setQuantity(new BigDecimal("5"));
        request.setRemark("采购入库");

        materialService.stockIn(MATERIAL_ID, request);

        verify(materialMapper).update(isNull(), any(Wrapper.class));
        ArgumentCaptor<Object> beforeCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> afterCaptor = ArgumentCaptor.forClass(Object.class);
        verify(bpmAuditService)
                .record(
                        anyString(),
                        any(),
                        any(AuditAction.class),
                        beforeCaptor.capture(),
                        afterCaptor.capture(),
                        any(),
                        any());
        assertThat(beforeCaptor.getValue().toString()).contains("10");
        assertThat(afterCaptor.getValue().toString()).contains("15");
    }

    @Test
    void applyShouldCreateApplyAndStartBpm() {
        when(materialMapper.selectById(MATERIAL_ID)).thenReturn(material("100", "10"));
        when(applyMapper.insert(any(AdminMaterialApply.class)))
                .thenAnswer(
                        invocation -> {
                            AdminMaterialApply apply = invocation.getArgument(0);
                            apply.setId(APPLY_ID);
                            return 1;
                        });
        when(bpmInstanceService.start(any(InstanceStartRequest.class))).thenReturn("9101");

        MaterialApplyRequest request = new MaterialApplyRequest();
        request.setMaterialId(MATERIAL_ID);
        request.setQuantity(new BigDecimal("8"));
        request.setReason("办公使用");

        String id = materialService.apply(request);

        assertThat(id).isEqualTo(String.valueOf(APPLY_ID));
        ArgumentCaptor<AdminMaterialApply> insertCaptor =
                ArgumentCaptor.forClass(AdminMaterialApply.class);
        verify(applyMapper).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getStatus()).isEqualTo(MaterialApplyStatus.APPROVING);
        assertThat(insertCaptor.getValue().getQuantity()).isEqualByComparingTo("8");
        assertThat(insertCaptor.getValue().getApplicantId()).isEqualTo(0L);

        ArgumentCaptor<InstanceStartRequest> startCaptor =
                ArgumentCaptor.forClass(InstanceStartRequest.class);
        verify(bpmInstanceService).start(startCaptor.capture());
        assertThat(startCaptor.getValue().getTemplateCode()).isEqualTo("ADMIN_MATERIAL_APPLY");
        assertThat(startCaptor.getValue().getBizId()).isEqualTo(APPLY_ID);

        ArgumentCaptor<AdminMaterialApply> updateCaptor =
                ArgumentCaptor.forClass(AdminMaterialApply.class);
        verify(applyMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getBpmInstanceId()).isEqualTo(9101L);
    }

    @Test
    void issueShouldRejectRepeatedIssue() {
        AdminMaterialApply apply = new AdminMaterialApply();
        apply.setId(APPLY_ID);
        apply.setStatus(MaterialApplyStatus.ISSUED);
        when(applyMapper.selectById(APPLY_ID)).thenReturn(apply);
        when(applyMapper.update(any(AdminMaterialApply.class), any(Wrapper.class))).thenReturn(0);

        assertThatThrownBy(() -> materialService.issue(APPLY_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅审批通过");
    }

    @Test
    void alertsShouldSortByShortageRatioDesc() {
        AdminMaterial small = material("10", "100");
        small.setId(1L);
        AdminMaterial medium = material("50", "100");
        medium.setId(2L);
        AdminMaterial large = material("90", "100");
        large.setId(3L);
        when(materialMapper.selectList(any(Wrapper.class))).thenReturn(List.of(small, medium, large));

        List<MaterialVO> alerts = materialService.alerts();

        assertThat(alerts).hasSize(3);
        assertThat(alerts.get(0).getId()).isEqualTo(1L);
        assertThat(alerts.get(1).getId()).isEqualTo(2L);
        assertThat(alerts.get(2).getId()).isEqualTo(3L);
        assertThat(alerts.get(0).getShortageRatio()).isEqualByComparingTo("0.9");
        assertThat(alerts.get(1).getShortageRatio()).isEqualByComparingTo("0.5");
    }

    private AdminMaterial material(String stock, String safety) {
        AdminMaterial material = new AdminMaterial();
        material.setId(MATERIAL_ID);
        material.setCode("M-001");
        material.setName("A4 纸");
        material.setUnit("包");
        material.setStockQty(new BigDecimal(stock));
        material.setSafetyStock(new BigDecimal(safety));
        material.setStatus(MaterialStatus.ENABLED);
        return material;
    }
}
