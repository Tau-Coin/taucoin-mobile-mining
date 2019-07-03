package io.taucoin.android.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;

import net.minidev.json.JSONValue;

import io.taucoin.android.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;
import io.taucoin.core.Transaction;
import io.taucoin.core.PendingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class tau_getTransactions extends JsonRpcServerMethod {

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public tau_getTransactions(Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        ArrayList<String> tmpTransactions = new ArrayList<String>();

        if (taucoin.getWireTransactions().size() > 0){
            for(Transaction tx: taucoin.getWireTransactions()) {
               tmpTransactions.add(Hex.toHexString(tx.getHash()));
            }
        }

        if (tmpTransactions.size() == 0) {
            tmpTransactions.add("Transactions: NULL");
        }

        JSONRPC2Response res = new JSONRPC2Response(tmpTransactions, req.getID());
        return res;
    }
}
