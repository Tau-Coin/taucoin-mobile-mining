package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.core.AccountState;
import io.taucoin.core.Block;
import io.taucoin.facade.Taucoin;
import org.spongycastle.util.encoders.Hex;
import java.math.BigInteger;
import java.util.List;

public class tau_getBlockByNumber extends JsonRpcServerMethod {

    public tau_getBlockByNumber (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 2) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            long blockNumber = (long)params.get(0);
            Boolean detailed = (Boolean)params.get(1);

            Block block = taucoin.getBlockchain().getBlockByNumber(blockNumber);

            JSONRPC2Response res = new JSONRPC2Response(blockToJS(block, detailed), req.getID());
            return res;
        }

    }
}
