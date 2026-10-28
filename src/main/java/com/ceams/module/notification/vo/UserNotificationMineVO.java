package com.ceams.module.notification.vo;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 「我的通知」分页结果（含未读数）。
 */
@Data
public class UserNotificationMineVO {

    private List<UserNotificationVO> records;

    private long total;

    private long page;

    private long size;

    /** 当前用户未读数。 */
    private long unreadCount;

    public List<UserNotificationVO> getRecords() {
        return records == null ? Collections.emptyList() : records;
    }
}
