package com.ceams.module.hr.support;

import java.time.LocalTime;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** HR 打卡规则配置（{@code ceams.hr.checkin.*}）。 */
@Data
@Component
@ConfigurationProperties(prefix = "ceams.hr.checkin")
public class HrCheckinProperties {

    /** 无排班时默认上班时间。 */
    private LocalTime defaultStart = LocalTime.of(9, 0);

    /** 无排班时默认下班时间。 */
    private LocalTime defaultEnd = LocalTime.of(18, 0);

    /** 迟到/早退宽限分钟数。 */
    private int graceMinutes = 10;

    /** 是否要求必须先排班才允许打卡（默认 false：非排班日允许打卡）。 */
    private boolean requireSchedule = false;
}
