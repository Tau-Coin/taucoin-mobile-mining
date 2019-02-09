package io.taucoin.android.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.android.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;

/*
Deprecated
*/
public class db_putString extends JsonRpcServerMethod {

    public db_putString (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        JSONRPC2Response res = new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
        return res;

    }
}