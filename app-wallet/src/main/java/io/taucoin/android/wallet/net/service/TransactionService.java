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
package io.taucoin.android.wallet.net.service;

import java.util.Map;

import io.reactivex.Observable;

import io.taucoin.android.wallet.module.bean.BalanceBean;
import io.taucoin.android.wallet.module.bean.RawTxList;
import io.taucoin.android.wallet.module.bean.TxDataBean;
import io.taucoin.foundation.net.callback.DataResult;
import retrofit2.http.Body;
import retrofit2.http.POST;
/**
 * Application Transaction-related Background Services
 * */
public interface TransactionService {

    @POST("getAccountDetail/")
    Observable<DataResult<BalanceBean>> getBalance(@Body Map<String,String> email);

    @POST("getTauTransaction/")
    Observable<DataResult<TxDataBean>> getRawTransaction(@Body Map<String,String> txId);

    @POST("sendTauTransaction/")
    Observable<DataResult<String>> sendRawTransaction(@Body Map<String,String> tx_hex);

    @POST("getTxRecords/")
    Observable<DataResult<RawTxList>> getTxRecords(@Body Map<String,String> address);

    @POST("getTauHeight/")
    Observable<DataResult<String>> getBlockHeight();
}