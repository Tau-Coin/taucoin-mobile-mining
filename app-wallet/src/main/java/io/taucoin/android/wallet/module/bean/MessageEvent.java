package io.taucoin.android.wallet.module.bean;

public class MessageEvent{

    public enum EventCode {
        ALL,
        BALANCE,
        NICKNAME,
        TRANSACTION,
        UPGRADE,
        BLOCK_HEIGHT,
        MINING_INFO,
        MINING_SYNC,
        MINING_REWARD,
        MINING_NOTIFY,
        MINING_STATE,
        GET_BLOCK,
        GET_BLOCK_LIST,
        CONSOLE_LOG,
        CLEAR_SEND,
        FORGED_TIME,
        FORGED_POT_DETAIL,
        APPLICATION_INFO,
        MINING_INCOME,
        IRREPARABLE_ERROR,
        APP_BACK_TO_FRONT,
        SWITCH_STOP_MINING
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