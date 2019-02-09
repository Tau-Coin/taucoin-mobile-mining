package io.taucoin.rpc.server.full.method;

import com.thetransactioncompany.jsonrpc2.*;
import com.thetransactioncompany.jsonrpc2.server.*;
import io.taucoin.rpc.server.full.JsonRpcServerMethod;
import io.taucoin.facade.Taucoin;
import io.taucoin.forge.BlockForger;
import net.minidev.json.JSONObject;

import java.util.List;

/*
TODO: must be changed in app that implement mining
*/
public class tau_forging extends JsonRpcServerMethod {

    private final static String FORGER_ERROR = "Can't get forger";
    private final static String IS_FORGING = "Now is forging";
    private final static String START_FORGING = "Start forging successfully";

    public tau_forging (Taucoin taucoin) {
        super(taucoin);
    }

    protected JSONRPC2Response worker(JSONRPC2Request req, MessageContext ctx) {

        List<Object> params = req.getPositionalParams();
        JSONObject obj = (JSONObject)params.get(0);
        String result = startForging(obj);
        JSONRPC2Response res = new JSONRPC2Response(result, req.getID());
        return res;
    }

    private String startForging(JSONObject request) {
        // Firstly  get forger
        BlockForger forger = taucoin.getBlockForger();
        if (forger == null) {
            return FORGER_ERROR;
        }

        if (forger.isForging()) {
            return IS_FORGING;
        }

        long amount = -1;
        if (request.containsKey("amount") && ((long)request.get("amount")) > 0) {
            amount = (long) request.get("amount");
        }
        forger.startForging(amount);

        return START_FORGING;
    }
}