package io.taucoin.net.rlpx;

import io.taucoin.net.rlpx.NodeType;
import io.taucoin.crypto.ECKey;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPItem;
import io.taucoin.util.RLPList;

import static io.taucoin.util.ByteUtil.bytesToIp;
import static io.taucoin.util.ByteUtil.longToBytes;
import static io.taucoin.util.ByteUtil.stripLeadingZeroes;

public class PingMessage extends Message {

    String toHost;
    int toPort;
    String fromHost;
    int fromPort;
    NodeType fromType;
    long expires;
    int version;

    public static PingMessage create(Node fromNode, Node toNode, ECKey privKey) {
        return create(fromNode, toNode, privKey, 4);
    }

    public static PingMessage create(Node fromNode, Node toNode, ECKey privKey, int version) {

        long expiration = 90 * 60 + System.currentTimeMillis() / 1000;

        /* RLP Encode data */
        byte[] tmpExp = longToBytes(expiration);
        byte[] rlpExp = RLP.encodeElement(stripLeadingZeroes(tmpExp));

        byte[] type = new byte[]{1};
        byte[] rlpVer = RLP.encodeInt(version);
        byte[] rlpFromList = fromNode.getBriefRLP();
        byte[] rlpToList = toNode.getBriefRLP();
        byte[] data = RLP.encodeList(rlpVer, rlpFromList, rlpToList, rlpExp);

        PingMessage ping = new PingMessage();
        ping.encode(type, data, privKey);

        ping.expires = expiration;
        ping.toHost = toNode.getHost();
        ping.toPort = toNode.getPort();
        ping.fromHost = fromNode.getHost();
        ping.fromPort = fromNode.getPort();
        ping.fromType = fromNode.getType();

        return ping;
    }

    @Override
    public void parse(byte[] data) {

        RLPList dataList = (RLPList) RLP.decode2OneItem(data, 0);

        RLPList fromList = (RLPList) dataList.get(1);
        byte[] ipF = fromList.get(0).getRLPData();
        this.fromHost = bytesToIp(ipF);
        this.fromPort = ByteUtil.byteArrayToInt(fromList.get(1).getRLPData());
        this.fromType = NodeType.fromCode((int) (fromList.get(3).getRLPData()[0]));

        RLPList toList = (RLPList) dataList.get(2);
        byte[] ipT = toList.get(0).getRLPData();
        this.toHost = bytesToIp(ipT);
        this.toPort = ByteUtil.byteArrayToInt(toList.get(1).getRLPData());

        RLPItem expires = (RLPItem) dataList.get(3);
        this.expires = ByteUtil.byteArrayToLong(expires.getRLPData());

        this.version = ByteUtil.byteArrayToInt(dataList.get(0).getRLPData());
    }


    public String getToHost() {
        return toHost;
    }

    public int getToPort() {
        return toPort;
    }

    public String getFromHost() {
        return fromHost;
    }

    public int getFromPort() {
        return fromPort;
    }

    public NodeType getFromType() {
        return fromType;
    }

    public long getExpires() {
        return expires;
    }

    @Override
    public String toString() {

        long currTime = System.currentTimeMillis() / 1000;

        String out = String.format("[PingMessage] \n %s:%d %s ==> %s:%d \n expires in %d seconds \n %s\n",
                fromHost, fromPort, fromType.toString(), toHost, toPort, (expires - currTime), super.toString());

        return out;
    }
}
