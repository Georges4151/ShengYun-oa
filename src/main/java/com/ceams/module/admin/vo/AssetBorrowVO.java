package com.ceams.module.admin.vo;

import com.ceams.module.admin.enums.AssetBorrowStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 资产领用记录展示对象。 */
@Data
public class AssetBorrowVO {

    /** 记录 ID。 */
    private Long id;

    /** 资产 ID。 */
    private Long assetId;

    /** 资产编号。 */
    private String assetNo;

    /** 资产名称。 */
    private String assetName;

    /** 借用人 ID。 */
    private Long borrowerId;

    /** 借用人姓名。 */
    private String borrowerName;

    /** 领用日期。 */
    private LocalDate borrowDate;

    /** 预计归还日期。 */
    private LocalDate expectedReturnDate;

    /** 实际归还日期。 */
    private LocalDate actualReturnDate;

    /** BPM 实例 ID。 */
    private Long bpmInstanceId;

    /** 状态。 */
    private AssetBorrowStatus status;

    /** 备注。 */
    private String remark;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
