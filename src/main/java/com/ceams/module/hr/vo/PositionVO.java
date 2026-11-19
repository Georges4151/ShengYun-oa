package com.ceams.module.hr.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 岗位视图对象。 */
@Data
public class PositionVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String code;

    private String name;

    private String level;

    private String jobFamily;

    private Integer sortOrder;

    private String status;

    private String remark;

    private LocalDateTime createTime;
}
