package io.taucoin.forge;

import java.math.BigInteger;

/**
 * Next block forged POT info.
 * Include:
 *    Base Target
 *    Generation Signature
 *    Cumulative Difficulty
 *    Forging Power
 *    Hit Value
 */
public class NextBlockForgedDetail {

    private BigInteger baseTarget;
    private BigInteger generationSignature;
    private BigInteger cumulativeDifficulty;
    private BigInteger forgingPower;
    private BigInteger hitValue;
    private long timeInternal;
    private long timePoint;

    public NextBlockForgedDetail(BigInteger baseTarget, BigInteger generationSignature,
            BigInteger cumulativeDifficulty, BigInteger forgingPower, BigInteger hitValue,
            long timeInternal, long timePoint) {
        this.baseTarget = baseTarget;
        this.generationSignature = generationSignature;
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.forgingPower = forgingPower;
        this.hitValue = hitValue;
        this.timeInternal = timeInternal;
        this.timePoint = timePoint;
    }

    public BigInteger getBaseTarget() {
        return baseTarget;
    }

    public BigInteger getGenerationSignature() {
        return generationSignature;
    }

    public BigInteger getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }

    public BigInteger getForgingPower() {
        return forgingPower;
    }

    public BigInteger getHitValue() {
        return hitValue;
    }

    public long getTimeInternal() {
        return timeInternal;
    }

    public long getTimePoint() {
        return timePoint;
    }
}
