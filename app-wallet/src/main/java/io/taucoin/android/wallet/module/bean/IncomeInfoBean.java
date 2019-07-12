package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

public class IncomeInfoBean extends BaseBean{

    @SerializedName(value = "payload")
    private IncomeBean payLoad;

    public IncomeBean getPayLoad() {
        return payLoad;
    }

    public void setPayLoad(IncomeBean payLoad) {
        this.payLoad = payLoad;
    }
}
