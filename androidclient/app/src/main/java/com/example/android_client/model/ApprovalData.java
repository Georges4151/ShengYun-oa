package com.example.android_client.model;

import java.util.Map;

public class ApprovalData {
    private Map<String, Object> list;
    private int total;

    public Map<String, Object> getList() {
        return list;
    }

    public void setList(Map<String, Object> list) {
        this.list = list;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
