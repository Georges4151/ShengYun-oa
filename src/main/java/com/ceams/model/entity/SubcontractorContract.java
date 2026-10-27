package com.ceams.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class SubcontractorContract {
    private Integer id;

    private Integer subcontractorId;

    private Integer projectId;

    private String contractName;

    private String contractNumber;

    private java.math.BigDecimal contractAmount;

    private Date startDate;

    private Date endDate;

    private String status;

    private String description;

    private Date createTime;

    private Date updateTime;

    // 关联的分包商信息
    private Subcontractor subcontractor;

    // 关联的项目信息
    private Project project;

    // 合同文件路径
    private String filePath;

    // 文件名称
    private String fileName;
    
    // 已支付金额
    private java.math.BigDecimal paidAmount;
}
