package io.taucoin.android.wallet.module.bean;

import java.util.List;

public class RawTxList {

    private List<RawTxBean> records;

    public List<RawTxBean> getRecords() {
        return records;
    }

    public void setRecords(List<RawTxBean> records) {
        this.records = records;
    }
}
