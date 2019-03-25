package io.taucoin.android.wallet.module.bean;

public class TxDataBean {

    private RawTxBean onData;
    private TxPoolBean offData;

    public RawTxBean getOnData() {
        return onData;
    }

    public void setOnData(RawTxBean onData) {
        this.onData = onData;
    }

    public TxPoolBean getOffData() {
        return offData;
    }

    public void setOffData(TxPoolBean offData) {
        this.offData = offData;
    }
}
