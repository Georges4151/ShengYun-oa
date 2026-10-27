package com.ceams.module.masterdata.converter;

import com.ceams.module.masterdata.entity.Equipment;
import com.ceams.module.masterdata.vo.EquipmentVO;
import org.mapstruct.Mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备台账实体 <-> VO 转换（购置日期 DATETIME -> date）。
 */
@Mapper(componentModel = "spring")
public interface EquipmentConverter {

    EquipmentVO toVO(Equipment equipment);

    List<EquipmentVO> toVOList(List<Equipment> equipmentList);

    default LocalDate toLocalDate(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }
}