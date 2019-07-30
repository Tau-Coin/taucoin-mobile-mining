package io.taucoin.android.wallet.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.foundation.util.StringUtil;

public class WifiSettings {

    protected static final Logger logger = LoggerFactory.getLogger("settings");

    private Context context;

    private ConnectivityManager connectivityManager;

    private volatile boolean started = false;

    private BroadcastReceiver networkStateListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()) && isMiningOn()) {
                NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (info != null) {
                    if (info.getType() == ConnectivityManager.TYPE_WIFI
                            && info.isConnected()) {
                        onWifiConnected();
                    } else if (info.getType() == ConnectivityManager.TYPE_MOBILE
                            && info.isConnected()) {
                        onMobileConnected();
                    }else if(!isHaveNetwork()){
                        logger.info("Do not have network connected, stop downloading");
                        stopDownload();
                    }
                }
            }
        }
    };

    private boolean isMiningOn(){
        if(UserUtil.isImportKey()){
            String miningState = MyApplication.getKeyValue().getMiningState();
            return StringUtil.isSame(miningState, TransmitKey.MiningState.Start);
        }
        return false;
    }

    private OnSharedPreferenceChangeListener sharedPreferencesListener
        = (sharedPreferences, key) -> {
            if (TransmitKey.FORGING_WIFI_ONLY.equals(key) && isMiningOn()) {
                onForgingWifiOnlySettingChanged();
            }
        };

    public WifiSettings() {
        context = MyApplication.getInstance();
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public synchronized void init() {
        if (started) {
            return;
        }

        // Register network state listener
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(networkStateListener, intentFilter);

        // Register sharedPreferences changed listener.
        SharedPreferencesHelper.getInstance()
            .getSP()
            .registerOnSharedPreferenceChangeListener(sharedPreferencesListener);
        started = true;
    }

    public synchronized void destroy() {
        if (!started) {
            return;
        }
        context.unregisterReceiver(networkStateListener);
        SharedPreferencesHelper.getInstance()
            .getSP()
            .unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener);
        started = false;
    }

    private boolean isForgingWifiOnly() {
        return SharedPreferencesHelper.getInstance().getBoolean(TransmitKey.FORGING_WIFI_ONLY, false);
    }

    private void onWifiConnected() {
        if (!started) {
            return;
        }

        logger.info("Wifi connected, try to restart downloading");
        startDownload();
    }

    private void onMobileConnected() {
        if (!started) {
            return;
        }
        if(isForgingWifiOnly()){
            logger.info("Mobile connected, try to stop downloading");
            stopDownload();
        }else{
            logger.info("Mobile connected, try to start downloading");
            startDownload();
        }
    }

    public synchronized void onForgingWifiOnlySettingChanged() {
        if (!started) {
            return;
        }

        boolean value = isForgingWifiOnly();
        logger.info("Forging wifi only value changed {}", value);
        if (value) {
            if (isMobileConnected()) {
                logger.info("Now mobile connected, stop downloading");
                stopDownload();
            }else if(isWifiConnected()){
                logger.info("Now wifi connected, start downloading");
                startDownload();
            }
        } else {
            if(isHaveNetwork()){
                logger.info("try to restart downloading");
                startDownload();
            }
        }
    }

    private synchronized void startDownload(){
        MyApplication.getRemoteConnector().startDownload();
    }

    private synchronized void stopDownload(){
        MyApplication.getRemoteConnector().stopDownload();
    }

    private boolean isHaveNetwork() {
        return isWifiConnected() || isMobileConnected();
    }

    private boolean isWifiConnected() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                && networkInfo.isConnected();
    }

    private boolean isMobileConnected() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE
                && networkInfo.isConnected();
    }
}
