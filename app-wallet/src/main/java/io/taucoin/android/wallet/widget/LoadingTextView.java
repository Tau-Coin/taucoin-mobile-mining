/**
 * Copyright 2018 Taucoin Core Developers.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.widget;

import android.content.Context;
import android.os.Message;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import io.taucoin.android.wallet.base.BaseHandler;

public class LoadingTextView extends AppCompatTextView implements BaseHandler.HandleCallBack{
    private BufferType mBufferType = BufferType.NORMAL;
    private boolean isLoading = false;
    private int pointNum = 0;
    private String text = "";
    private BaseHandler mHandler;
    private Thread mThread;

    public LoadingTextView(Context context) {
        this(context, null);
    }

    public LoadingTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHandler = new BaseHandler(this);
    }

    public void setLoadingText(int reid) {
        setLoadingText(getResources().getString(reid));
    }

    public synchronized void setLoadingText(String text){
        this.text = text;
        pointNum = 0;
        if(!isLoading){
            mHandler.sendEmptyMessage(0);
        }
        isLoading = true;
    }

    @Override
    public void handleMessage(Message message) {
        if(isLoading){
            initData();
            startLoadingDelay();
        }
    }

    private void initData() {
        if(pointNum > 3){
            pointNum = 0;
        }
        String loadingText = text;
        if(pointNum == 0){
            loadingText += "   ";
        }else if(pointNum == 1){
            loadingText += ".  ";
        }else if(pointNum == 2){
            loadingText += ".. ";
        }else if(pointNum == 3){
            loadingText += "...";
        }
        setText(loadingText, mBufferType);
    }

    private synchronized void startLoadingDelay() {
        if(mThread == null){
            mThread = new Thread(() -> {
                if(isLoading){
                    try {
                        Thread.sleep(380);
                        pointNum ++;
                        mHandler.sendEmptyMessage(0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        mThread.start();
    }

    public void setNormalText(String text) {
        this.text = text;
        isLoading = false;
        setText(text, mBufferType);
    }

    public void setNormalText(int reid) {
        setNormalText(getResources().getString(reid));
    }
}