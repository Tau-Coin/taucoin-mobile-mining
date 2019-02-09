package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.core.Account;
import io.taucoin.crypto.ECKey;
import io.taucoin.facade.Taucoin;
import org.spongycastle.util.encoders.Hex;
import java.util.Arrays;
import java.util.List;
import static io.taucoin.util.ByteUtil.bigIntegerToBytes;


public class tau_sign extends JsonRpcServerMethod {

    public tau_sign (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        if (params.size() != 2) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        } else {
            byte[] address = jsToAddress((String) params.get(0));
            byte[] data =  jsToAddress((String) params.get(1));
            byte[] sigData = null;

            for (Account ac : taucoin.getWallet().getAccountCollection()) {
                if (Arrays.equals(ac.getAddress(), address)) {
                    ECKey.ECDSASignature sig = ac.getEcKey().doSign(data);
                    sigData = new byte[65];
                    sigData[0] = sig.v;
                    System.arraycopy(bigIntegerToBytes(sig.r, 32), 0, sigData, 1, 32);
                    System.arraycopy(bigIntegerToBytes(sig.s, 32), 0, sigData, 33, 32);
                    break;
                }
            }

            if (sigData == null) {
                return new JSONRPC2Response(JSONRPC2Error.INTERNAL_ERROR, req.getID());
            }

            String tmp = "0x" + Hex.toHexString(sigData);
            JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
            return res;
        }

    }
}