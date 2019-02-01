package io.taucoin.net.rlpx;

import io.taucoin.net.rlpx.NodeType;
import io.taucoin.crypto.ECKey;
import io.taucoin.datasource.mapdb.Serializers;
import io.taucoin.db.ByteArrayWrapper;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;
import io.taucoin.util.Utils;
import org.mapdb.Serializer;
import org.spongycastle.util.encoders.Hex;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static io.taucoin.crypto.HashUtil.sha3;
import static io.taucoin.util.ByteUtil.byteArrayToInt;

public class Node implements Serializable {
    private static final long serialVersionUID = -4267600517925770636L;

    public static final Serializer<Node> MapDBSerializer = new Serializer<Node>() {
        @Override
        public void serialize(DataOutput out, Node value) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(value);
            oos.close();
            Serializers.BYTE_ARRAY_WRAPPER.serialize(out, new ByteArrayWrapper(baos.toByteArray()));
        }

        @Override
        public Node deserialize(DataInput in, int available) throws IOException {
            ByteArrayWrapper bytes = Serializers.BYTE_ARRAY_WRAPPER.deserialize(in, available);
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes.getData());
            ObjectInputStream ois = new ObjectInputStream(bais);
            try {
                return (Node) ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } finally {
                ois.close();
            }
        }
    };

    byte[] id;
    String host;
    int port;
    NodeType type;
    // discovery endpoint doesn't have real nodeId for example
    private boolean isFakeNodeId = false;

    /**
     *  - create Node instance from enode if passed,
     *  - otherwise fallback to random nodeId, if supplied with only "address:port"
     * NOTE: validation is absent as method is not heavily used
     */
    public static Node instanceOf(String addressOrEnode) {
        try {
            URI uri = new URI(addressOrEnode);
            if (uri.getScheme().equals("enode")) {
                return new Node(addressOrEnode);
            }
        } catch (URISyntaxException e) {
            // continue
        }

        final ECKey generatedNodeKey = ECKey.fromPrivate(sha3(addressOrEnode.getBytes()));
        final String generatedNodeId = Hex.toHexString(generatedNodeKey.getNodeId());
        final Node node = new Node("enode://" + generatedNodeId + "@" + addressOrEnode);
        node.isFakeNodeId = true;
        return node;
    }

    public Node(String enodeURL) {
        try {
            URI uri = new URI(enodeURL);
            if (!uri.getScheme().equals("enode")) {
                throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT");
            }
            this.id = Hex.decode(uri.getUserInfo());
            this.host = uri.getHost();
            this.port = uri.getPort();
            this.type = NodeType.UNKNOWN;
        } catch (URISyntaxException e) {
            throw new RuntimeException("expecting URL in the format enode://PUBKEY@HOST:PORT", e);
        }
    }

    public Node(byte[] id, String host, int port, NodeType type) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.type = type;
    }

    public Node(byte[] id, String host, int port) {
        this(id, host, port, NodeType.UNKNOWN);
    }

    public Node(byte[] rlp) {

        RLPList nodeRLP = RLP.decode2(rlp);
        nodeRLP = (RLPList) nodeRLP.get(0);

        byte[] hostB = nodeRLP.get(0).getRLPData();
        byte[] portB = nodeRLP.get(1).getRLPData();
        byte[] idB;
        byte[] typeB;

        if (nodeRLP.size() > 3) {
            idB = nodeRLP.get(3).getRLPData();
            typeB = nodeRLP.get(4).getRLPData();
        } else {
            idB = nodeRLP.get(2).getRLPData();
            typeB = nodeRLP.get(3).getRLPData();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(hostB[0] & 0xFF);
        sb.append(".");
        sb.append(hostB[1] & 0xFF);
        sb.append(".");
        sb.append(hostB[2] & 0xFF);
        sb.append(".");
        sb.append(hostB[3] & 0xFF);

//        String host = new String(hostB, Charset.forName("UTF-8"));
        String host = sb.toString();
        int port = byteArrayToInt(portB);

        this.host = host;
        this.port = port;
        this.id = idB;
        this.type = NodeType.fromCode((int) typeB[0]);
    }


    public byte[] getId() {
        return id;
    }

    public String getHexId() {
        return Hex.toHexString(id);
    }

    public String getHexIdShort() {
        return Utils.getNodeIdShort(getHexId());
    }

    public void setId(byte[] id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    /**
     * @return true if this node is endpoint for discovery loaded from config
     */
    public boolean isDiscoveryNode() {
        return isFakeNodeId;
    }

    public void setDiscoveryNode(boolean isDiscoveryNode) {
        isFakeNodeId = isDiscoveryNode;
    }

    /**
     * Full RLP
     * [host, udpPort, tcpPort, nodeId, nodeType]
     * @return RLP-encoded node data
     */
    public byte[] getRLP() {

        byte[] rlphost = RLP.encodeElement(host.getBytes(StandardCharsets.UTF_8));
        byte[] rlpTCPPort = RLP.encodeInt(port);
        byte[] rlpUDPPort = RLP.encodeInt(port);
        byte[] rlpId = RLP.encodeElement(id);
        byte[] rlpNodeType = RLP.encodeElement(new byte[]{type.getCode()});

        byte[] data = RLP.encodeList(rlphost, rlpUDPPort, rlpTCPPort, rlpId, rlpNodeType);
        return data;
    }

    /**
     * RLP without nodeId
     * [host, udpPort, tcpPort, nodeType]
     * @return RLP-encoded node data
     */
    public byte[] getBriefRLP() {

        byte[] rlphost = RLP.encodeElement(host.getBytes(StandardCharsets.UTF_8));
        byte[] rlpTCPPort = RLP.encodeInt(port);
        byte[] rlpUDPPort = RLP.encodeInt(port);
        byte[] rlpNodeType = RLP.encodeElement(new byte[]{type.getCode()});

        byte[] data = RLP.encodeList(rlphost, rlpUDPPort, rlpTCPPort, rlpNodeType);
        return data;
    }


    @Override
    public String toString() {
        return "Node{" +
                " host='" + host + '\'' +
                ", port=" + port +
                ", id=" + Hex.toHexString(id) +
                ", type=" + type.toString() +
                '}';
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (o instanceof Node) {
            return Arrays.equals(((Node) o).getId(), this.getId());
        }

        return false;
    }
}
