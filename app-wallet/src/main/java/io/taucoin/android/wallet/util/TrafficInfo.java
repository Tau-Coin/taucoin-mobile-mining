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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;

import com.github.naturs.logger.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import io.taucoin.android.wallet.MyApplication;

import static android.content.Context.NETWORK_STATS_SERVICE;

class TrafficInfo {
    private int UNSUPPORTED = -1;
//    private long preRxBytes = 0;

    /**
     * Acquire total traffic
     */
    long getTrafficInfo(int uid) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getNetworkMobileData(uid) + getNetworkWifiData(uid);
        }
        // Download traffic
        long rcvTraffic = UNSUPPORTED;
        // Upload traffic
        long sndTraffic = UNSUPPORTED;

        rcvTraffic = getRcvTraffic(uid);
        sndTraffic = getSndTraffic(uid);
        if (rcvTraffic == UNSUPPORTED || sndTraffic == UNSUPPORTED)
            return UNSUPPORTED;
        else
            return rcvTraffic + sndTraffic;
    }

    /**
     * Get download traffic
     */
    private long getRcvTraffic(int uid) {
        long rcvTraffic = UNSUPPORTED;
        rcvTraffic = TrafficStats.getUidRxBytes(uid);
        if (rcvTraffic == UNSUPPORTED) {
            return UNSUPPORTED;
        }
        Logger.i(rcvTraffic + "--1");
        RandomAccessFile rafRcv = null, rafSnd = null;
        String rcvPath = "/proc/uid_stat/" + uid + "/tcp_rcv";
        try {
            rafRcv = new RandomAccessFile(rcvPath, "r");
            rcvTraffic = Long.parseLong(rafRcv.readLine());
        } catch (FileNotFoundException e) {
            Logger.e(e, "FileNotFoundException: ");
            rcvTraffic = UNSUPPORTED;
        } catch (Exception e) {
            Logger.e(e, "getRcvTraffic");
        } finally {
            try {
                if (rafRcv != null)
                    rafRcv.close();
                if (rafSnd != null)
                    rafSnd.close();
            } catch (IOException e) {
                Logger.w("Close RandomAccessFile exception: " + e.getMessage());
            }
        }
        Logger.i(rcvTraffic + "--2");
        return rcvTraffic;
    }

    /**
     * Get upload traffic
     */
    private long getSndTraffic(int uid) {
        long sndTraffic = UNSUPPORTED;
        sndTraffic = TrafficStats.getUidTxBytes(uid);
        if (sndTraffic == UNSUPPORTED) {
            return UNSUPPORTED;
        }

        RandomAccessFile rafRcv = null, rafSnd = null;
        String sndPath = "/proc/uid_stat/" + uid + "/tcp_snd";
        try {
            rafSnd = new RandomAccessFile(sndPath, "r");
            sndTraffic = Long.parseLong(rafSnd.readLine());
        } catch (FileNotFoundException e) {
            Logger.e(e, "FileNotFoundException: ");
            sndTraffic = UNSUPPORTED;
        } catch (Exception e) {
            Logger.e(e, "getSndTraffic");
        } finally {
            try {
                if (rafRcv != null)
                    rafRcv.close();
                if (rafSnd != null)
                    rafSnd.close();
            } catch (Exception e) {
                Logger.w("Close RandomAccessFile exception: " + e.getMessage());
            }
        }
        return sndTraffic;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private long getNetworkMobileData(int uid) {
        long summaryTotal = 0;
        try {
            NetworkStats.Bucket summaryBucket = new NetworkStats.Bucket();
            Context context = MyApplication.getInstance();
            NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(NETWORK_STATS_SERVICE);
            String subscriberId = getSubscriberId(context);
            NetworkStats summaryStats = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE, subscriberId, 0, System.currentTimeMillis(), uid);
            do {
                summaryStats.getNextBucket(summaryBucket);
                long summaryRx = summaryBucket.getRxBytes();
                long summaryTx = summaryBucket.getTxBytes();
                summaryTotal += summaryRx + summaryTx;
            } while (summaryStats.hasNextBucket());
            Logger.i(summaryTotal + "data_uid mobile:" + uid + " rx:" + summaryBucket.getRxBytes() +
                    " tx:" + summaryBucket.getTxBytes());
        } catch (Exception e) {
            Logger.e(e, "getNetworkData is error");
        }
        return summaryTotal;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private long getNetworkWifiData(int uid) {
        long summaryTotal = 0;
        try {
            NetworkStats.Bucket summaryBucket = new NetworkStats.Bucket();
            Context context = MyApplication.getInstance();
            NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(NETWORK_STATS_SERVICE);
            NetworkStats summaryStats = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, "", 0, System.currentTimeMillis(), uid);
            do {
                summaryStats.getNextBucket(summaryBucket);
                long summaryRx = summaryBucket.getRxBytes();
                long summaryTx = summaryBucket.getTxBytes();
                summaryTotal += summaryRx + summaryTx;
            } while (summaryStats.hasNextBucket());
            Logger.i(summaryTotal + "data_uid wifi:" + uid + " rx:" + summaryBucket.getRxBytes() +
                    " tx:" + summaryBucket.getTxBytes());
        } catch (Exception e) {
            Logger.e(e, "getNetworkData is error");
        }
        return summaryTotal;
    }

    @SuppressLint("HardwareIds")
    private String getSubscriberId(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.getSubscriberId();
        }
        return "";
    }

    //    public static long getNetworkRxBytes() {
//        return TrafficStats.getTotalRxBytes();
//    }
//
//    public static long getNetworkTxBytes() {
//        return TrafficStats.getTotalTxBytes();
//    }
//
//    public double getNetSpeed() {
//        long curRxBytes = getNetworkRxBytes();
//        if (preRxBytes == 0)
//            preRxBytes = curRxBytes;
//        long bytes = curRxBytes - preRxBytes;
//        preRxBytes = curRxBytes;
//        //int kb = (int) Math.floor(bytes / 1024 + 0.5);
//        double kb = (double)bytes / (double)1024;
//        BigDecimal bd = new BigDecimal(kb);
//
//        return bd.setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue();
//    }

//    public static long getNetWorkData(int uid) {
//        long netWorkData = 0;
//        Logger.d("data_data" + "\t getNetWorkData");
//        try {
//            Runtime runtime = Runtime.getRuntime();
//            Process process = runtime.exec("adb shell cat /proc/net/xt_qtaguid/stats | grep " + uid);
//            try {
//                Logger.d("data_data" + "\t getNetWorkData");
//                if (process.waitFor() != 0) {
//                    Logger.d("exit value = " + process.exitValue());
//                }
//                BufferedReader in = new BufferedReader(new InputStreamReader(
//                        process.getInputStream()));
//                StringBuffer stringBuffer = new StringBuffer();
//                String line;
//                int index = 0;
//                while ((line = in.readLine()) != null) {
//                    stringBuffer.append(line).append(" ");
//                    Logger.d("data_data" + index + "\t" + line);
//                    index += 1;
//                }
//            } catch (InterruptedException e) {
//                Logger.e(e, "InterruptedException");
//            } finally {
//                try {
//                    process.destroy();
//                } catch (Exception e2) {
//                    Logger.e(e2, "process.destroy is error");
//                }
//            }
//        } catch (Exception ignore) {
//            Logger.e(ignore, "data_data" + "\t getNetWorkData");
//        }
//        return netWorkData;
//    }
//    private long getSndTraffic1(int uid) {
//        long sndTraffic = UNSUPPORTED;
//        RandomAccessFile rafRcv = null, rafSnd = null;
//        String sndPath = "/proc/net/xt_qtaguid/stats";
//        try {
//
//            String line;
//            int index = 0;
//            rafSnd = new RandomAccessFile(sndPath, "r");
//            StringBuffer stringBuffer = new StringBuffer();
//            while ((line = rafSnd.readLine()) != null) {
//                Logger.d("data_data\t" + index + "\t" + line);
//                index += 1;
//            }
//
//        } catch (FileNotFoundException e) {
//            Logger.e(e,  "Close RandomAccessFile exception: ");
//            sndTraffic = UNSUPPORTED;
//        } catch (Exception ignore) {
//            Logger.e(ignore,  "Close RandomAccessFile exception: ");
//        } finally {
//            try {
//                if (rafRcv != null)
//                    rafRcv.close();
//                if (rafSnd != null)
//                    rafSnd.close();
//            } catch (Exception e) {
//                Logger.w( "Close RandomAccessFile exception: " + e.getMessage());
//            }
//        }
//        return sndTraffic;
//    }
}