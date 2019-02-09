package io.taucoin.android.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import io.taucoin.core.Account;
import io.taucoin.facade.Taucoin;
import io.taucoin.android.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.util.ByteUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class tau_importprikey extends JsonRpcServerMethod {
    public tau_importprikey (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() < 1 ) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        }
        taucoin.getWallet().importKey(ByteUtil.bigIntegerToBytes(new BigInteger((String)params.get(0))));
        JSONRPC2Response res = new JSONRPC2Response(req.getID());
        return res;

    }
}
