package com.ceams.module.hr.support;

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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 人力 / 行政事件提醒定时调度器。
 *
 * <p>按 {@code ceams.reminder.cron}（默认每天 09:00）触发一次扫描：
 *
 * <ol>
 *   <li>劳动合同到期：调用 {@link HrContractService#expiring(int)}（30 天内），
 *       按员工绑定的系统账号发送 {@link NotificationType#HR} 通知；
 *   <li>资产借用逾期：调用 {@link AdminAssetService#listOverdueBorrows(LocalDate)}，
 *       按借用人发送 {@link NotificationType#ADMIN} 通知。
 * </ol>
 *
 * <p>去重口径：同一「类型 + 深链」24 小时内已发送则跳过，深链内含业务单据 ID 天然按单据去重。
 * 调度异常、单条发送异常均只记录日志，不向容器抛出，避免调度线程被阻塞。
 *
 * <p>注意：{@code @EnableScheduling} 已由 {@code AiSummarySchedulingConfiguration} 开启，本类不重复声明。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "ceams.reminder",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class HrAdminReminderScheduler {

    /** 合同到期提醒窗口（天）。 */
    private static final int EXPIRING_DAYS = 30;

    /** 同一业务单据提醒去重窗口（小时）。 */
    private static final long DEDUP_HOURS = 24L;

    /** 合同到期深链前缀。 */
    private static final String CONTRACT_LINK_PREFIX = "ceams://hr/contract/";

    /** 资产借用深链前缀。 */
    private static final String ASSET_BORROW_LINK_PREFIX = "ceams://admin/asset-borrow/";

    private final HrContractService contractService;
    private final AdminAssetService assetService;
    private final HrEmployeeMapper employeeMapper;
    private final NotificationService notificationService;
    private final NotificationDispatchSupport notificationDispatchSupport;

    /** 定时扫描并发送提醒（单条失败不影响后续）。 */
    @Scheduled(cron = "${ceams.reminder.cron:0 0 9 * * ?}")
    public void scanAndRemind() {
        try {
            remindExpiringContracts();
        } catch (Exception e) {
            log.warn("劳动合同到期提醒执行失败（已忽略，等待下一轮调度）", e);
        }
        try {
            remindOverdueBorrows();
        } catch (Exception e) {
            log.warn("资产借用逾期提醒执行失败（已忽略，等待下一轮调度）", e);
        }
    }

    /** 劳动合同 30 天内到期提醒。 */
    private void remindExpiringContracts() {
        List<ContractVO> contracts = contractService.expiring(EXPIRING_DAYS);
        if (contracts == null || contracts.isEmpty()) {
            return;
        }
        Map<Long, Long> employeeUserIds = loadEmployeeUserIds(contracts);
        LocalDateTime since = LocalDateTime.now().minusHours(DEDUP_HOURS);
        for (ContractVO contract : contracts) {
            try {
                if (contract.getId() == null) {
                    continue;
                }
                Long userId = employeeUserIds.get(contract.getEmployeeId());
                if (userId == null) {
                    continue;
                }
                String linkUrl = CONTRACT_LINK_PREFIX + contract.getId();
                if (notificationService.existsRecent(NotificationType.HR, linkUrl, since)) {
                    continue;
                }
                String content =
                        "您的劳动合同（编号 "
                                + (contract.getContractNo() == null ? "-" : contract.getContractNo())
                                + "）将于 "
                                + contract.getEndDate()
                                + " 到期，请及时办理续签或终止手续。";
                notificationDispatchSupport.send(
                        NotificationType.HR,
                        "劳动合同即将到期（30 天内）",
                        content,
                        linkUrl,
                        List.of(userId));
            } catch (Exception e) {
                log.warn(
                        "劳动合同到期提醒发送失败（已忽略）：contractId={}，原因={}",
                        contract.getId(),
                        e.getMessage(),
                        e);
            }
        }
    }

    /** 资产借用逾期提醒。 */
    private void remindOverdueBorrows() {
        List<AdminAssetBorrow> borrows = assetService.listOverdueBorrows(LocalDate.now());
        if (borrows == null || borrows.isEmpty()) {
            return;
        }
        LocalDateTime since = LocalDateTime.now().minusHours(DEDUP_HOURS);
        for (AdminAssetBorrow borrow : borrows) {
            try {
                if (borrow.getId() == null || borrow.getBorrowerId() == null) {
                    continue;
                }
                String linkUrl = ASSET_BORROW_LINK_PREFIX + borrow.getId();
                if (notificationService.existsRecent(NotificationType.ADMIN, linkUrl, since)) {
                    continue;
                }
                String content =
                        "您借用的资产（借用记录 ID "
                                + borrow.getId()
                                + "）已于 "
                                + borrow.getExpectedReturnDate()
                                + " 逾期未归还，请尽快办理归还手续。";
                notificationDispatchSupport.send(
                        NotificationType.ADMIN,
                        "资产借用已逾期",
                        content,
                        linkUrl,
                        List.of(borrow.getBorrowerId()));
            } catch (Exception e) {
                log.warn(
                        "资产借用逾期提醒发送失败（已忽略）：borrowId={}，原因={}",
                        borrow.getId(),
                        e.getMessage(),
                        e);
            }
        }
    }

    /** 批量解析员工 -> 系统账号（无绑定账号的员工不产生映射）。 */
    private Map<Long, Long> loadEmployeeUserIds(List<ContractVO> contracts) {
        List<Long> employeeIds =
                contracts.stream()
                        .map(ContractVO::getEmployeeId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        List<HrEmployee> employees = employeeMapper.selectBatchIds(employeeIds);
        if (employees == null || employees.isEmpty()) {
            return Map.of();
        }
        return employees.stream()
                .filter(employee -> employee.getUserId() != null)
                .collect(
                        Collectors.toMap(
                                HrEmployee::getId, HrEmployee::getUserId, (a, b) -> a));
    }
}
