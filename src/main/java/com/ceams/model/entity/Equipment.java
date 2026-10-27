package com.ceams.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Equipment {
    private Integer equipmentId;
    private String name;
    private String type;
    private String model;
    private String status;
    private Date purchaseDate;
    private BigDecimal purchasePrice;
    private Date createdAt;
    private Date updatedAt;
}