package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.hr.enums.CheckinSource;
import com.ceams.module.hr.enums.CheckinStatus;
import com.ceams.module.hr.enums.CheckinType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** HR 打卡记录（hr_checkin）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_checkin")
public class HrCheckin extends BaseEntity {

    /** 员工 ID（逻辑引用 hr_employee.id）。 */
    private Long employeeId;

    /** 打卡日期。 */
    private LocalDate checkinDate;

    /** 打卡时间。 */
    private LocalDateTime checkinTime;

    /** 打卡类型。 */
    private CheckinType checkinType;

    /** 纬度。 */
    private BigDecimal latitude;

    /** 经度。 */
    private BigDecimal longitude;

    /** WiFi 名称。 */
    private String wifiSsid;

    /** WiFi MAC 地址。 */
    private String wifiBssid;

    /** 打卡地址。 */
    private String address;

    /** 打卡来源。 */
    private CheckinSource source;

    /** 打卡状态。 */
    private CheckinStatus status;

    /** 备注。 */
    private String remark;
}
