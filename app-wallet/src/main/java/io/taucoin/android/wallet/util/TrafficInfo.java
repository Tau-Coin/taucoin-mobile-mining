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

import android.net.TrafficStats;
import com.github.naturs.logger.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

class TrafficInfo {
    private int UNSUPPORTED = -1;
//    private long preRxBytes = 0;

    /**
     * Acquire total traffic
     */
    long getTrafficInfo(int uid) {
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
//            Logger.e(e,"FileNotFoundException: ");
            rcvTraffic = UNSUPPORTED;
        } catch (Exception ignore) {

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
//            Logger.e(e, "FileNotFoundException: ");
            sndTraffic = UNSUPPORTED;
        } catch (Exception ignore) {
        } finally {
            try {
                if (rafRcv != null)
                    rafRcv.close();
                if (rafSnd != null)
                    rafSnd.close();
            } catch (Exception e) {
                Logger.w( "Close RandomAccessFile exception: " + e.getMessage());
            }
        }
        return sndTraffic;
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
}