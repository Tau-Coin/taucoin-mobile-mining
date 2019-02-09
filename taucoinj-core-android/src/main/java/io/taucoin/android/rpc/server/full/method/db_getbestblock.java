package io.taucoin.android.rpc.server.full.method;
import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.db.BlockStore;
import io.taucoin.android.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;

public class db_getbestblock extends JsonRpcServerMethod{
    public db_getbestblock(Taucoin taucoin){
        super(taucoin);
    }
    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {
        BlockStore bs = taucoin.getBlockStore();
        JSONRPC2Response res = new JSONRPC2Response(bs.getBestBlock().toString(), req.getID());
        return res;

    }
}
