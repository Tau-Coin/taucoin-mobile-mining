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
package io.taucoin.android.wallet.module.service;

import android.content.Context;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageStats;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.support.annotation.NonNull;

import com.github.naturs.logger.Logger;

import java.math.BigInteger;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.BaseHandler;
import io.taucoin.android.wallet.util.SysUtil;
import io.taucoin.foundation.util.ThreadPool;
import io.taucoin.foundation.util.TrafficUtil;

class ResManager implements BaseHandler.HandleCallBack{

     private boolean isRunning = true;
     private IBinder mBinder = null;
     private IInterface mInterface = null;

     private BaseHandler mHandler;
     private ResCallBack mResCallBack;
     private SysUtil mSysUtil;

     ResManager(){
         mHandler = new BaseHandler(this);
     }
     @Override
     public void handleMessage(Message msg) {
         switch (msg.what) {
             case 2:
                 Bundle bundle = msg.getData();
                 SysUtil.MemoryInfo info = bundle.getParcelable("data");
                 if(info != null){
                     String memoryInfo = SysUtil.formatFileSizeMb(info.totalMemory);

                     String cpuInfo = String.valueOf(info.cpuUsageRate);
                     int pointIndex = cpuInfo.indexOf(".");
                     int length = cpuInfo.length();
                     if(pointIndex > 0 && length - pointIndex > 3){
                         cpuInfo = cpuInfo.substring(0, pointIndex + 3);
                     }
                     cpuInfo += "%";

                     long dailyTraffic = TrafficUtil.getTrafficTotal();
                     String netDataInfo = SysUtil.formatFileSizeMb(dailyTraffic);

                     if(mResCallBack != null){
                         mResCallBack.updateCpuAndMemory(cpuInfo, memoryInfo, netDataInfo);
                     }
                 }
                 break;
             case 3:
                 PackageStats newPs = msg.getData().getParcelable("data");
                 if (newPs != null) {
                     long dataSize = newPs.dataSize + newPs.cacheSize + newPs.codeSize;
                     dataSize += newPs.externalCacheSize + newPs.externalCodeSize + newPs.externalDataSize;
                     dataSize += newPs.externalMediaSize + newPs.externalObbSize;
                     String dataInfo = SysUtil.formatFileSizeMb(dataSize);
                     if(mResCallBack != null){
                         mResCallBack.updateDataSize(dataInfo);
                     }
                 }
                 break;
             default:
                 break;
         }
     }
//
//    private long handleTrafficData(long currentTraffic) {
//        long currentTrafficTime = new Date().getTime();
//        long oldTraffic = SharedPreferencesHelper.getInstance().getLong(TransmitKey.TRAFFIC, 0);
//        long oldTrafficTime = SharedPreferencesHelper.getInstance().getLong(TransmitKey.TRAFFIC_TIME, currentTrafficTime);
//        long dailyTraffic = currentTraffic - oldTraffic;
//
//        if(dailyTraffic < 0){
//            dailyTraffic = 0;
//        }
//
//        if(DateUtil.compareDay(oldTrafficTime, currentTrafficTime) > 0 || oldTraffic <= 0 || oldTrafficTime <= 0){
//            dailyTraffic = 0;
//            SharedPreferencesHelper.getInstance().putLong(TransmitKey.TRAFFIC, currentTraffic);
//            SharedPreferencesHelper.getInstance().putLong(TransmitKey.TRAFFIC_TIME, currentTrafficTime);
//        }
//        return dailyTraffic;
//    }

    private synchronized void startResThreadDelay() {
         ThreadPool.getThreadPool().execute(() -> {
             try {
                 if(isRunning){
                     Context context = MyApplication.getInstance();
                     mSysUtil.getPkgInfo(context.getPackageName(), packageStatsObserver);

                     SysUtil.MemoryInfo info =  mSysUtil.loadAppProcess();
                     Bundle bundle = new Bundle();
                     bundle.putParcelable("data", info);
                     Message message = mHandler.obtainMessage(2);
                     message.setData(bundle);
                     mHandler.sendMessage(message);
                     Thread.sleep(3500);
                     startResThreadDelay();
                 }
             } catch (InterruptedException e) {
                 Logger.e("startResThreadDelay is error", e);
             }
         });
     }

    void startResThread(ResCallBack resCallBack) {
        mResCallBack = resCallBack;
        mSysUtil = new SysUtil();
        startResThreadDelay();
    }

     private IPackageStatsObserver.Stub packageStatsObserver = new IPackageStatsObserver.Stub() {
         public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) {
             Message msg = mHandler.obtainMessage(3);
             Bundle data = new Bundle();
             data.putParcelable("data", pStats);
             msg.setData(data);
             mHandler.sendMessage(msg);
         }

         @Override
         public IBinder asBinder() {
             mBinder = super.asBinder();
             return mBinder;
         }

         @Override
         public IInterface queryLocalInterface(@NonNull String descriptor) {
             mInterface = super.queryLocalInterface(descriptor);
             return mInterface;
         }
     };

     void stopResThread() {
         isRunning = false;
         if(mInterface != null){
             mBinder = mInterface.asBinder();
         }
         mBinder = null;
         mInterface = null;
     }

     abstract static class ResCallBack{
         abstract void updateCpuAndMemory(String cpuInfo, String memoryInfo, String networkData);
         abstract void updateDataSize(String dataInfo);
     }
 }