package com.ceams.module.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/** 资产领用申请请求。 */
@Data
public class AssetBorrowRequest {

    /** 领用日期。 */
    @NotNull(message = "领用日期不能为空")
    private LocalDate borrowDate;

    /** 预计归还日期。 */
    private LocalDate expectedReturnDate;

    /** 备注。 */
    private String remark;
}
