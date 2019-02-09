package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;
import io.taucoin.core.*;
import org.spongycastle.util.encoders.Hex;
import java.util.ArrayList;
import java.util.Collection;

public class tau_accounts extends JsonRpcServerMethod {

    public tau_accounts (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        Collection<Account> accounts = taucoin.getWallet().getAccountCollection();
        ArrayList<String> tmp = new ArrayList<String>();
        for (Account ac : accounts) {
            tmp.add("0x" + Hex.toHexString(ac.getEcKey().getAddress()));
        }
        JSONRPC2Response res = new JSONRPC2Response(tmp, req.getID());
        return res;

    }
}