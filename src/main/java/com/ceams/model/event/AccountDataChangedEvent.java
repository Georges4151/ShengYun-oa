package com.ceams.model.event;

import org.springframework.context.ApplicationEvent;

/**
 * 账目数据发生变化事件
 * 当账目相关数据（如收支记录、票据、交易等）发生变化时发布此事件
 */
public class AccountDataChangedEvent extends ApplicationEvent {

    /**
     * 创建一个新的事件实例
     * 
     * @param source 事件源
     */
    public AccountDataChangedEvent(Object source) {
        super(source);
    }

    /**
     * 创建一个带消息的事件实例
     * 
     * @param source  事件源
     * @param message 事件消息
     */
    public AccountDataChangedEvent(Object source, String message) {
        super(source);
        this.message = message;
    }

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}