package com.ceams.module.admin.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ceams.module.admin.entity.AdminAsset;
import com.ceams.module.admin.entity.AdminRepair;
import com.ceams.module.admin.mapper.AdminAssetMapper;
import com.ceams.module.admin.mapper.AdminRepairMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 行政单据编号生成器。
 *
 * <p>资产编号格式 {@code AST-yyyyMM-####}：按当月已有序号 +1；报修工单号格式 {@code RP-yyyyMMdd-####}：
 * 按当日已有序号 +1。并发写入依赖对应唯一键兜底，冲突时由调用方捕获 {@code DuplicateKeyException}
 * 重新取号重试（最多 3 次）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminNoGenerator {

    /** 资产编号前缀。 */
    private static final String ASSET_PREFIX = "AST-";

    /** 报修工单号前缀。 */
    private static final String REPAIR_PREFIX = "RP-";

    /** 月份格式。 */
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /** 日期格式。 */
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 序号宽度。 */
    private static final int SEQ_WIDTH = 4;

    private final AdminAssetMapper assetMapper;
    private final AdminRepairMapper repairMapper;

    /** 生成下一个资产编号（线程内同步，跨实例靠唯一键 + 重试）。 */
    public synchronized String nextAssetNo() {
        String prefix = ASSET_PREFIX + LocalDate.now().format(MONTH_FORMATTER) + "-";
        QueryWrapper<AdminAsset> wrapper = new QueryWrapper<>();
        wrapper.select("asset_no")
                .likeRight("asset_no", prefix)
                .orderByDesc("asset_no")
                .last("LIMIT 1");
        AdminAsset latest = assetMapper.selectOne(wrapper);
        int seq = 1;
        if (latest != null && StringUtils.isNotBlank(latest.getAssetNo())) {
            String tail = latest.getAssetNo().substring(prefix.length());
            try {
                seq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException e) {
                log.warn("资产编号 {} 序号段非法，重置为 1", latest.getAssetNo());
            }
        }
        return prefix + String.format("%0" + SEQ_WIDTH + "d", seq);
    }

    /** 生成下一个报修工单号 {@code RP-yyyyMMdd-####}（线程内同步，跨实例靠唯一键 + 重试）。 */
    public synchronized String nextRepairNo() {
        String prefix = REPAIR_PREFIX + LocalDate.now().format(DAY_FORMATTER) + "-";
        QueryWrapper<AdminRepair> wrapper = new QueryWrapper<>();
        wrapper.select("repair_no")
                .likeRight("repair_no", prefix)
                .orderByDesc("repair_no")
                .last("LIMIT 1");
        AdminRepair latest = repairMapper.selectOne(wrapper);
        int seq = 1;
        if (latest != null && StringUtils.isNotBlank(latest.getRepairNo())) {
            String tail = latest.getRepairNo().substring(prefix.length());
            try {
                seq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException e) {
                log.warn("报修工单号 {} 序号段非法，重置为 1", latest.getRepairNo());
            }
        }
        return prefix + String.format("%0" + SEQ_WIDTH + "d", seq);
    }
}
