package io.taucoin.core;

public final class Constants {

    //max block tx number
    public static final int MAX_BLOCKTXSIZE = 50;
    //transaction expiration height
    public static final int TX_EXPIRATIONHEIGHT = 144;
    //forge block time interval
    public static final int BLOCKTIMEINTERVAL = 300;
    //block time drift
    public static final int MAX_TIMEDRIFT = 60; // allow up to 60 s clock difference

    public static final String GENESIS_BLOCK_HASH = "fbc608d4e9a8c31576df9065d55289465c4be9f7";
}
