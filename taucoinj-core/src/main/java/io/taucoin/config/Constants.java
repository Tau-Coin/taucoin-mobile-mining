package io.taucoin.config;

import java.math.BigInteger;

import static io.taucoin.config.SystemProperties.CONFIG;

public class Constants {
    
    public static BigInteger MINIMUM_DIFFICULTY = BigInteger.valueOf(131072);
    public static BigInteger DIFFICULTY_BOUND_DIVISOR = BigInteger.valueOf(2048);
    public static int EXP_DIFFICULTY_PERIOD = 100000;

    public static int UNCLE_GENERATION_LIMIT = 7;
    public static int UNCLE_LIST_LIMIT = 2;

    public static int BEST_NUMBER_DIFF_LIMIT = 100;

    public static final BigInteger SECP256K1N = new BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16);
    public static final BigInteger SECP256K1N_HALF = SECP256K1N.divide(BigInteger.valueOf(2));
    public static long HOMESTEAD_FORK_BLKNUM = 10_000_000;

    public static int getDURATION_LIMIT() {
        return CONFIG.isFrontier() ? 13 : 8;
    }
}
