package com.ceams.module.iam.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 部门视图对象（组织树节点）。 */
@Data
public class DeptVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 部门名称。 */
    private String name;

    /** 上级部门 ID（根部门为 null）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /** 部门主管用户 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long leaderId;

    /** 部门主管姓名（批量装配，避免前端二次查询）。 */
    private String leaderName;

    /** 同级排序号。 */
    private Integer sortOrder;

    private LocalDateTime createTime;

    /** 子部门（仅树接口填充）。 */
    private List<DeptVO> children = new ArrayList<>();
}
