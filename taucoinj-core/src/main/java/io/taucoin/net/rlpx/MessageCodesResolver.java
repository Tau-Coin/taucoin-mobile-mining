package io.taucoin.net.rlpx;

import io.taucoin.net.client.Capability;
import io.taucoin.net.tau.TauVersion;
import io.taucoin.net.tau.message.TauMessageCodes;
import io.taucoin.net.p2p.P2pMessageCodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.taucoin.net.tau.TauVersion.*;

/**
 * @author Mikhail Kalinin
 * @since 14.07.2015
 */
public class MessageCodesResolver {

    private Map<String, Integer> offsets = new HashMap<>();

    public MessageCodesResolver() {
    }

    public MessageCodesResolver(List<Capability> caps) {
        init(caps);
    }

    public void init(List<Capability> caps) {
        Collections.sort(caps);
        int offset = P2pMessageCodes.USER.asByte() + 1;

        for (Capability capability : caps) {
            if (capability.getName().equals(Capability.TAU)) {
                setEthOffset(offset);
                TauVersion v = fromCode(capability.getVersion());
                offset += TauMessageCodes.values(v).length;
            }
        }
    }

    public byte withP2pOffset(byte code) {
        return withOffset(code, Capability.P2P);
    }

    public byte withEthOffset(byte code) {
        return withOffset(code, Capability.TAU);
    }

    public byte withOffset(byte code, String cap) {
        byte offset = getOffset(cap);
        return (byte)(code + offset);
    }

    public byte resolveP2p(byte code) {
        return resolve(code, Capability.P2P);
    }

    public byte resolveEth(byte code) {
        return resolve(code, Capability.TAU);
    }

    private byte resolve(byte code, String cap) {
        byte offset = getOffset(cap);
        return (byte)(code - offset);
    }

    private byte getOffset(String cap) {
        Integer offset = offsets.get(cap);
        return offset == null ? 0 : offset.byteValue();
    }

    public void setEthOffset(int offset) {
        setOffset(Capability.TAU, offset);
    }

    private void setOffset(String cap, int offset) {
        offsets.put(cap, offset);
    }
}
