/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.module.presenter;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import io.taucoin.android.wallet.R;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.model.IUserModel;
import io.taucoin.android.wallet.module.model.UserModel;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.module.view.manage.iview.IAddressView;
import io.taucoin.android.wallet.module.view.manage.iview.IImportKeyView;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.android.wallet.widget.CommonDialog;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;

public class UserPresenter {

    private IImportKeyView mIImportKeyView;
    private IUserModel mUserModel;
    private IAddressView mAddressView;
    private TxPresenter mTxPresenter;

    public UserPresenter() {
        mUserModel = new UserModel();
        mTxPresenter = new TxPresenter();
    }
    public UserPresenter(IImportKeyView view) {
        mUserModel = new UserModel();
        mIImportKeyView = view;
        mTxPresenter = new TxPresenter();
    }

    public UserPresenter(IAddressView view) {
        mUserModel = new UserModel();
        mAddressView = view;
        mTxPresenter = new TxPresenter();
    }

    public void showSureDialog(FragmentActivity context) {
        showSureDialog(context, null);
    }

    public void showSureDialog(FragmentActivity context, KeyValue keyValue) {
        View view = LinearLayout.inflate(context, R.layout.view_dialog_keys, null);
        new CommonDialog.Builder(context)
            .setContentView(view)
            .setButtonWidth(240)
            .setExchange(keyValue == null)
            .setPositiveButton(R.string.send_dialog_yes, (dialog, which) -> {
                dialog.cancel();
                saveKeyAndAddress(context, keyValue);
            }).setNegativeButton(R.string.send_dialog_no, (dialog, which) -> dialog.cancel())
            .create().show();
    }

    private void saveKeyAndAddress(FragmentActivity context, KeyValue keyValue) {
        ProgressManager.showProgressDialog(context, false);
        saveKeyAndAddress(keyValue);
    }

    private void saveKeyAndAddress(KeyValue keyValue) {
        boolean isGenerateKey = keyValue == null;
        mUserModel.saveKeyAndAddress(keyValue, new LogicObserver<KeyValue>() {
            @Override
            public void handleData(KeyValue keyValue) {
                MyApplication.setKeyValue(keyValue);
                SharedPreferencesHelper.getInstance().putString(TransmitKey.PUBLIC_KEY, keyValue.getPubKey());
                SharedPreferencesHelper.getInstance().putString(TransmitKey.ADDRESS, keyValue.getAddress());
                SharedPreferencesHelper.getInstance().putString(TransmitKey.RAW_ADDRESS, keyValue.getRawAddress());
                TxService.startTxService(TransmitKey.ServiceType.GET_HOME_DATA);
                TxService.startTxService(TransmitKey.ServiceType.GET_INFO);
                Intent intent = new Intent();
                intent.putExtra(TransmitKey.SERVICE_TYPE, TransmitKey.ServiceType.GET_BALANCE);
                intent.putExtra(TransmitKey.DATA, false);
                TxService.startTxService(intent);
                MyApplication.getRemoteConnector().init();
                if(isGenerateKey){
                    gotoKeysActivity();
                }else{
                    getAddOuts();
                }
            }

            @Override
            public void handleError(int code, String msg) {
                ProgressManager.closeProgressDialog();
                super.handleError(code, msg);
            }
        });
    }

    private void gotoKeysActivity() {
        ProgressManager.closeProgressDialog();
        if(mIImportKeyView != null){
            mIImportKeyView.gotoKeysActivity();
        }
        if(mAddressView != null){
            mAddressView.refreshView();
        }
        EventBusUtil.post(MessageEvent.EventCode.TRANSACTION);
        EventBusUtil.post(MessageEvent.EventCode.MINING_REWARD);
        EventBusUtil.post(MessageEvent.EventCode.NICKNAME);
    }

    private void getAddOuts() {
        mTxPresenter.getTxRecords(new LogicObserver<Boolean>(){

            @Override
            public void handleData(Boolean aBoolean) {
                gotoKeysActivity();
            }
        });
    }

    public void saveName(String name) {
        String pubKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
        saveName(pubKey, name, true, null);
    }

    public void saveName(String pubKey, String name, boolean isSelf, LogicObserver<KeyValue> observer) {
        mUserModel.saveName(pubKey, name, new LogicObserver<KeyValue>() {
            @Override
            public void handleData(KeyValue keyValue) {
                if(isSelf){
                    MyApplication.setKeyValue(keyValue);
                }
                EventBusUtil.post(MessageEvent.EventCode.NICKNAME);
                if(observer != null){
                    observer.handleData(keyValue);
                }

            }
        });
    }

    public void initLocalData() {

        String publicKey = SharedPreferencesHelper.getInstance().getString(TransmitKey.PUBLIC_KEY, "");
        mUserModel.getKeyAndAddress(publicKey, new LogicObserver<KeyValue>() {
            @Override
            public void handleData(KeyValue keyValue) {
                MyApplication.setKeyValue(keyValue);
                SharedPreferencesHelper.getInstance().putString(TransmitKey.PUBLIC_KEY, keyValue.getPubKey());
                SharedPreferencesHelper.getInstance().putString(TransmitKey.ADDRESS, keyValue.getAddress());
                SharedPreferencesHelper.getInstance().putString(TransmitKey.RAW_ADDRESS, keyValue.getRawAddress());
            }
        });
    }

    public void saveTransExpiry(long transExpiry, LogicObserver<KeyValue> logicObserver) {
        mUserModel.saveTransExpiry(transExpiry, logicObserver);
    }

    public void getAddressList(String key, LogicObserver<List<KeyValue>> observer) {
        mUserModel.getAddressList(key, observer);
    }
    public void getAddressList(String key) {
        mUserModel.getAddressList(key, new LogicObserver<List<KeyValue>>(){

            @Override
            public void handleData(List<KeyValue> keyValues) {
                if(mAddressView != null && keyValues != null){
                    mAddressView.loadData(keyValues);
                }
            }
        });
    }

    public void deleteAddress(String pubKey, LogicObserver<Boolean> observer) {
        mUserModel.deleteAddress(pubKey, observer);
    }

    public void switchAddress(FragmentActivity context, KeyValue keyValue) {
        if(UserUtil.isImportKey()){
            String address =  MyApplication.getKeyValue().getAddress();
            String miningState =  MyApplication.getKeyValue().getMiningState();
            if(StringUtil.isSame(keyValue.getAddress(), address)){
                return;
            }else if(StringUtil.isSame(miningState, TransmitKey.MiningState.Start)){
                ToastUtils.showShortToast(R.string.mining_import_private_key);
                return;
            }
        }
        View viewTip = LinearLayout.inflate(context, R.layout.view_dialog_keys, null);
        TextView tvMsg = viewTip.findViewById(R.id.tv_msg);
        tvMsg.setText(R.string.address_book_switch_sure);
        new CommonDialog.Builder(context)
                .setContentView(viewTip)
                .setHorizontal()
                .setPositiveButton(R.string.common_yes, (dialog, which) -> {
                    dialog.cancel();
                    saveKeyAndAddress(context, keyValue);
                })
                .setNegativeBgResource(R.drawable.grey_rect_round_bg)
                .setNegativeButton(R.string.common_no, (dialog, which) -> dialog.cancel())
                .create().show();
    }
}