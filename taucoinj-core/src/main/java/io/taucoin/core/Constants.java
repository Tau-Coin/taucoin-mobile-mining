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

    public static final String GENESIS_BLOCK_HASH = "0e19e167b7160f7ce173dbe8dbf50d2266365907";
}
