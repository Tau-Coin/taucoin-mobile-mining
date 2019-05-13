package io.taucoin.forge;

public class  ForgeStatus {

    public static ForgeStatus FORGE_NORMAL = new ForgeStatus(1, "forging normal running");
    public static ForgeStatus FORGE_NORMAL_EXIT = new ForgeStatus(2, "forging normal exit");
    public static ForgeStatus FORGE_POWER_LESS_THAN_ZERO = new ForgeStatus(3, "Mining power is less than 0");
    public static ForgeStatus BALANCE_LESS_THAN_HISTORY_FEE = new ForgeStatus(4, "Address balance shall be larger than average block transaction fee ");
    public static ForgeStatus FORGE_TASK_INTERRUPTED_NOT_SYNCED
                              = new ForgeStatus(5,"Forging task is interrupted when waiting for sync done");
    public static ForgeStatus BLOCK_SYNC_PROCESSING = new ForgeStatus(6,"wait to block sync completed");
    public static ForgeStatus FORGE_TASK_INTERRUPTED = new ForgeStatus(7,"Forging task is interrupted");
    public static ForgeStatus PULL_POOL_TX_TIMEOUT = new ForgeStatus(8,"Pull pool tx timeout, retry again");
    public static ForgeStatus FORGE_CONTINUE = new ForgeStatus(9,"Got a new best block, continue forging...");
    public static ForgeStatus FORGE_INTERRUPTED_OR_CANCELED
                              = new ForgeStatus(10,"OK, we've been cancelled, just exit");
    public static ForgeStatus EXCEPTION_DURING_FORGING = new ForgeStatus(11,"Exception during mining");

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

    public boolean isContinue(){
        if (this.getCode() == 2 || this.getCode() == 3
        || this.getCode() == 4 || this.getCode() == 5
        || this.getCode() == 7 || this.getCode() == 10
        || this.getCode() == 11) {
            return false;
        }
        return true;
    }
}
