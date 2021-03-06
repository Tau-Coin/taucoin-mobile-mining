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
package io.taucoin.android.wallet.net.callback;


import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.foundation.net.callback.MainObserver;
import io.taucoin.foundation.util.StringUtil;

public abstract class TxObserver<T> extends MainObserver<T> {

    @Override
    public void handleError(String msg, int msgCode) {

        if(StringUtil.isNotEmpty(msg)){
            ToastUtils.showLongToast(msg);
        }
    }

    @Override
    public void handleData(T t) {

    }
}
