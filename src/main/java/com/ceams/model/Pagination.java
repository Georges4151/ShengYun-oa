package com.ceams.model;

public class Pagination {
    private int pageNum;
    private int pageSize;
    private String keyword;
    
    public Pagination() {
        this.pageNum = 1;
        this.pageSize = 10;
    }
    
    public Pagination(int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
    
    public Pagination(int pageNum, int pageSize, String keyword) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.keyword = keyword;
    }
    
    public int getPageNum() {
        return pageNum;
    }
    
    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public String getKeyword() {
        return keyword;
    }
    
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }
}