package io.taucoin.android.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.android.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.core.Blockchain;
import io.taucoin.facade.Taucoin;
import java.util.List;

public class tau_getTransactionByHash extends JsonRpcServerMethod {

    public tau_getTransactionByHash (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            byte[] address = jsToAddress((String) params.get(0));
            
            /*
            Blockchain blockchain = (Blockchain)taucoin.getBlockchain();
            TransactionReceipt transaction = blockchain.getTransactionReceiptByHash(address);

            if (transaction == null)
                return new JSONRPC2Response(null, req.getID());

            JSONRPC2Response res = new JSONRPC2Response(transactionToJS(null, transaction.getTransaction()), req.getID());
            */
            JSONRPC2Response res = new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
            return res;
        }

    }
}
