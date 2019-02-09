package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.core.AccountState;
import io.taucoin.core.Repository;
import io.taucoin.facade.Taucoin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import java.math.BigInteger;
import java.util.List;

public class tau_getBalance extends JsonRpcServerMethod {

    private static final Logger logger = LoggerFactory.getLogger("rpc");

    public tau_getBalance (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 2) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            byte[] address = jsToAddress((String) params.get(0));
            String height = (String)params.get(1);

            long blockNumber = getBlockNumber(height);
            logger.info("block Num is: {}", blockNumber) ;

            if (blockNumber >= 0) {
                Repository repository = (Repository)taucoin.getRepository();
            }

            BigInteger balance = taucoin.getRepository().getBalance(address);

            if (blockNumber == -2) {
                BigInteger tmpB = taucoin.getWallet().getBalance(address);
                balance = tmpB != BigInteger.ZERO ? tmpB : balance;
            }

            if (blockNumber >= 0) {
                Repository repository = (Repository)taucoin.getRepository();
            }

            String tmp = "0x" + balance.toString(16);
            JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
            return res;
        }

    }
}
