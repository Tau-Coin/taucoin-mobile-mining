package io.taucoin.android.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.android.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;

public class tau_blockNumber extends JsonRpcServerMethod {

    public tau_blockNumber (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        String tmp = "0x" + Long.toHexString(taucoin.getBlockchain().getBestBlock().getNumber());
        JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
        return res;

    }
}