package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;
import io.taucoin.net.tau.TauVersion;
import io.taucoin.net.tau.handler.TauHandler;

public class tau_protocolVersion extends JsonRpcServerMethod {

    public tau_protocolVersion (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        String tmp = Byte.toString(TauVersion.LOWER);
        JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
        return res;

    }
}