package io.taucoin.android.wallet.module.bean;

public class MessageEvent{

    public enum EventCode {
        ALL,
        BALANCE,
        NICKNAME,
        TRANSACTION,
        UPGRADE,
        BLOCK_HEIGHT,
        MINING_INIT,
        MINING_INFO,
        MINING_STATE,
        GET_BLOCK,
        GET_BLOCK_LIST,
        CONSOLE_LOG,
        CLEAR_SEND,
        FORGED_TIME,
        NOTIFY_MINING,
        MINING_REWARD,
        APPLICATION_INFO
    }
    private Object data;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public EventCode getCode() {
        return code;
    }

    public void setCode(EventCode code) {
        this.code = code;
    }

    private EventCode code;
}