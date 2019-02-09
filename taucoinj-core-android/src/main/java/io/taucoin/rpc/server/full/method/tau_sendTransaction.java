package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import net.minidev.json.JSONObject;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.core.Account;
import io.taucoin.core.Transaction;
import io.taucoin.facade.Taucoin;
import org.spongycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static io.taucoin.core.Denomination.SZABO;
import static io.taucoin.config.SystemProperties.CONFIG;


/*
TODO: get more information from Roman, he think about this right now about 20 - 32 result.
*/
public class tau_sendTransaction extends JsonRpcServerMethod {

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public tau_sendTransaction (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 1) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            JSONObject obj = (JSONObject)params.get(0);
            Transaction tx;
            try {
                tx = jsToTransaction(obj);
                // verify transaction
                if (!tx.validate()) {
                    throw new Exception("Invalid params");
                }
            } catch (Exception e) {
                e.printStackTrace();
                return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
            }

            try {
                taucoin.submitTransaction(tx);//.get(CONFIG.transactionApproveTimeout(), TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
                return new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR, req.getID());
            }

            String tmp = "0x" + Hex.toHexString(tx.getHash());
            JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
            return res;
        }

    }
}