package io.taucoin.forge;

public enum  ForgeStatus {
    FORGE_NORMAL,
    FORGE_NORMAL_EXIT,
    FORGE_POWER_LESS_THAN_ZERO,
    BALANCE_LESS_THAN_HISTORY_FEE;
    public String explainStatus(){
        if (equals(FORGE_POWER_LESS_THAN_ZERO)) {
            return "forge power is less than 0";
        }
        if (equals(BALANCE_LESS_THAN_HISTORY_FEE)) {
            return "balance less than history average fee";
        }
        return "forging normal running";
    }
}
