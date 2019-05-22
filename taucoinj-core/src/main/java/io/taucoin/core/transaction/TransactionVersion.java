package io.taucoin.core.transaction;

/**
 * Represents taucoin transaction versions
 */
public enum TransactionVersion {

    V01((byte) 0x00);

    private byte code;

    TransactionVersion(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static TransactionVersion fromCode(int code) {
        for (TransactionVersion v : values()) {
            if (v.code == code) {
                return v;
            }
        }

        return null;
    }
}

