package com.ceams.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Contact {
    private Integer contactId;
    private String name;
    private String type;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;
    private Date createdAt;
    private Date updatedAt;
}