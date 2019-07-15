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

public class WifiSettings {

    protected static final Logger logger = LoggerFactory.getLogger("settings");

    private Context context;

    private ConnectivityManager connectivityManager;

    private volatile boolean started = false;

    private BroadcastReceiver networkStateListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (info != null) {
                    if (info.getType() == ConnectivityManager.TYPE_WIFI
                            && info.isConnected()) {
                        onWifiConnected();
                    } else if (info.getType() == ConnectivityManager.TYPE_MOBILE
                            && info.isConnected()) {
                        onMobileConnected();
                    }
                }
            }
        }
    };

    private OnSharedPreferenceChangeListener sharedPreferencesListener
        = (sharedPreferences, key) -> {
            if (TransmitKey.FORGING_WIFI_ONLY.equals(key)) {
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

    public synchronized boolean isSyncDownloadDisabled() {
        return getForgingWifiOnlyValue() && isMobileConnected();
    }

    private boolean getForgingWifiOnlyValue() {
        return SharedPreferencesHelper.getInstance().getBoolean(TransmitKey.FORGING_WIFI_ONLY, false);
    }

    private void onWifiConnected() {
        if (!started || !getForgingWifiOnlyValue()) {
            return;
        }

        logger.info("Wifi connected, try to restart downloading");
        startDownload();
    }

    private void onMobileConnected() {
        if (!started || !getForgingWifiOnlyValue()) {
            return;
        }

        logger.info("Mobile connected, try to stop downloading");
        stopDownload();
    }

    public synchronized void onForgingWifiOnlySettingChanged() {
        if (!started) {
            return;
        }

        boolean value = getForgingWifiOnlyValue();
        logger.info("Forging wifi only value changed {}", value);
        if (value) {
            if (isMobileConnected()) {
                logger.info("Now mobile connected, stop downloading");
                stopDownload();
            }
        } else {
            logger.info("try to restart downloading");
            startDownload();
        }
    }

    private synchronized void startDownload(){
        MyApplication.getRemoteConnector().startDownload();
    }

    private synchronized void stopDownload(){
        MyApplication.getRemoteConnector().stopDownload();
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
