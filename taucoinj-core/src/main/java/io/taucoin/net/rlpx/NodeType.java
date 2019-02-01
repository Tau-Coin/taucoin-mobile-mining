package io.taucoin.net.rlpx;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents Node Type: Super node or mobile node
 */
public enum NodeType {

    UNKNOWN((byte) 0),
    SUPER((byte) 1),
    MOBILE((byte) 2);

    private byte code;

    NodeType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static NodeType fromCode(int code) {
        for (NodeType v : values()) {
            if (v.code == code) {
                return v;
            }
        }

        return null;
    }

    public String toString() {
        if (code == 0x00) return "Unkonwn";
        return  code == 0x02 ? "Mobile" : "Super";
    }
}
