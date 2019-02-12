package io.taucoin.android.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import io.taucoin.config.MainNetParams;
import io.taucoin.core.Address;
import io.taucoin.core.DumpedPrivateKey;
import io.taucoin.facade.Taucoin;
import io.taucoin.crypto.ECKey;
import io.taucoin.android.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.core.Base58;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class tau_importprikey extends JsonRpcServerMethod {
    private static final Logger log = LoggerFactory.getLogger("rpc");
    public tau_importprikey (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() < 1 ) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        }
        ECKey key;
        String prikey = Hex.toHexString(jsToByteArray((String)params.get(0)));
        log.info("privkey is {}",prikey);

        if (prikey.length() == 51 || prikey.length() == 52) {
            DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(MainNetParams.get(),prikey);
            key = dumpedPrivateKey.getKey();
        } else {
            BigInteger privKey = Base58.decodeToBigInteger(prikey);
            key = ECKey.fromPrivate(privKey);
        }
        log.info("Address from private key is:{}",new Address(MainNetParams.get(),key.getAddress()).toString());
        log.info("canonical Address  is:{}", Hex.toHexString(key.getAddress()));
        taucoin.getWallet().importKey(jsToByteArray((String)params.get(0)));
        JSONRPC2Response res = new JSONRPC2Response(req.getID());
        return res;

    }
}
