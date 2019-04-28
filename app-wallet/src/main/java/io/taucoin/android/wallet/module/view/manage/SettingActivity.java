package io.taucoin.android.wallet.module.view.manage;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;

import com.github.naturs.logger.Logger;
import io.taucoin.android.wallet.R;

import java.math.BigInteger;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.presenter.UserPresenter;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.android.wallet.widget.InputDialog;
import io.taucoin.android.wallet.widget.ItemTextView;
import io.taucoin.foundation.net.callback.LogicObserver;

public class SettingActivity extends BaseActivity {


    @BindView(R.id.tv_trans_expiry)
    ItemTextView tvTransExpiry;
    private UserPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        ButterKnife.bind(this);
        mPresenter = new UserPresenter();
        loadView();
    }

    private void loadView() {
        KeyValue keyValue = MyApplication.getKeyValue();
        if(keyValue != null){
            tvTransExpiry.setRightText(UserUtil.getTransExpiryTime() + "min");
        }
    }

    @OnClick({R.id.tv_trans_expiry})
    void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_trans_expiry:
                showInputDialog();
                break;
            default:
                break;
        }
    }

    private void showInputDialog(){
        int inputHint = R.string.setting_transaction_expiry_tip;
        new InputDialog.Builder(this)
            .setInputType(InputType.TYPE_CLASS_NUMBER)
            .setInputHint(inputHint)
            .setNegativeButton(R.string.common_cancel, (InputDialog.InputDialogListener) (dialog, text) -> dialog.cancel())
            .setPositiveButton(R.string.common_done, (InputDialog.InputDialogListener) (dialog, text) -> {
                try {
                    long input = new BigInteger(text).longValue();
                    if(input > TransmitKey.MAX_TRANS_EXPIRY || input < TransmitKey.MIN_TRANS_EXPIRY){
                        String expiryLimit = getText(R.string.setting_transaction_expiry_limit).toString();
                        expiryLimit = String.format(expiryLimit, TransmitKey.MIN_TRANS_EXPIRY, TransmitKey.MAX_TRANS_EXPIRY);
                        ToastUtils.showShortToast(expiryLimit);
                        return;
                    }
                    mPresenter.saveTransExpiry(input, logicObserver);
                }catch (Exception e){
                    Logger.e("new BigInteger is error", e);
                }
                dialog.cancel();
            }).create().show();
    }

    private LogicObserver<KeyValue> logicObserver = new LogicObserver<KeyValue>() {
        @Override
        public void handleData(KeyValue keyValue) {
            MyApplication.setKeyValue(keyValue);
            loadView();
        }
    };
}