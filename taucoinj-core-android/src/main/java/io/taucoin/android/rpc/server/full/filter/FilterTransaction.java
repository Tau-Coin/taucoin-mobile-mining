package io.taucoin.android.rpc.server.full.filter;

import net.minidev.json.JSONArray;
import io.taucoin.core.Transaction;
import io.taucoin.facade.Taucoin;
import org.spongycastle.util.encoders.Hex;
import java.util.ArrayList;


public class FilterTransaction extends FilterBase {

    private ArrayList<String> transactions = new ArrayList<String>();

    public void processEvent(Object data) {
        if (data instanceof Transaction) {
            synchronized (transactions) {
                transactions.add("0x" + Hex.toHexString(((Transaction) data).getHash()));
            }
        }
    }

    public JSONArray toJS() {
        updateLastRequest();
        JSONArray res = new JSONArray();
        synchronized (transactions) {
            for(String item : transactions) {
                res.add(item);
            }
            transactions.clear();
        }
        return res;
    }

    public JSONArray toJS(Taucoin taucoin) {
        return null;
    }

}
