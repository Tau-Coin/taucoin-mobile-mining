package io.taucoin.config;

import java.math.BigInteger;

import static io.taucoin.config.SystemProperties.CONFIG;

public class Constants {

    //max block tx number
    public static final int MAX_BLOCKTXSIZE = 50;
    //transaction expiration height
    public static final int TX_EXPIRATIONHEIGHT = 144;
    //forge block time interval
    public static final int BLOCKTIMEINTERVAL = 300;
    //block time drift
    public static final int MAX_TIMEDRIFT = 15; // allow up to 15 s clock difference

    public static final String GENESIS_BLOCK_HASH = "0e19e167b7160f7ce173dbe8dbf50d2266365907";

    public static BigInteger MINIMUM_DIFFICULTY = BigInteger.valueOf(131072);
    public static BigInteger DIFFICULTY_BOUND_DIVISOR = BigInteger.valueOf(2048);
    public static int EXP_DIFFICULTY_PERIOD = 100000;

    public static final BigInteger SECP256K1N = new BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16);
    public static final BigInteger SECP256K1N_HALF = SECP256K1N.divide(BigInteger.valueOf(2));
    public static final String BURN_COIN_ADDR = "0000000000000000000000000000000000000000";
}
