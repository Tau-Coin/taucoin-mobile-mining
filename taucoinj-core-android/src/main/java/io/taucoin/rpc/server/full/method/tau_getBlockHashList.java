package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.core.Blockchain;
import io.taucoin.facade.Taucoin;
import net.minidev.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

/*
TODO: must be changed in app that implement mining
*/
public class tau_getBlockHashList extends JsonRpcServerMethod {

    public tau_getBlockHashList (Taucoin taucoin) {
        super(taucoin);
    }

    private Blockchain blockChain = null;

    private Blockchain getBlockChain() {
        if (this.blockChain == null) {
            this.blockChain = taucoin.getBlockchain();
        }

        return this.blockChain;
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        JSONObject request = (JSONObject)params.get(0);

        long startBlockNumber = 0;
        if (request.containsKey("start") && ((long)request.get("start")) > 0) {
            startBlockNumber = (long) request.get("start");
        }

        int limit = 100;
        if (request.containsKey("limit") && ((int)request.get("limit")) > 0) {
            limit = (int) request.get("limit");
        }

        List<String> blockHashList = new ArrayList<String>();
        List<byte[]> hashList = getBlockChain().getListOfHashesStartFromBlock(startBlockNumber, limit);
        for (byte[] hash : hashList) {
            blockHashList.add("0x" + Hex.toHexString(hash));
        }

        JSONRPC2Response res = new JSONRPC2Response(blockHashList, req.getID());
        return res;
    }
}
