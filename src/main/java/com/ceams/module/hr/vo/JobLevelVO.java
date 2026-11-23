package com.ceams.module.hr.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 职级视图对象。 */
@Data
public class JobLevelVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String code;

    private String name;

    private String jobFamily;

    private Integer rankNo;

    private Integer sortOrder;

    private String status;

    private String remark;

    private LocalDateTime createTime;
}
