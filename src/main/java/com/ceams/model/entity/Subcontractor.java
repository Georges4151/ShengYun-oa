package com.ceams.model.entity;

import java.util.Date;
import java.util.List;

public class Subcontractor {
    private Integer id;
    private String name;
    private String contactPerson;
    private String phone;
    private String address;
    private String qualificationLevel;
    private String creditRating;
    private String businessScope;
    private String taxIdentificationNumber;
    private String bankAccount;
    private String description;
    private Date createTime;
    private Date updateTime;
    private List<Project> projects;
    private List<SubcontractorContract> contracts;
    private List<String> projectNames;
    private Integer contractCount;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getQualificationLevel() { return qualificationLevel; }
    public void setQualificationLevel(String qualificationLevel) { this.qualificationLevel = qualificationLevel; }
    
    public String getCreditRating() { return creditRating; }
    public void setCreditRating(String creditRating) { this.creditRating = creditRating; }
    
    public String getBusinessScope() { return businessScope; }
    public void setBusinessScope(String businessScope) { this.businessScope = businessScope; }
    
    public String getTaxIdentificationNumber() { return taxIdentificationNumber; }
    public void setTaxIdentificationNumber(String taxIdentificationNumber) { this.taxIdentificationNumber = taxIdentificationNumber; }
    
    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    
    public List<Project> getProjects() { return projects; }
    public void setProjects(List<Project> projects) { this.projects = projects; }
    
    public List<SubcontractorContract> getContracts() { return contracts; }
    public void setContracts(List<SubcontractorContract> contracts) { this.contracts = contracts; }
    
    public List<String> getProjectNames() { return projectNames; }
    public void setProjectNames(List<String> projectNames) { this.projectNames = projectNames; }
    
    public Integer getContractCount() { return contractCount; }
    public void setContractCount(Integer contractCount) { this.contractCount = contractCount; }
}
