package io.taucoin.net.rlpx;

import io.taucoin.crypto.ECKey;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPItem;
import io.taucoin.util.RLPList;
import org.spongycastle.util.encoders.Hex;

import static io.taucoin.util.ByteUtil.longToBytesNoLeadZeroes;

public class FindNodeMessage extends Message {

    byte[] target;
    long expires;

    @Override
    public void parse(byte[] data) {

        RLPList list = RLP.decode2(data);
        list = (RLPList) list.get(0);

        RLPItem target = (RLPItem) list.get(0);
        RLPItem expires = (RLPItem) list.get(1);

        this.target = target.getRLPData();
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());
    }


    public static FindNodeMessage create(byte[] target, ECKey privKey) {

        long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        /* RLP Encode data */
        byte[] rlpToken = RLP.encodeElement(target);

        byte[] rlpExp = longToBytesNoLeadZeroes(expiration);
        rlpExp = RLP.encodeElement(rlpExp);

        byte[] type = new byte[]{3};
        byte[] data = RLP.encodeList(rlpToken, rlpExp);

        FindNodeMessage findNode = new FindNodeMessage();
        findNode.encode(type, data, privKey);
        findNode.target = target;
        findNode.expires = expiration;

        return findNode;
    }

    public byte[] getTarget() {
        return target;
    }

    public long getExpires() {
        return expires;
    }

    @Override
    public String toString() {

        long currTime = System.currentTimeMillis() / 1000;

        String out = String.format("[FindNodeMessage] \n target: %s \n expires in %d seconds \n %s\n",
                Hex.toHexString(target), (expires - currTime), super.toString());

        return out;
    }

}
