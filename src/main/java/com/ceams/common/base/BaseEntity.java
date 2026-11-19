package com.ceams.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 所有业务实体的公共审计基类。
 *
 * <ul>
 *   <li>{@code id}：雪花 ID（DB BIGINT，JSON 序列化为字符串）；数据库主键不 AUTO_INCREMENT。
 *   <li>{@code createTime / updateTime}：创建 / 更新时间（DATETIME）。
 *   <li>{@code createBy / updateBy}：创建 / 更新人 ID（BIGINT，逻辑引用，不建物理外键）。
 *   <li>{@code deleted}：逻辑删除标记（0=未删除，1=已删除）。
 *   <li>{@code version}：乐观锁版本号（默认 0）。
 * </ul>
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键：雪花 ID，JSON 序列化为字符串。 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 ID。 */
    @TableField(fill = FieldFill.INSERT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createBy;

    /** 更新人 ID。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateBy;

    /** 逻辑删除标记：0=未删除，1=已删除。 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted = 0;

    /** 乐观锁版本号。 */
    @Version
    @TableField("version")
    private Integer version = 0;
}
