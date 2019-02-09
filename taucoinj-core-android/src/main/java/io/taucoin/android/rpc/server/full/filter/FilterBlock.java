package io.taucoin.android.rpc.server.full.filter;

import net.minidev.json.JSONArray;
import io.taucoin.core.Block;
import io.taucoin.facade.Taucoin;
import org.spongycastle.util.encoders.Hex;
import java.util.ArrayList;


public class FilterBlock extends FilterBase {

    private ArrayList<String> blocks = new ArrayList<String>();

    public void processEvent(Object data) {
        if (data instanceof Block) {
            synchronized (blocks) {
                blocks.add("0x" + Hex.toHexString(((Block) data).getHash()));
            }
        }
    }

    public JSONArray toJS() {
        updateLastRequest();
        JSONArray res = new JSONArray();
        synchronized (blocks) {
            for(String item : blocks) {
                res.add(item);
            }
            blocks.clear();
        }
        return res;
    }

    public JSONArray toJS(Taucoin taucoin) {
        return null;
    }

}
