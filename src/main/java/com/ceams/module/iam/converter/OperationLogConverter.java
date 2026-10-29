package com.ceams.module.iam.converter;

import com.ceams.module.iam.entity.OperationLog;
import com.ceams.module.iam.vo.OperationLogVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 操作日志实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface OperationLogConverter {

    OperationLogVO toVO(OperationLog log);

    List<OperationLogVO> toVOList(List<OperationLog> logs);
}
