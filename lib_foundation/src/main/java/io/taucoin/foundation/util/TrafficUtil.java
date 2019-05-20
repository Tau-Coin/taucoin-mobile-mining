package io.taucoin.foundation.util;

import java.util.Calendar;
import java.util.Date;

public class TrafficUtil {
    private static final String TRAFFIC_WALLET = "trafficWallet";
    private static final String TRAFFIC_MINING = "trafficMining";
    private static final String TRAFFIC_TIME = "trafficTime";

    public static void saveTrafficWallet(long byteSize){
        resetTrafficInfo();
        byteSize += SharedPreferencesHelper.getInstance().getLong(TRAFFIC_WALLET, 0);
        SharedPreferencesHelper.getInstance().putLong(TRAFFIC_WALLET, byteSize);
    }

    public static void saveTrafficMining(long byteSize){
        resetTrafficInfo();
        byteSize += SharedPreferencesHelper.getInstance().getLong(TRAFFIC_MINING, 0);
        SharedPreferencesHelper.getInstance().putLong(TRAFFIC_MINING, byteSize);
    }

    public static long getTrafficTotal() {
        return getTrafficMining() + getTrafficWallet();
    }

    private static long getTrafficWallet() {
        resetTrafficInfo();
        return SharedPreferencesHelper.getInstance().getLong(TRAFFIC_WALLET, 0);
    }

    private static long getTrafficMining() {
        resetTrafficInfo();
        return SharedPreferencesHelper.getInstance().getLong(TRAFFIC_MINING, 0);
    }

    private static void resetTrafficInfo() {
        long currentTrafficTime = new Date().getTime();
        long oldTrafficTime = SharedPreferencesHelper.getInstance().getLong(TRAFFIC_TIME, 0);
        if(oldTrafficTime == 0 || compareDay(oldTrafficTime, currentTrafficTime) > 0){
            SharedPreferencesHelper.getInstance().putLong(TRAFFIC_TIME, currentTrafficTime);
            SharedPreferencesHelper.getInstance().putLong(TRAFFIC_WALLET, 0);
            SharedPreferencesHelper.getInstance().putLong(TRAFFIC_MINING, 0);
        }
    }

    private static int compareDay(long formerTime, long latterTime) {
        int day = 0;
        if(latterTime > formerTime){
            try {
                Date date1 = new Date(formerTime);
                Date date2 = new Date(latterTime);
                day = differentDays(date1, date2);
            }catch (Exception ignore){
            }
        }
        return day;
    }

    private static int differentDays(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        int day1= cal1.get(Calendar.DAY_OF_YEAR);
        int day2 = cal2.get(Calendar.DAY_OF_YEAR);

        int year1 = cal1.get(Calendar.YEAR);
        int year2 = cal2.get(Calendar.YEAR);
        if(year1 != year2) {
            int timeDistance = 0 ;
            for(int i = year1 ; i < year2 ; i ++) {
                if(i%4==0 && i%100!=0 || i%400==0){
                    timeDistance += 366;
                } else {
                    timeDistance += 365;
                }
            }
            return timeDistance + (day2 - day1) ;
        } else {
            return day2 - day1;
        }
    }
}
