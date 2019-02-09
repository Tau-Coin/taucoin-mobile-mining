package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;

/*
TODO: maybe in future AdminState will have information about this.
*/
public class net_version extends JsonRpcServerMethod {

    public net_version (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        String tmp = "";
        JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
        return res;
    }

}