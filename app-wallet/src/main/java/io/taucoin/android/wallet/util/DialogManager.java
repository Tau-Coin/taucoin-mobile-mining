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
import android.view.View;
import android.widget.TextView;

import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.widget.CommonDialog;
import io.taucoin.foundation.util.StringUtil;

public class DialogManager {

    public static void showSureDialog(FragmentActivity activity, int msg, int negButton, int posButton,
              DialogOnClickListener negListener, DialogOnClickListener posListener){
        String message = activity.getText(msg).toString();
        String negativeButton = activity.getText(negButton).toString();
        String positiveButton = activity.getText(posButton).toString();
        showSureDialog(activity, message, negativeButton, positiveButton, negListener, posListener);
    }

    public static void showSureDialog(FragmentActivity activity, int msg, int negButton, int posButton, DialogOnClickListener posListener){
        String message = activity.getText(msg).toString();
        String negativeButton = activity.getText(negButton).toString();
        String positiveButton = activity.getText(posButton).toString();
        showSureDialog(activity, message, negativeButton, positiveButton, null, posListener);
    }

    public static void showSureDialog(FragmentActivity activity, int msg, int posButton, DialogOnClickListener posListener){
        String message = activity.getText(msg).toString();
        String positiveButton = activity.getText(posButton).toString();
        showSureDialog(activity, message, null, positiveButton, null, posListener);
    }

    private static void showSureDialog(FragmentActivity activity, String msg, String negButton, String posButton,
            DialogOnClickListener negListener, DialogOnClickListener posListener){

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setMessage(Html.fromHtml(msg))
                .setCancelable(false);
        if(StringUtil.isNotEmpty(negButton)){
            builder.setNegativeButton(negButton, null);
        }
        if(StringUtil.isNotEmpty(posButton)){
            builder.setPositiveButton(posButton, null);
        }

        AlertDialog mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(false);
        mDialog.show();

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if(posListener != null){
                posListener.onClick(v);
            }
            mDialog.cancel();
        });

        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            if(negListener != null){
                negListener.onClick(v);
            }
            mDialog.cancel();
        });
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
    }

    public interface DialogOnClickListener{
        void onClick(View view);
    }

    public static void showTipDialog(FragmentActivity activity, String msg) {
        TextView textView = new TextView(activity);
        textView.setTextAppearance(activity, R.style.style_normal_grey_dark);
        textView.setText(msg);
        textView.setLineSpacing(5.0f, 1.2f);
        new CommonDialog.Builder(activity)
                .setContentView(textView)
                .create().show();
    }

    public static void showTipDialog(FragmentActivity activity, int msgRes) {
        showTipDialog(activity, ResourcesUtil.getText(msgRes));
    }
}