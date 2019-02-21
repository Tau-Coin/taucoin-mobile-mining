package io.taucoin.android.wallet.module.bean;

public class MessageEvent{

    public enum EventCode {
        ALL,
        BALANCE,
        NICKNAME,
        TRANSACTION,
        TRANSACTION_IMPORT,
        UPGRADE,
        BLOCK_HEIGHT,
        MINING_INFO,
        GET_BLOCK,
        GET_BLOCK_LIST
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