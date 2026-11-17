package com.ceams.module.hr.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.module.admin.entity.AdminAssetBorrow;
import com.ceams.module.admin.service.AdminAssetService;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.service.HrContractService;
import com.ceams.module.hr.vo.ContractVO;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.service.NotificationService;
import com.ceams.module.notification.support.NotificationDispatchSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 合同到期 / 资产逾期提醒去重单元测试：24 小时内已提醒过则不重复发送。 */
@ExtendWith(MockitoExtension.class)
class M6ReminderNotificationTest {

    private static final int EXPIRING_DAYS = 30;

    @Mock private HrContractService contractService;
    @Mock private AdminAssetService assetService;
    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private NotificationService notificationService;
    @Mock private NotificationDispatchSupport notificationDispatchSupport;

    private HrAdminReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler =
                new HrAdminReminderScheduler(
                        contractService,
                        assetService,
                        employeeMapper,
                        notificationService,
                        notificationDispatchSupport);
    }

    @Test
    void expiringContractShouldSkipWhenRemindedWithin24Hours() {
        when(contractService.expiring(EXPIRING_DAYS))
                .thenReturn(List.of(contract(8001L, 900L)));
        when(employeeMapper.selectBatchIds(any())).thenReturn(List.of(employee(900L, 501L)));
        when(assetService.listOverdueBorrows(any(LocalDate.class))).thenReturn(List.of());
        when(notificationService.existsRecent(
                        eq(NotificationType.HR), anyString(), any(LocalDateTime.class)))
                .thenReturn(true);

        scheduler.scanAndRemind();

        verify(notificationDispatchSupport, never())
                .send(any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void overdueBorrowShouldSkipWhenRemindedWithin24Hours() {
        when(contractService.expiring(EXPIRING_DAYS)).thenReturn(List.of());
        when(assetService.listOverdueBorrows(any(LocalDate.class)))
                .thenReturn(List.of(borrow(7001L, 502L)));
        when(notificationService.existsRecent(
                        eq(NotificationType.ADMIN), anyString(), any(LocalDateTime.class)))
                .thenReturn(true);

        scheduler.scanAndRemind();

        verify(notificationDispatchSupport, never())
                .send(any(), anyString(), anyString(), anyString(), any());
    }

    private ContractVO contract(Long id, Long employeeId) {
        ContractVO vo = new ContractVO();
        vo.setId(id);
        vo.setEmployeeId(employeeId);
        vo.setContractNo("HT-2026-001");
        vo.setEndDate(LocalDate.of(2026, 12, 1));
        return vo;
    }

    private HrEmployee employee(Long id, Long userId) {
        HrEmployee employee = new HrEmployee();
        employee.setId(id);
        employee.setUserId(userId);
        return employee;
    }

    private AdminAssetBorrow borrow(Long id, Long borrowerId) {
        AdminAssetBorrow borrow = new AdminAssetBorrow();
        borrow.setId(id);
        borrow.setBorrowerId(borrowerId);
        borrow.setExpectedReturnDate(LocalDate.of(2026, 11, 1));
        return borrow;
    }
}
