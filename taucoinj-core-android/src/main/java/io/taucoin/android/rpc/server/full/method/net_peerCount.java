package io.taucoin.android.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.android.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;
import io.taucoin.net.peerdiscovery.PeerInfo;
import java.util.Set;

public class net_peerCount extends JsonRpcServerMethod {

    public net_peerCount (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        int pc = 0;
        final Set<PeerInfo> peers = taucoin.getPeers();
        synchronized (peers) {
            for (PeerInfo peer : peers) {
                if (peer.isOnline())
                    pc++;
            }
        }
        String tmp = "0x" + Integer.toHexString(pc);
        JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
        return res;

    }
}