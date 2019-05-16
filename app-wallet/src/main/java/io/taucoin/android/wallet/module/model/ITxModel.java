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

import java.util.List;

import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.module.bean.AccountBean;
import io.taucoin.android.wallet.module.bean.ChainBean;
import io.taucoin.android.wallet.module.bean.RawTxList;
import io.taucoin.android.wallet.module.bean.TransactionBean;
import io.taucoin.android.wallet.net.callback.TxObserver;
import io.taucoin.foundation.net.callback.LogicObserver;

public interface ITxModel {
    /** Get balance from the server */
    void getBalance(TxObserver<AccountBean> observer);

    /** Detecting whether a transaction enters the trading pool and block chain */
    void checkRawTransaction(List<String> txIds, LogicObserver<Boolean> observer);

    /** Get the list of transactions to be Pending */
    void getTxPendingListDelay(LogicObserver<List<List<String>>> observer);

    /** Create transaction data */
    void createTransaction(TransactionHistory txHistory, LogicObserver<TransactionBean> observer);

    /** Send transaction to the server */
    void sendRawTransaction(TransactionBean transaction, LogicObserver<Boolean> observer);

    /** Update local transaction history */
    void updateTransactionHistory(TransactionHistory txHistory, LogicObserver<Boolean> observer);

    /** Get local transaction history */
    void queryTransactionHistory(int pageNo, String time, LogicObserver<List<TransactionHistory>> logicObserver);

    /** Get the transaction history of the server  */
    void getTxRecords(TxObserver<RawTxList> observer);

    /** Save the transaction history of the server  */
    void saveTxRecords(RawTxList rawTxList, LogicObserver<Boolean> observer);

    /** get block height from the server */
    void getBlockHeight(TxObserver<ChainBean> observer);

    /** Update balance from the server */
    void updateBalance(AccountBean accountInfo, LogicObserver<KeyValue> observer);

    /** update or save current block height */
    void updateBlockHeight(int blockHeight, LogicObserver<Boolean> observer);

}