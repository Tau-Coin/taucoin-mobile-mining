package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;

public class net_listening extends JsonRpcServerMethod {

    public net_listening (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        Boolean tmp = true;
        JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
        return res;

    }
}