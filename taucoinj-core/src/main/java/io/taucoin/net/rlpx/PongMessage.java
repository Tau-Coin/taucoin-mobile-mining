package io.taucoin.net.rlpx;

import io.taucoin.crypto.ECKey;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPItem;
import io.taucoin.util.RLPList;
import org.spongycastle.util.encoders.Hex;

//import java.nio.charset.Charset;

import static io.taucoin.util.ByteUtil.longToBytes;
import static io.taucoin.util.ByteUtil.stripLeadingZeroes;

public class PongMessage extends Message {

    byte[] token; // token is the MDC of the ping
    long expires;

    public static PongMessage create(byte[] token, String host, int port, ECKey privKey) {

        long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        byte[] rlpIp = RLP.encodeElement(host.getBytes());

        byte[] tmpPort = longToBytes(port);
        byte[] rlpPort = RLP.encodeElement(stripLeadingZeroes(tmpPort));
        byte[] rlpToList = RLP.encodeList(rlpIp, rlpPort, rlpPort);

        /* RLP Encode data */
        byte[] rlpToken = RLP.encodeElement(token);
        byte[] tmpExp = longToBytes(expiration);
        byte[] rlpExp = RLP.encodeElement(stripLeadingZeroes(tmpExp));

        byte[] type = new byte[]{2};
        byte[] data = RLP.encodeList(rlpToList, rlpToken, rlpExp);

        PongMessage pong = new PongMessage();
        pong.encode(type, data, privKey);

        pong.token = token;
        pong.expires = expiration;

        return pong;
    }

    public static PongMessage create(byte[] token, ECKey privKey) {
        return create(token, privKey, 3 + System.currentTimeMillis() / 1000);
    }

    static PongMessage create(byte[] token, ECKey privKey, long expiration) {

        /* RLP Encode data */
        byte[] rlpToken = RLP.encodeElement(token);
        byte[] rlpExp = RLP.encodeElement(ByteUtil.longToBytes(expiration));

        byte[] type = new byte[]{2};
        byte[] data = RLP.encodeList(rlpToken, rlpExp);

        PongMessage pong = new PongMessage();
        pong.encode(type, data, privKey);

        pong.token = token;
        pong.expires = expiration;

        return pong;
    }


    @Override
    public void parse(byte[] data) {
        RLPList list = RLP.decode2(data);
        list = (RLPList) list.get(0);

        this.token = list.get(0).getRLPData();
        RLPItem expires = (RLPItem) list.get(1);
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());
    }


    public byte[] getToken() {
        return token;
    }

    public long getExpires() {
        return expires;
    }

    @Override
    public String toString() {
        long currTime = System.currentTimeMillis() / 1000;

        String out = String.format("[PongMessage] \n token: %s \n expires in %d seconds \n %s\n",
                Hex.toHexString(token), (expires - currTime), super.toString());

        return out;
    }
}
