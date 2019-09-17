package io.taucoin.android.wallet.module.service;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.ArrayMap;

import com.github.naturs.logger.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.util.BlockInfoDaoUtils;
import io.taucoin.android.wallet.module.bean.StatesTagBean;
import io.taucoin.android.wallet.net.callback.FileCallback;
import io.taucoin.android.wallet.net.callback.FileResponseBody;
import io.taucoin.android.wallet.net.callback.TxObserver;
import io.taucoin.android.wallet.net.service.FileLoad;
import io.taucoin.android.wallet.util.FileUtil;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
import io.taucoin.android.wallet.util.UriUtil;
import io.taucoin.foundation.net.NetWorkManager;
import io.taucoin.foundation.util.StringUtil;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;

public class StateTagManager {

    private static final int mRetryTime = 5;
    private static final int mSleepTime = 2 * 1000;
    // Block count for about five days
    private static final int mNeedDownloadTagsSize = 288 * 5;

    private enum Status{
        CHECK,
        PROGRESS,
        SUCCESS,
        FAIL
    }
    private TxService mService;
    private volatile Status mStatus;

    private Retrofit.Builder mRetrofitBuilder;
    private Map<String, Integer> mMapResult = new ArrayMap<>();
    public static String tagFileDirName = "states-tag";
    private String destFileDir = "states-tag";

    synchronized boolean isDownloading() {
        return null != mStatus;
    }

    synchronized void initAndCheckStateTag(TxService service){
        Logger.d("init and check StateTag");
        mStatus = Status.CHECK;
        this.mService = service;
        mRetrofitBuilder = NetWorkManager.getRetrofit().newBuilder();
        mRetrofitBuilder.client(initOkHttpClient());
        mMapResult.clear();
        destFileDir =  service.getApplicationInfo().dataDir + File.separator;
        destFileDir += tagFileDirName;

        checkStateTag(mRetryTime);
    }

    private void checkStateTag(int retryTime) {
        if(mService == null){
            return;
        }
        Logger.d("checkStateTag");
        mService.mAppModel.checkStateTag(new TxObserver<StatesTagBean>() {
            @Override
            public void handleError(String msg, int msgCode) {
                Logger.d("checkStateTag error: code= %s, msg= %s", msgCode, msg);
                if(retryTime <= 0){
                    try {
                        Thread.sleep(mSleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    checkStateTag(retryTime - 1);
                }else{
                    handleDownloadFail();
                }
            }

            @Override
            public void handleData(StatesTagBean result) {
                if(result != null && result.getStatus() == 200){
                    Logger.d("checkStateTag success");
                    checkNeedDownload(result);
                }else {
                    handleError("status or data exception ", result == null ? -1 : result.getStatus());
                }
            }
        });
    }

    private void checkNeedDownload(StatesTagBean result) {
        BlockInfo blockInfo = BlockInfoDaoUtils.getInstance().query();
        long syncBlockHeight = blockInfo != null ? blockInfo.getBlockSync() : 0;
        boolean isDirectlyInit = true;
        if(result != null){
            StatesTagBean.TagsBean tags = result.getTags();
            if(tags != null && tags.getStartNo() - syncBlockHeight >= mNeedDownloadTagsSize){
                mStatus = Status.PROGRESS;
                setStatesTagStartNo(tags.getStartNo());
                if(!isDownloaded(tags.getStartNo())){
                    isDirectlyInit = false;
                    setDownloaded(tags.getStartNo(), false);
                    reLoadStateTags();
                    FileUtil.deleteFile(destFileDir);
                    Logger.d("delete states-tag file dir");
                    startDownload(tags.getStartNo(), tags.getStates());
                    startDownload(tags.getStartNo(), tags.getBlocks());
                }else if(isStateTagLoadedFailed()){
                    // Whether or not the end of chain loading
                    isDirectlyInit = false;
                    initBlockChain(true);
                }
            }
        }
        if(isDirectlyInit){
            initBlockChain(false);
        }
    }

    private void startDownload(long startNo, List<String> links) {
        if(links != null && links.size() > 0){
            for (String link : links) {
                if(StringUtil.isNotEmpty(link)){
                    Logger.d("start download tags: %s", link);
                    startDownload(startNo, link);
                }
            }
        }
    }

    private void startDownload(long startNo, String link) {
        try {
            Uri uri = Uri.parse(link);
            String destFileName = getFileName(uri);
            if(StringUtil.isEmpty(destFileName)){
                return;
            }
            DownloadCallback callback = new DownloadCallback(startNo, link);
            callback.setFileData(destFileDir, destFileName);
            Map<String,String> params = UriUtil.getQueryMap(uri);
            Call<ResponseBody> call = mRetrofitBuilder
                .baseUrl(UriUtil.getBaseUrl(uri))
                .build()
                .create(FileLoad.class)
                .loadTagsFile(UriUtil.getPath(uri, false), params);
            call.enqueue(callback);
            // Download 1 times and add 1, default 0
            int time = getDownloadTime(link);
            time += 1;
            mMapResult.put(link, time);
        }catch (Exception e){
            Logger.e(e,"init startDownload Failure %s", link);
        }
    }

    private int getDownloadTime(String link) {
        int retryTime = 0;
        if(mMapResult.containsKey(link)){
            Integer time = mMapResult.get(link);
            if(time != null){
                retryTime = time;
            }
        }
        return retryTime;
    }

    private String getFileName(Uri uri) {
        if(uri != null && StringUtil.isNotEmpty(uri.getQueryParameter("filename"))){
            return uri.getQueryParameter("filename");
        }
        return "";
    }

    private void handleDownloadSuccess(long startNo) {
        boolean isHaveFail = false;
        for(Integer value : mMapResult.values()){
            if(value == mRetryTime){
                isHaveFail = true;
            }
            if(value < mRetryTime){
                return;
            }
        }
        if(isHaveFail){
            handleDownloadFail();
            return;
        }
        mStatus = Status.SUCCESS;
        Logger.d("handleDownloadSuccess");
        // Pre-initialization assignment
        setDownloaded(startNo, true);

        setStateTagLoaded(startNo, 0);
        initBlockChain(true);
    }

    private void handleDownloadFail() {
        mStatus = Status.FAIL;
        Logger.d("handleDownloadFail");
        initBlockChain(false);
    }

    private synchronized void initBlockChain(boolean isReset) {
        if(isReset){
            Logger.d("delete block chain file dir");
            MiningUtil.deleteBlockChainFileDir();
        }
        Logger.d("initBlockChain");
        MyApplication.getRemoteConnector().initBlockChain();
        // Initialization post empty
        mStatus = null;
    }

    class DownloadCallback extends FileCallback {

        private String link;
        private long startNo;

        DownloadCallback(long startNo, String link){
            this.startNo = startNo;
            this.link = link;
            this.isDeleteDir = false;
        }
        @Override
        public void onSuccess(File file) {
            Logger.d("download success=%s", link);
            mMapResult.put(link, mRetryTime + 1);
            handleDownloadSuccess(startNo);
        }

        @Override
        public void onLoading(long progress, long total) {

        }

        @Override
        public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
            super.onFailure(call, t);
            Logger.d("download %s failure: %S", link, t.getMessage());
            int time = getDownloadTime(link);
            if(time < mRetryTime){
                try {
                    Thread.sleep(mSleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startDownload(startNo, link);
            }else{
                handleDownloadSuccess(startNo);
            }
        }
    }

    private OkHttpClient initOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.retryOnConnectionFailure(false);
        builder.readTimeout(6, TimeUnit.HOURS);
        builder.writeTimeout(6, TimeUnit.HOURS);
        builder.connectTimeout(6, TimeUnit.HOURS);
        builder.networkInterceptors().add(chain -> {
            Response originalResponse = chain.proceed(chain.request());
            return originalResponse
                    .newBuilder()
                    .body(new FileResponseBody(originalResponse))
                    .build();
        });
        return builder.build();
    }

    public static void reLoadStateTags() {
        Map<String, ?> map = SharedPreferencesHelper.getInstance().getSP().getAll();
        for (String key : map.keySet()) {
            if(key.startsWith(TransmitKey.STATES_TAG_LOADED) ){
                SharedPreferencesHelper.getInstance().putInt(key, -1);
            }
        }
    }

    public static void reDownLoadStateTags() {
        Map<String, ?> map = SharedPreferencesHelper.getInstance().getSP().getAll();
        for (String key : map.keySet()) {
            if(key.startsWith(TransmitKey.STATES_TAG_DOWNLOAD) ){
                SharedPreferencesHelper.getInstance().putBoolean(key, false);
            }
        }
    }

    private static boolean isStateTagLoadedFailed(){
        String key = TransmitKey.STATES_TAG_LOADED + "_" + getStatesTagStartNo();
        int statesTagLoaded = SharedPreferencesHelper.getInstance()
                .getInt(key, -1);
        return statesTagLoaded == 2;
    }

    public static boolean isStateTagLoading(){
        String key = TransmitKey.STATES_TAG_LOADED + "_" + getStatesTagStartNo();
        int statesTagLoaded = SharedPreferencesHelper.getInstance()
                .getInt(key, -1);
        return statesTagLoaded == 0;
    }

    /**
     * is state tag loaded completed
     * -1: states not loaded
     * 0: states loading
     * 1: states loaded completed
     * 2: states loaded failed
     * */
    public static void setStateTagLoaded(long startNo, int status){
        SharedPreferencesHelper.getInstance()
                .putInt(TransmitKey.STATES_TAG_LOADED+ "_" + startNo, status);
    }

    private static void setDownloaded(long startNo, boolean status){
        String key = TransmitKey.STATES_TAG_DOWNLOAD + "_" + startNo;
        SharedPreferencesHelper.getInstance()
                .putBoolean(key, status);
    }

    private static boolean isDownloaded(long startNo){
        String key = TransmitKey.STATES_TAG_DOWNLOAD + "_" + startNo;
        return  SharedPreferencesHelper.getInstance()
                .getBoolean(key, false);
    }

    private static long getStatesTagStartNo(){
        String key = TransmitKey.STATES_TAG_START_NO;
        return  SharedPreferencesHelper.getInstance()
                .getLong(key, 0);
    }

    private static void setStatesTagStartNo(long startNo){
        String key = TransmitKey.STATES_TAG_START_NO;
        SharedPreferencesHelper.getInstance()
                .putLong(key, startNo);
    }
}