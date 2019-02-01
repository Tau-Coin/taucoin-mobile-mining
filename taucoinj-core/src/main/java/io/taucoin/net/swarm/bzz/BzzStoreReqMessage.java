package io.taucoin.net.swarm.bzz;

import io.taucoin.net.client.Capability;
import io.taucoin.net.swarm.Key;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPElement;
import io.taucoin.util.RLPList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BzzStoreReqMessage extends BzzMessage {

    private Key key;
    private byte[] data;

    // optional
    byte[] metadata = new byte[0];

    public BzzStoreReqMessage(byte[] encoded) {
        super(encoded);
    }

    public BzzStoreReqMessage(long id, Key key, byte[] data) {
        this.id = id;
        this.key = key;
        this.data = data;
    }

    public BzzStoreReqMessage(Key key, byte[] data) {
        this.key = key;
        this.data = data;
    }

    @Override
    protected void decode() {
        RLPList paramsList = (RLPList) RLP.decode2(encoded).get(0);

        key = new Key(paramsList.get(0).getRLPData());
        data = paramsList.get(1).getRLPData();

        if (paramsList.size() > 2) {
            id = ByteUtil.byteArrayToLong(paramsList.get(2).getRLPData());
        }
        if (paramsList.size() > 3) {
            metadata = paramsList.get(2).getRLPData();
        }

        parsed = true;
    }

    private void encode() {
        List<byte[]> elems = new ArrayList<>();
        elems.add(RLP.encodeElement(key.getBytes()));
        elems.add(RLP.encodeElement(data));
//        if (id >= 0 || metadata != null) {
            elems.add(RLP.encodeInt((int) id));
//        }
//        if (metadata != null) {
            elems.add(RLP.encodeList(metadata));
//        }
        this.encoded = RLP.encodeList(elems.toArray(new byte[0][]));
    }

    @Override
    public byte[] getEncoded() {
        if (encoded == null) encode();
        return encoded;
    }

    @Override
    public Class<?> getAnswerMessage() {
        return null;
    }

    @Override
    public BzzMessageCodes getCommand() {
        return BzzMessageCodes.STORE_REQUEST;
    }

    public Key getKey() {
        return key;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "BzzStoreReqMessage{" +
                "key=" + key +
                ", data=" + Arrays.toString(data) +
                ", id=" + id +
                ", metadata=" + Arrays.toString(metadata) +
                '}';
    }
}
