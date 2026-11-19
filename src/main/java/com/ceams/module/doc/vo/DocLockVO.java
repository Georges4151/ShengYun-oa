package com.ceams.module.doc.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/** 文档编辑锁状态。 */
@Data
public class DocLockVO {

    /** 是否处于编辑锁（Redis 为准，已过期视为未锁）。 */
    private boolean locked;

    /** 锁持有人用户 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ownerId;

    /** 锁持有人姓名（取不到返回 null）。 */
    private String ownerName;
}
