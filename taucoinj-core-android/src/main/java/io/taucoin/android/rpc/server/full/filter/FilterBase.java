package io.taucoin.android.rpc.server.full.filter;

import net.minidev.json.JSONArray;

import io.taucoin.facade.Taucoin;

public abstract class FilterBase {
    protected int id;
    protected long lastRequest = System.currentTimeMillis();

    public int getId() {
        return id;
    }

    public void  setId(int id) {
        this.id = id;
    }

    public long getLastRequestTime() {
        return lastRequest;
    }

    protected void updateLastRequest() {
        lastRequest = System.currentTimeMillis();
    }

    public abstract void processEvent(Object data);
    public abstract JSONArray toJS();
    public abstract JSONArray toJS(Taucoin taucoin);
}
