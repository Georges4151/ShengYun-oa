package com.ceams.module.hr.vo;

import com.ceams.module.hr.enums.CheckinSource;
import com.ceams.module.hr.enums.CheckinStatus;
import com.ceams.module.hr.enums.CheckinType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 打卡记录视图对象。 */
@Data
public class CheckinVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    private LocalDate checkinDate;

    private LocalDateTime checkinTime;

    private CheckinType checkinType;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private String wifiSsid;

    private String wifiBssid;

    private String address;

    private CheckinSource source;

    private CheckinStatus status;

    private String remark;
}
