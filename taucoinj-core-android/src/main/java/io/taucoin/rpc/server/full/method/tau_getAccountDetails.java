package io.taucoin.rpc.server.full.method;
import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.core.AccountState;
import io.taucoin.db.BlockStore;
import io.taucoin.facade.Repository;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;
import io.taucoin.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class tau_getAccountDetails extends JsonRpcServerMethod {
    public tau_getAccountDetails(Taucoin taucoin){
        super(taucoin);
    }

    //todo
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        Repository repo = taucoin.getRepository();
        List<Object> params = req.getPositionalParams();
        if (params.size() < 1 ) {
            return new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, req.getID());
        }
        ArrayList<String> result = new ArrayList<String>();
        System.out.println("address is: "+ Hex.toHexString(jsToByteArray((String)params.get(0))));
        if(repo.isExist(jsToByteArray((String)params.get(0)))){
            System.out.println("have come here");
            result.add("address: "+ Hex.toHexString(jsToByteArray((String)params.get(0))));
            result.add("balance: "+repo.getBalance(jsToByteArray((String)params.get(0))).toString());
            result.add("forgepower: "+repo.getforgePower(jsToByteArray((String)params.get(0))).toString());
            AccountState ac = repo.getAccountState(jsToByteArray((String)params.get(0)));
            Iterator iterator = ac.getTranHistory().entrySet().iterator();
            int i=0;
            while(iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                result.add("tran "+i+" hashcode: "+entry.getKey().toString());
                result.add("tran "+i+" tranTime: "+entry.getValue().toString());
                i++;
            }
        }
        JSONRPC2Response res = new JSONRPC2Response(result, req.getID());
        return res;

    }
}
