package com.ceams.module.hr.converter;

import com.ceams.module.hr.entity.HrPosition;
import com.ceams.module.hr.vo.PositionVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 岗位实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface HrPositionConverter {

    default PositionVO toVO(HrPosition position) {
        if (position == null) {
            return null;
        }
        PositionVO vo = new PositionVO();
        vo.setId(position.getId());
        vo.setCode(position.getCode());
        vo.setName(position.getName());
        vo.setLevel(position.getLevel());
        vo.setJobFamily(position.getJobFamily());
        vo.setSortOrder(position.getSortOrder());
        vo.setStatus(position.getStatus());
        vo.setRemark(position.getRemark());
        vo.setCreateTime(position.getCreateTime());
        return vo;
    }

    List<PositionVO> toVOList(List<HrPosition> positions);
}
