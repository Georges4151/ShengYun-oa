package com.ceams.module.notification.enums;

/**
 * 通知优先级。
 *
 * <p>{@code level} 用于免打扰阈值比较：数值越大优先级越高。
 * 判断「priority 不高于 threshold」即 {@code priority.getLevel() <= threshold.getLevel()}。</p>
 */
public enum NotificationPriority {

    /** 低。 */
    LOW(1),

    /** 普通。 */
    NORMAL(2),

    /** 高。 */
    HIGH(3),

    /** 紧急。 */
    URGENT(4);

    private final int level;

    NotificationPriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
