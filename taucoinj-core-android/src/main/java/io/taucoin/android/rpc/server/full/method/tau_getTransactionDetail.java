package io.taucoin.android.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import io.taucoin.android.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.core.Transaction;
import io.taucoin.facade.Taucoin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class tau_getTransactionDetail extends JsonRpcServerMethod {
    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public tau_getTransactionDetail (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        List<Object> params = req.getPositionalParams();
        if (params.size() < 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        }

        ArrayList<String> result = new ArrayList<String>();
        Transaction tx = new Transaction(jsToByteArray((String)params.get(0)), false);
        result.add("Hash: " + Hex.toHexString(tx.getHash()));
        result.add("TimeStamp: " + new BigInteger(1, tx.getTime()).toString());
        result.add("ExpireTime: " + new BigInteger(1, tx.getExpireTime()).toString());
        result.add("Sender Address: " + Hex.toHexString(tx.getSender()));
        result.add("Receiver Address: " + Hex.toHexString(tx.getReceiveAddress()));
        result.add("Amount: " + new BigInteger(1, tx.getAmount()).toString());
        result.add("Fee: " + new BigInteger(1, tx.getFee()).toString());

        JSONRPC2Response res = new JSONRPC2Response(result, req.getID());
        return res;
    }
}
