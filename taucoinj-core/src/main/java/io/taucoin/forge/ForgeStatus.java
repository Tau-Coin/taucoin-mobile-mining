package io.taucoin.forge;

public enum  ForgeStatus {

    FORGE_NORMAL(1, "forging normal running"),
    FORGE_NORMAL_EXIT(2, "forging normal exit"),
    FORGE_POWER_LESS_THAN_ZERO(3, "forge power is less than 0"),
    BALANCE_LESS_THAN_HISTORY_FEE(4, "balance less than history average fee"),;

    private int code;
    private String msg;
    ForgeStatus(int code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
