package io.taucoin.net.tau;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents supported Tau versions
 *
 * @author Mikhail Kalinin
 * @since 14.08.2015
 */
public enum TauVersion {

    V60((byte) 60),
    V61((byte) 61),
    V62((byte) 62);

    public static final byte LOWER = V60.getCode();
    public static final byte UPPER = V62.getCode();

    private byte code;

    TauVersion(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static TauVersion fromCode(int code) {
        for (TauVersion v : values()) {
            if (v.code == code) {
                return v;
            }
        }

        return null;
    }

    public static boolean isSupported(byte code) {
        return code >= LOWER && code <= UPPER;
    }

    public static List<TauVersion> supported() {
        List<TauVersion> supported = new ArrayList<>();
        for (TauVersion v : values()) {
            if (isSupported(v.code)) {
                supported.add(v);
            }
        }

        return supported;
    }

    public boolean isCompatible(TauVersion version) {

        if (version.getCode() >= V62.getCode()) {
            return this.getCode() >= V62.getCode();
        } else {
            return this.getCode() < V62.getCode();
        }
    }
}
