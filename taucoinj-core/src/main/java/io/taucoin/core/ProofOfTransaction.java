package io.taucoin.core;

import io.taucoin.db.BlockStore;

import java.math.BigInteger;

import static java.lang.Math.abs;
import static java.lang.Math.log;

public class ProofOfTransaction {
    private final static int MAXRATIO = 67;
    private final static int MINRATIO = 53;
    private final static double GAMMA = 0.64;
    private final static BigInteger DiffAdjustNumerator = new BigInteger("010000000000000000",16);
    private final static BigInteger DiffAdjustNumeratorHalf = new BigInteger("0100000000",16);
    private final static BigInteger DiffAdjustNumeratorCoe = new BigInteger("800000000000000",16); //2^59


    /*
        get required base target
     */
    public static BigInteger calculateRequiredBaseTarget(Block block, BlockStore blockStore) {
        long blockNumber = block.getNumber();
        if(blockNumber <= 3) {
            return (new BigInteger("369D0369D036978",16));
        }

        Block block2 = blockStore.getChainBlockByNumber(blockNumber - 1);
        Block block3 = blockStore.getChainBlockByNumber(blockNumber - 2);
        BigInteger lastBlockbaseTarget = block2.getBaseTarget();
        long pastTimeFromLatestBlock = new BigInteger(block3.getTimestamp()).longValue() -
                new BigInteger(block.getTimestamp()).longValue();

        if (pastTimeFromLatestBlock < 0)
            pastTimeFromLatestBlock = 0;
        long pastTimeAver = pastTimeFromLatestBlock/3;

        BigInteger newRequiredBaseTarget;
        if( pastTimeAver > 60 ) {
            long min = 0;
            if (pastTimeAver < MAXRATIO){
                min = pastTimeAver;
            }else {
                min = MAXRATIO;
            }
            newRequiredBaseTarget = lastBlockbaseTarget.multiply(BigInteger.valueOf(min).divide(BigInteger.valueOf(60)));
        }else{
            long max = 0;

            if (pastTimeAver > MINRATIO){
                max = pastTimeAver;
            }else{
                max = MINRATIO;
            }
            //if GAMMA=0.64 then devide 60 ,when 64 then 6000
            //double doubletemp =  (60-max)*GAMMA/60;
            //long temp = Math.round(1000*doubletemp);
            //this.newRequiredBaseTarget = lastBlockbaseTarget.subtract(BigInteger.valueOf(temp).multiply(lastBlockbaseTarget).divide(BigInteger.valueOf(1000)));
            newRequiredBaseTarget = lastBlockbaseTarget.subtract(lastBlockbaseTarget.divide(BigInteger.valueOf(375)).multiply(BigInteger.valueOf(60-max)).multiply(BigInteger.valueOf(4)));
        }
        return newRequiredBaseTarget;
    }

    /*
        get next block generation signature
        Gn+1 = hash(Gn, pubkey)
     */
    public static byte[] calculateNextBlockGenerationSignature(byte[] generationSignature, byte[] pubkey){
        byte[] data = new byte[generationSignature.length + pubkey.length];
        System.arraycopy(generationSignature, 0, data, 0, generationSignature.length);
        System.arraycopy(pubkey, 0, data, generationSignature.length, pubkey.length);
        byte[] nextGenerationSignature = Sha256Hash.hash(data);
        return nextGenerationSignature;
    }

    /*
        get miner target value
        target = base target * mining power * time
     */
    public static BigInteger calculateMinerTargetValue(BigInteger baseTarget, BigInteger miningPower, long time){
        BigInteger targetValue = baseTarget.multiply(miningPower).
                multiply(BigInteger.valueOf(time));
        return targetValue;
    }

    /*
        get hit value
     */
    public static BigInteger calculateRandomHit(byte[] generationSignature){
        byte[] headBytes = new byte[8];
        /*
        for (int i = 0; i < 8; i++) {
            headBytes[i] = temp[i];
        }*/
        System.arraycopy(generationSignature,0,headBytes,0,8);
        BigInteger bhit = new BigInteger(headBytes);
        BigInteger bhitUzero = bhit.add(BigInteger.ONE);
        double logarithm = log(bhitUzero.doubleValue()) - 2 * log(DiffAdjustNumeratorHalf.doubleValue());
        logarithm = abs(logarithm);
        long ulogarithm = (new Double(logarithm*1000)).longValue();
        BigInteger adjustHit = DiffAdjustNumeratorCoe.multiply(BigInteger.valueOf(ulogarithm)).divide(BigInteger.valueOf(1000));
        return adjustHit;
    }

    public static BigInteger calculateCumulativeDifficulty(BigInteger lastCumulativeDifficulty, BigInteger baseTarget){
        BigInteger delta = DiffAdjustNumerator.divide(baseTarget);
        BigInteger cumulativeDifficulty = lastCumulativeDifficulty.add(delta);
        return cumulativeDifficulty;
    }

}
