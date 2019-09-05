package io.taucoin.android.wallet.module.view.manage.iview;

import java.util.List;

import io.taucoin.android.wallet.db.entity.KeyValue;

public interface IAddressView {
    void loadData(List<KeyValue> data);
    void refreshView();
}
