package io.taucoin.android.settings;

import io.taucoin.manager.WorldManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaucoinSettings {

    protected static final Logger logger = LoggerFactory.getLogger("settings");

    private static final String SHARED_PREFERENCE_FILE = "sp";
    private static final String FORGING_WIFI_ONLY_KEY = "forging_wifi_only";

    private Context context;

    private WorldManager worldManager;

    private ConnectivityManager connectivityManager;

    private SharedPreferences sharedPreferences;

    private volatile boolean started = false;

    private BroadcastReceiver networkStateListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                NetworkInfo info
                        = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
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
            = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            if (FORGING_WIFI_ONLY_KEY.equals(key)) {
                onForgingWifiOnlySettingChanged();
            }
        }
    };

    @Inject
    public TaucoinSettings(Context context, WorldManager worldManager) {
        this.context = context;
        this.worldManager = worldManager;

        connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        sharedPreferences = context.getSharedPreferences(
                SHARED_PREFERENCE_FILE, Context.MODE_MULTI_PROCESS);
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
        sharedPreferences.registerOnSharedPreferenceChangeListener(
                sharedPreferencesListener);

        started = true;
    }

    public synchronized void destory() {
        if (!started) {
            return;
        }

        context.unregisterReceiver(networkStateListener);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(
                sharedPreferencesListener);

        started = false;
    }

    public synchronized boolean isSyncDownloadDisabled() {
        return getForgingWifiOnlyValue() && isMobileConnected();
    }

    private boolean getForgingWifiOnlyValue() {
        return sharedPreferences.getBoolean(FORGING_WIFI_ONLY_KEY, false);
    }

    private void onWifiConnected() {
        if (!started || !getForgingWifiOnlyValue()) {
            return;
        }

        logger.info("Wifi connected, try to restart downloading");
        worldManager.startDownload();
    }

    private void onMobileConnected() {
        if (!started || !getForgingWifiOnlyValue()) {
            return;
        }

        logger.info("Mobile connected, try to stop downloading");
        worldManager.stopDownload();
    }

    private void onForgingWifiOnlySettingChanged() {
        if (!started) {
            return;
        }

        boolean value = getForgingWifiOnlyValue();
        logger.info("Forging wifi only value changed {}", value);
        if (value) {
            if (isMobileConnected()) {
                logger.info("Now mobile connected, stop downloading");
                worldManager.stopDownload();
            }
        } else {
            logger.info("try to restart downloading");
            worldManager.startDownload();
        }
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
