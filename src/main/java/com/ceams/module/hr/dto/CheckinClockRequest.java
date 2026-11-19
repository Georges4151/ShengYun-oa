package com.ceams.module.hr.dto;

import com.ceams.module.hr.enums.CheckinSource;
import com.ceams.module.hr.enums.CheckinType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/** 打卡请求。 */
@Data
public class CheckinClockRequest {

    @NotNull(message = "打卡类型不能为空")
    private CheckinType checkinType;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Size(max = 64, message = "WiFi名称长度不能超过 64")
    private String wifiSsid;

    @Size(max = 64, message = "WiFi MAC地址长度不能超过 64")
    private String wifiBssid;

    @Size(max = 255, message = "打卡地址长度不能超过 255")
    private String address;

    /** 打卡来源，默认 MINI。 */
    private CheckinSource source;
}
