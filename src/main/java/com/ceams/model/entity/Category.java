package com.ceams.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Category {
    private Integer categoryId;
    private String name;
    private String type;
    private Integer parentId;
    private Date createdAt;
    private Date updatedAt;
}