package com.ceams.module.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.admin.dto.RepairAssignRequest;
import com.ceams.module.admin.dto.RepairCreateRequest;
import com.ceams.module.admin.dto.RepairFinishRequest;
import com.ceams.module.admin.entity.AdminRepair;
import com.ceams.module.admin.enums.RepairCategory;
import com.ceams.module.admin.enums.RepairStatus;
import com.ceams.module.admin.mapper.AdminRepairMapper;
import com.ceams.module.admin.support.AdminNoGenerator;
import com.ceams.module.admin.vo.RepairVO;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.notification.service.NotificationService;
import com.ceams.module.notification.support.NotificationDispatchSupport;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** 报修服务单元测试：编号生成与 BPM、派单、处理 / 完工、验收、状态机与图片上限。 */
@ExtendWith(MockitoExtension.class)
class AdminRepairServiceImplTest {

    private static final Long REPAIR_ID = 3101L;
    private static final Long ASSIGNEE_ID = 55L;
    private static final Long OTHER_USER_ID = 99L;

    @Mock private AdminRepairMapper repairMapper;
    @Mock private AdminNoGenerator noGenerator;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private UserMapper userMapper;
    @Mock private NotificationService notificationService;
    @Mock private NotificationDispatchSupport notificationDispatchSupport;

    private AdminRepairServiceImpl repairService;

    @BeforeEach
    void setUp() {
        repairService =
                new AdminRepairServiceImpl(
                        repairMapper,
                        noGenerator,
                        bpmInstanceService,
                        userMapper,
                        notificationService,
                        notificationDispatchSupport);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createShouldGenerateRepairNoAndStartBpm() {
        when(noGenerator.nextRepairNo()).thenReturn("RP-20261114-0001");
        when(repairMapper.insert(any(AdminRepair.class)))
                .thenAnswer(
                        invocation -> {
                            AdminRepair record = invocation.getArgument(0);
                            record.setId(REPAIR_ID);
                            return 1;
                        });
        when(bpmInstanceService.start(any(InstanceStartRequest.class))).thenReturn("9300");

        String id = repairService.create(request(List.of("a.jpg", "b.jpg")));

        assertThat(id).isEqualTo(String.valueOf(REPAIR_ID));
        ArgumentCaptor<AdminRepair> captor = ArgumentCaptor.forClass(AdminRepair.class);
        verify(repairMapper).insert(captor.capture());
        assertThat(captor.getValue().getRepairNo()).isEqualTo("RP-20261114-0001");
        assertThat(captor.getValue().getStatus()).isEqualTo(RepairStatus.SUBMITTED);
        assertThat(captor.getValue().getImagesJson()).isEqualTo("[\"a.jpg\",\"b.jpg\"]");
        assertThat(captor.getValue().getCategory()).isEqualTo(RepairCategory.ELECTRIC);
        assertThat(captor.getValue().getReporterId()).isEqualTo(0L);

        ArgumentCaptor<InstanceStartRequest> startCaptor =
                ArgumentCaptor.forClass(InstanceStartRequest.class);
        verify(bpmInstanceService).start(startCaptor.capture());
        assertThat(startCaptor.getValue().getTemplateCode()).isEqualTo("ADMIN_REPAIR");
        assertThat(startCaptor.getValue().getBizType()).isEqualTo("ADMIN_REPAIR");
        assertThat(startCaptor.getValue().getBizId()).isEqualTo(REPAIR_ID);

        ArgumentCaptor<AdminRepair> updateCaptor = ArgumentCaptor.forClass(AdminRepair.class);
        verify(repairMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getBpmInstanceId()).isEqualTo(9300L);
    }

    @Test
    void createShouldRejectEmptyImages() {
        RepairCreateRequest request = request(List.of());

        assertThatThrownBy(() -> repairService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(repairMapper, never()).insert(any(AdminRepair.class));
        verify(bpmInstanceService, never()).start(any(InstanceStartRequest.class));
    }

    @Test
    void createShouldRejectMoreThanSixImages() {
        RepairCreateRequest request =
                request(List.of("1", "2", "3", "4", "5", "6", "7"));

        assertThatThrownBy(() -> repairService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(repairMapper, never()).insert(any(AdminRepair.class));
        verify(bpmInstanceService, never()).start(any(InstanceStartRequest.class));
    }

    @Test
    void assignShouldRejectWhenStatusNotAssigned() {
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair(RepairStatus.SUBMITTED, 0L));

        assertThatThrownBy(() -> repairService.assign(REPAIR_ID, assignRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(repairMapper, never()).update(any(AdminRepair.class), any(Wrapper.class));
    }

    @Test
    void assignShouldWriteAssigneeAndKeepStatus() {
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair(RepairStatus.ASSIGNED, 0L));
        when(repairMapper.update(any(AdminRepair.class), any(Wrapper.class))).thenReturn(1);

        repairService.assign(REPAIR_ID, assignRequest());

        ArgumentCaptor<AdminRepair> captor = ArgumentCaptor.forClass(AdminRepair.class);
        verify(repairMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getAssigneeId()).isEqualTo(ASSIGNEE_ID);
        assertThat(captor.getValue().getAssignedAt()).isNotNull();
        // 状态保持 ASSIGNED，不写入状态字段
        assertThat(captor.getValue().getStatus()).isNull();
        assertThat(captor.getValue().getVersion()).isNull();
    }

    @Test
    void processShouldRejectNonAssignee() {
        when(repairMapper.selectById(REPAIR_ID))
                .thenReturn(repair(RepairStatus.ASSIGNED, OTHER_USER_ID));

        assertThatThrownBy(() -> repairService.process(REPAIR_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(repairMapper, never()).update(any(AdminRepair.class), any(Wrapper.class));
    }

    @Test
    void processShouldMoveToProcessing() {
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair(RepairStatus.ASSIGNED, 0L));
        when(repairMapper.update(any(AdminRepair.class), any(Wrapper.class))).thenReturn(1);

        repairService.process(REPAIR_ID, null);

        ArgumentCaptor<AdminRepair> captor = ArgumentCaptor.forClass(AdminRepair.class);
        verify(repairMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getStatus()).isEqualTo(RepairStatus.PROCESSING);
        assertThat(captor.getValue().getProcessedAt()).isNotNull();
    }

    @Test
    void processShouldRejectIllegalTransition() {
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair(RepairStatus.DONE, 0L));

        assertThatThrownBy(() -> repairService.process(REPAIR_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(repairMapper, never()).update(any(AdminRepair.class), any(Wrapper.class));
    }

    @Test
    void finishShouldRejectNonAssignee() {
        when(repairMapper.selectById(REPAIR_ID))
                .thenReturn(repair(RepairStatus.PROCESSING, OTHER_USER_ID));

        assertThatThrownBy(() -> repairService.finish(REPAIR_ID, finishRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(repairMapper, never()).update(any(AdminRepair.class), any(Wrapper.class));
    }

    @Test
    void finishShouldMoveToDoneWithRemark() {
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair(RepairStatus.PROCESSING, 0L));
        when(repairMapper.update(any(AdminRepair.class), any(Wrapper.class))).thenReturn(1);

        repairService.finish(REPAIR_ID, finishRequest());

        ArgumentCaptor<AdminRepair> captor = ArgumentCaptor.forClass(AdminRepair.class);
        verify(repairMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getStatus()).isEqualTo(RepairStatus.DONE);
        assertThat(captor.getValue().getFinishedAt()).isNotNull();
        assertThat(captor.getValue().getRemark()).isEqualTo("已更换灯管");
        assertThat(captor.getValue().getFinishImagesJson()).isEqualTo("[\"c.jpg\"]");
        assertThat(captor.getValue().getVersion()).isNull();
    }

    @Test
    void finishShouldRejectEmptyImages() {
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair(RepairStatus.PROCESSING, 0L));
        RepairFinishRequest request = new RepairFinishRequest();
        request.setRemark("已更换灯管");

        assertThatThrownBy(() -> repairService.finish(REPAIR_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(repairMapper, never()).update(any(AdminRepair.class), any(Wrapper.class));
    }

    @Test
    void finishShouldRejectMoreThanSixImages() {
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair(RepairStatus.PROCESSING, 0L));
        RepairFinishRequest request = new RepairFinishRequest();
        request.setImages(List.of("1.jpg", "2.jpg", "3.jpg", "4.jpg", "5.jpg", "6.jpg", "7.jpg"));

        assertThatThrownBy(() -> repairService.finish(REPAIR_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(repairMapper, never()).update(any(AdminRepair.class), any(Wrapper.class));
    }

    @Test
    void detailShouldRejectOtherUser() {
        AdminRepair repair = repair(RepairStatus.DONE, OTHER_USER_ID);
        repair.setReporterId(OTHER_USER_ID + 1);
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair);

        assertThatThrownBy(() -> repairService.detail(REPAIR_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    void detailShouldAllowReporter() {
        AdminRepair repair = repair(RepairStatus.DONE, OTHER_USER_ID);
        repair.setFinishImagesJson("[\"c.jpg\"]");
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair);

        RepairVO vo = repairService.detail(REPAIR_ID);

        assertThat(vo.getFinishImages()).containsExactly("c.jpg");
    }

    @Test
    void detailShouldAllowAssignee() {
        loginAs(ASSIGNEE_ID);
        AdminRepair repair = repair(RepairStatus.PROCESSING, ASSIGNEE_ID);
        repair.setReporterId(OTHER_USER_ID);
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair);

        assertThat(repairService.detail(REPAIR_ID).getId()).isEqualTo(REPAIR_ID);
    }

    @Test
    void detailShouldAllowAdmin() {
        loginAs(OTHER_USER_ID, "ROLE_SYS_ADMIN");
        AdminRepair repair = repair(RepairStatus.DONE, OTHER_USER_ID + 1);
        repair.setReporterId(OTHER_USER_ID + 2);
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair);

        assertThat(repairService.detail(REPAIR_ID).getId()).isEqualTo(REPAIR_ID);
    }

    @Test
    void detailShouldThrowNotFound() {
        assertThatThrownBy(() -> repairService.detail(REPAIR_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_NOT_FOUND);
    }

    @Test
    void assignedShouldQueryCurrentUserAsAssignee() {
        when(repairMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(repair(RepairStatus.ASSIGNED, 0L)));

        assertThat(repairService.assigned()).hasSize(1);
        verify(repairMapper).selectList(any(Wrapper.class));
    }

    @Test
    void acceptShouldRejectNonReporter() {
        AdminRepair repair = repair(RepairStatus.DONE, OTHER_USER_ID);
        repair.setReporterId(OTHER_USER_ID);
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair);

        assertThatThrownBy(() -> repairService.accept(REPAIR_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(repairMapper, never()).update(any(AdminRepair.class), any(Wrapper.class));
    }

    @Test
    void acceptShouldMoveToAccepted() {
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair(RepairStatus.DONE, 0L));
        when(repairMapper.update(any(AdminRepair.class), any(Wrapper.class))).thenReturn(1);

        repairService.accept(REPAIR_ID);

        ArgumentCaptor<AdminRepair> captor = ArgumentCaptor.forClass(AdminRepair.class);
        verify(repairMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getStatus()).isEqualTo(RepairStatus.ACCEPTED);
        assertThat(captor.getValue().getAcceptedAt()).isNotNull();
    }

    @Test
    void acceptShouldRejectWhenNotDone() {
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair(RepairStatus.PROCESSING, 0L));

        assertThatThrownBy(() -> repairService.accept(REPAIR_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(repairMapper, never()).update(any(AdminRepair.class), any(Wrapper.class));
    }

    @Test
    void acceptShouldAllowAdmin() {
        loginAs(OTHER_USER_ID, "ROLE_SYS_ADMIN");
        when(repairMapper.selectById(REPAIR_ID))
                .thenReturn(repair(RepairStatus.DONE, OTHER_USER_ID + 1));
        when(repairMapper.update(any(AdminRepair.class), any(Wrapper.class))).thenReturn(1);

        repairService.accept(REPAIR_ID);

        verify(repairMapper).update(any(AdminRepair.class), any(Wrapper.class));
    }

    private AdminRepair repair(RepairStatus status, Long assigneeId) {
        AdminRepair repair = new AdminRepair();
        repair.setId(REPAIR_ID);
        repair.setRepairNo("RP-20261114-0001");
        // 报修人与当前登录用户（单元测试无登录态为 0）保持一致
        repair.setReporterId(0L);
        repair.setLocation("3 号楼 502");
        repair.setCategory(RepairCategory.ELECTRIC);
        repair.setDescription("会议室灯管闪烁");
        repair.setAssigneeId(assigneeId);
        repair.setStatus(status);
        return repair;
    }

    private RepairCreateRequest request(List<String> images) {
        RepairCreateRequest request = new RepairCreateRequest();
        request.setLocation("3 号楼 502");
        request.setCategory(RepairCategory.ELECTRIC);
        request.setDescription("会议室灯管闪烁");
        request.setImages(images);
        return request;
    }

    private RepairAssignRequest assignRequest() {
        RepairAssignRequest request = new RepairAssignRequest();
        request.setAssigneeId(ASSIGNEE_ID);
        return request;
    }

    private RepairFinishRequest finishRequest() {
        RepairFinishRequest request = new RepairFinishRequest();
        request.setRemark("已更换灯管");
        request.setImages(List.of("c.jpg"));
        return request;
    }

    private void loginAs(Long userId, String... authorities) {
        List<GrantedAuthority> granted =
                Arrays.stream(authorities)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, null, granted));
    }
}
