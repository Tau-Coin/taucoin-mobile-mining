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
package io.taucoin.android.wallet.util;

import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.Html;

import com.mofei.tau.R;

public class DialogManager {

    public static void showSureDialog(FragmentActivity activity, int msg, int button){
        showSureDialog(activity, activity.getText(msg).toString(), activity.getText(button).toString());
    }
    public static void showSureDialog(FragmentActivity activity, String msg, String button){

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(R.string.app_upgrade_title)
                .setMessage(Html.fromHtml(msg))
                .setPositiveButton(button, null)
                .setCancelable(false);

        AlertDialog mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(false);
        mDialog.show();

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            mDialog.cancel();
        });
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
    }
}