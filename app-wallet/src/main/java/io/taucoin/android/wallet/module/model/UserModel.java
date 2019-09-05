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
package io.taucoin.android.wallet.module.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.util.KeyValueDaoUtils;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.net.exception.CodeException;
import io.taucoin.foundation.util.StringUtil;
import io.taucoin.platform.adress.Key;
import io.taucoin.platform.adress.KeyManager;

public class UserModel implements IUserModel{
    private Scheduler scheduler = Schedulers.from(Executors.newFixedThreadPool(20));
    @Override
    public void saveKeyAndAddress(final KeyValue keyValue, LogicObserver<KeyValue> observer) {
        Observable.create((ObservableOnSubscribe<KeyValue>) emitter -> {

            KeyValue kv = keyValue;
            if(kv == null){
                Key key = KeyManager.generatorKey();
                if(key != null){
                    kv = new KeyValue();
                    kv.setPriKey(key.getPriKey());
                    kv.setPubKey(key.getPubKey());
                    kv.setAddress(key.getAddress());
                    kv.setRawAddress(key.getRawAddress());
                }else {
                    emitter.onError(CodeException.getError());
                    return;
                }
            }
            kv.setLastUseTime(System.currentTimeMillis());
            KeyValue result = KeyValueDaoUtils.getInstance().insertOrReplace(kv);
            emitter.onNext(result);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .subscribe(observer);
    }

    @Override
    public void saveName(String pubKey, String name, LogicObserver<KeyValue> observer) {
        Observable.create((ObservableOnSubscribe<KeyValue>) emitter -> {
            KeyValue keyValue = KeyValueDaoUtils.getInstance().queryByPubicKey(pubKey);
            keyValue.setNickName(name);
            KeyValueDaoUtils.getInstance().update(keyValue);
            emitter.onNext(keyValue);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .subscribe(observer);
    }

    @Override
    public void getKeyAndAddress(String publicKey, LogicObserver<KeyValue> observer) {
        Observable.create((ObservableOnSubscribe<KeyValue>) emitter -> {
            KeyValue keyValue = KeyValueDaoUtils.getInstance().queryByPubicKey(publicKey);

            if(keyValue != null && StringUtil.isNotEmpty(keyValue.getPriKey())){
                Key key = KeyManager.validateKey(keyValue.getPriKey());
                if(key != null){
                    keyValue.setPubKey(key.getPubKey());
                    keyValue.setAddress(key.getAddress());
                    keyValue.setRawAddress(key.getRawAddress());
                    KeyValueDaoUtils.getInstance().update(keyValue);
                }
                emitter.onNext(keyValue);
            }else{
                SharedPreferencesHelper.getInstance().clear();
                saveKeyAndAddress(null, observer);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .subscribe(observer);
    }

    @Override
    public void saveTransExpiry(long transExpiry, LogicObserver<KeyValue> observer) {
        Observable.create((ObservableOnSubscribe<KeyValue>) emitter -> {
            String publicKey = MyApplication.getKeyValue().getPubKey();
            KeyValue keyValue = KeyValueDaoUtils.getInstance().queryByPubicKey(publicKey);
            keyValue.setTransExpiry(transExpiry);
            KeyValueDaoUtils.getInstance().update(keyValue);
            emitter.onNext(keyValue);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .subscribe(observer);
    }

    @Override
    public void getAddressList(String key, LogicObserver<List<KeyValue>> observer) {
        Observable.create((ObservableOnSubscribe<List<KeyValue>>) emitter -> {
            List<KeyValue> keyValues = KeyValueDaoUtils.getInstance().querySearch(key);
            Collections.sort(keyValues, (o1, o2) -> Long.compare(o2.getLastUseTime(), o1.getLastUseTime()));
            emitter.onNext(keyValues);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .subscribe(observer);
    }

    @Override
    public void deleteAddress(String pubKey, LogicObserver<Boolean> observer) {
        Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            KeyValueDaoUtils.getInstance().deleteByPubKey(pubKey);
            emitter.onNext(true);
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .subscribe(observer);
    }
}