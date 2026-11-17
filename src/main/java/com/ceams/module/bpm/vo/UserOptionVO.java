package com.ceams.module.bpm.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/** 代理候选人选项（仅返回展示所需字段，不复用 IAM 管理接口）。 */
@Data
public class UserOptionVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;
    private String username;
}
