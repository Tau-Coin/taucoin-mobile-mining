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

import java.util.List;
import java.util.Map;

import io.reactivex.Observable;

import io.taucoin.android.wallet.module.bean.AccountBean;
import io.taucoin.android.wallet.module.bean.ChainBean;
import io.taucoin.android.wallet.module.bean.IncomeInfoBean;
import io.taucoin.android.wallet.module.bean.MinerListBean;
import io.taucoin.android.wallet.module.bean.MinerInfoBean;
import io.taucoin.android.wallet.module.bean.RankInfoBean;
import io.taucoin.android.wallet.module.bean.NewTxBean;
import io.taucoin.android.wallet.module.bean.RawTxList;
import io.taucoin.android.wallet.module.bean.RewardInfoBean;
import io.taucoin.android.wallet.module.bean.StatesTagBean;
import io.taucoin.android.wallet.module.bean.TxDataBean;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
/**
 * Application Transaction-related Background Services
 * */
public interface TransactionService {

    @POST("getaccountinfo")
    Observable<AccountBean> getBalance(@Body Map<String,String> address);

    @POST("gettxsstatus")
    Observable<TxDataBean> getRawTransaction(@Body Map<String, List<String>> txIds);

    @POST("newtransaction")
    Observable<NewTxBean> sendRawTransaction(@Body Map<String,String> tx_hex);

    @POST("newtransaction")
    Call<ResponseBody> sendBudgetTransaction(@Body Map<String,String> tx_hex);

    @POST("gettxsrecords")
    Observable<RawTxList> getTxRecords(@Body Map<String,String> address);

    @GET("getchaininfo")
    Observable<ChainBean> getBlockHeight();

    @GET("getincomeinfo")
    Observable<IncomeInfoBean> getIncomeInfo();

    @POST("getminerinfo")
    Observable<MinerInfoBean> getMinerInfo(@Body Map<String,String> address);

    @POST("getrankinfo")
    Observable<RankInfoBean> getRankInfo(@Body Map<String,String> address);

    @POST("getminerhistory")
    Observable<MinerListBean> getMinerHistory(@Body Map<String,String> address);

    @POST("getrewardinfo")
    Observable<RewardInfoBean> getRewardInfo(@Body Map<String,String> address);

    @GET("getstatetag")
    Observable<StatesTagBean> getStatesTag();
}