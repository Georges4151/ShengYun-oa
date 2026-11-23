package com.ceams.module.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 系统配置（sys_config）。AI 模块仅复用其保存全局开关 ai.enabled。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    /** 配置键（唯一）。 */
    private String configKey;

    /** 配置值。 */
    private String configValue;

    /** 值类型：STRING/INT/BOOL/JSON。 */
    private String configType;

    /** 描述。 */
    private String description;
}
