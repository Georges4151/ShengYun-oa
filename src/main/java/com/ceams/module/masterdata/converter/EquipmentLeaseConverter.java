package com.ceams.module.masterdata.converter;

import com.ceams.module.masterdata.entity.EquipmentLease;
import com.ceams.module.masterdata.vo.EquipmentLeaseVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.mapstruct.Mapper;

/** 设备租赁实体 <-> VO 转换（租赁日期 DATETIME -> date）。 */
@Mapper(componentModel = "spring")
public interface EquipmentLeaseConverter {

    EquipmentLeaseVO toVO(EquipmentLease lease);

    List<EquipmentLeaseVO> toVOList(List<EquipmentLease> leases);

    default LocalDate toLocalDate(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }
}
