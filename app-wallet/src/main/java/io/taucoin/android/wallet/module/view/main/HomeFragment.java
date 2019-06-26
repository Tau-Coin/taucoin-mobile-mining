package io.taucoin.android.wallet.module.view.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.taucoin.android.wallet.BuildConfig;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseFragment;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.presenter.MiningPresenter;
import io.taucoin.android.wallet.module.service.NotifyManager;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.module.view.main.iview.IHomeView;
import io.taucoin.android.wallet.module.view.manage.ImportKeyActivity;
import io.taucoin.android.wallet.net.callback.CommonObserver;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.DialogManager;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.PermissionUtils;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.android.wallet.widget.ProgressView;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.DrawablesUtil;
import io.taucoin.foundation.util.StringUtil;
import io.taucoin.foundation.util.permission.EasyPermissions;

public class HomeFragment extends BaseFragment implements IHomeView {

    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;
    @BindView(R.id.iv_mining_switch)
    Switch ivMiningSwitch;
    @BindView(R.id.iv_sync_switch)
    Switch ivSyncSwitch;
    @BindView(R.id.tv_mining_switch)
    TextView tvMiningSwitch;
    @BindView(R.id.tv_mining_income)
    TextView tvMiningIncome;
    @BindView(R.id.tv_power)
    TextView tvPower;
    @BindView(R.id.tv_synchronized)
    TextView tvSynchronized;
    @BindView(R.id.tv_chain_height_title)
    TextView tvChainHeightTitle;
    @BindView(R.id.tv_mined)
    TextView tvMined;
    @BindView(R.id.tv_mined_title)
    TextView tvMinedTitle;
    @BindView(R.id.tv_cpu)
    TextView tvCPU;
    @BindView(R.id.tv_memory)
    TextView tvMemory;
    @BindView(R.id.tv_data_storage)
    TextView tvDataStorage;
    @BindView(R.id.tv_chain_height)
    TextView tvChainHeight;
    @BindView(R.id.tv_balance)
    TextView tvBalance;
    @BindView(R.id.tv_power_title)
    TextView tvPowerTitle;
    @BindView(R.id.tv_miners)
    TextView tvMiners;
    @BindView(R.id.iv_mining_power)
    ProgressView ivMiningPower;
    @BindView(R.id.iv_mining_sync)
    ProgressView ivMiningSync;

    private MiningPresenter miningPresenter;
    public static boolean mIsToast = false;

    @Override
    public View getViewLayout(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        ButterKnife.bind(this, view);
        miningPresenter = new MiningPresenter(this);
        initView();
        MyApplication.getRemoteConnector().init();
        handleMiningView();
        TxService.startTxService(TransmitKey.ServiceType.GET_HOME_DATA);
        TxService.startTxService(TransmitKey.ServiceType.GET_INFO);
        TxService.startTxService(TransmitKey.ServiceType.GET_BLOCK_HEIGHT);
        requestWriteLOgPermissions();
        return view;
    }

    @OnClick({R.id.iv_mining_switch, R.id.iv_sync_switch, R.id.tv_chain_height, R.id.tv_mined,
            R.id.tv_chain_height_title, R.id.tv_mined_title, R.id.iv_right, R.id.tv_power_title,
            R.id.iv_mining_power, R.id.iv_mining_sync})
    public void onClick(View view) {
        if (!UserUtil.isImportKey() && view.getId() != R.id.tv_power_title) {
            Intent intent = new Intent(getActivity(), ImportKeyActivity.class);
            startActivityForResult(intent, 100);
            return;
        }
        switch (view.getId()) {
            case R.id.iv_mining_switch:
                if(UserUtil.isImportKey()){
                    throttleFirst(view, 2);
                    starOrStopMining();
                }else{
                    ActivityUtil.startActivity(getActivity(), ImportKeyActivity.class);
                }
                break;
            case R.id.iv_sync_switch:
                if(UserUtil.isImportKey()){
                    throttleFirst(view, 2);
                    starOrStopSync();
                }else{
                    ActivityUtil.startActivity(getActivity(), ImportKeyActivity.class);
                }
                break;
            case R.id.tv_chain_height_title:
            case R.id.tv_chain_height:
                if(UserUtil.isImportKey()){
                    ActivityUtil.openUri(getActivity(), TransmitKey.ExternalUrl.BLOCKS_INFO);
                }else{
                    ActivityUtil.startActivity(getActivity(), ImportKeyActivity.class);
                }
                break;
            case R.id.tv_mined_title:
            case R.id.tv_mined:
                if(UserUtil.isImportKey()){
                    String address = MyApplication.getKeyValue().getAddress();
                    String uriStr = TransmitKey.ExternalUrl.MINING_INFO + address;
                    ActivityUtil.openUri(getActivity(), uriStr);
                }else{
                    ActivityUtil.startActivity(getActivity(), ImportKeyActivity.class);
                }
                break;
            case R.id.iv_right:
                ProgressManager.showProgressDialog(getActivity());
                mIsToast = true;
                onRefresh(null);
                break;
            case R.id.tv_power_title:
                DialogManager.showTipDialog(getActivity(), R.string.home_mining_power_tips);
                break;
            case R.id.iv_mining_power:
                DialogManager.showTipDialog(getActivity(), R.string.mining_power_zero);
                break;
            case R.id.iv_mining_sync:
                DialogManager.showTipDialog(getActivity(), R.string.mining_sync_complete);
                break;
            default:
                break;
        }
    }

    @OnTouch({R.id.iv_mining_switch})
    boolean onMiningSwitchTouch(){
        if(ivSyncSwitch != null && !ivSyncSwitch.isChecked()){
            ToastUtils.showShortToast(R.string.mining_need_sync_open);
            return true;
        }
        return false;
    }

    private void starOrStopSync() {
        if(ivSyncSwitch == null){
            return;
        }
        boolean isOn = ivSyncSwitch.isChecked();

        if(isOn){
            MyApplication.getRemoteConnector().init();
            MyApplication.getRemoteConnector().startSyncAll();
        }else{
            MyApplication.getRemoteConnector().stopSyncAll();
            if(ivMiningSwitch.isChecked()){
                MyApplication.getRemoteConnector().stopBlockForging();
                ivMiningSwitch.setChecked(false);
                NotifyManager.getInstance().sendNotify(TransmitKey.MiningState.Stop);
            }
        }
        String syncState = isOn ? TransmitKey.MiningState.Start : TransmitKey.MiningState.Stop;
        if(BuildConfig.DEBUG){
            requestWriteLOgPermissions();
        }
        miningPresenter.updateSyncState(syncState);
    }

    private void starOrStopMining() {
        if(ivMiningSwitch == null){
            return;
        }
        boolean isOn = ivMiningSwitch.isChecked();
        if(isOn){
            MyApplication.getRemoteConnector().init();
            MyApplication.getRemoteConnector().startBlockForging();
        }else{
            MyApplication.getRemoteConnector().stopBlockForging();
        }
        String miningState = isOn ? TransmitKey.MiningState.Start : TransmitKey.MiningState.Stop;
        if(BuildConfig.DEBUG){
            requestWriteLOgPermissions();
        }
        miningPresenter.updateMiningState(miningState);
        NotifyManager.getInstance().sendNotify(miningState);
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent object) {
        if (object == null) {
            return;
        }
        switch (object.getCode()) {
            case ALL:
            case BALANCE:
                if (refreshLayout != null && refreshLayout.isRefreshing()) {
                    refreshLayout.finishRefresh();
                }
                UserUtil.setUserInfo(tvPower, tvMiningIncome, tvMined);
                UserUtil.setBalance(tvBalance);
                // If the connection with the mining process is interrupted, restore the connection
                MyApplication.getRemoteConnector().restoreConnection();
                break;
            case BLOCK_HEIGHT:
                MiningUtil.setBlockHeight(tvChainHeight);
                break;
            case MINING_INFO:
            case MINING_STATE:
                handleMiningView();
                break;
            case MINING_NOTIFY:
                if(ivMiningSwitch != null && object.getData() != null){
                    ivMiningSwitch.setChecked((Boolean) object.getData());
                }
                break;
            case APPLICATION_INFO:
                UserUtil.setApplicationInfo(tvCPU, tvMemory, tvDataStorage, object.getData());
                break;
            case MINING_INCOME:
                BlockInfo blockInfo = (BlockInfo) object.getData();
                showMiningView(blockInfo, false);
                break;
            default:
                break;
        }
    }

    @Override
    public void initView() {
        refreshLayout.setEnableLoadmore(false);
        refreshLayout.setOnRefreshListener(this);
        onEvent(EventBusUtil.getMessageEvent(MessageEvent.EventCode.ALL));
        DrawablesUtil.setEndDrawable(tvChainHeightTitle, R.mipmap.icon_right_grey, 8);
        DrawablesUtil.setEndDrawable(tvMinedTitle, R.mipmap.icon_right_grey, 8);
        DrawablesUtil.setEndDrawable(tvPowerTitle, R.mipmap.icon_log_help, 12);

        initTopSwitch();
    }

    public synchronized void handleMiningView() {
        if (UserUtil.isImportKey()) {
            miningPresenter.getMiningInfo(new LogicObserver<BlockInfo>() {
                @Override
                public void handleData(BlockInfo blockInfo) {
                    showMiningView(blockInfo);
                }
            });
        }else{
            showMiningView(null);
        }
    }

    private void initTopSwitch() {
        KeyValue keyValue = MyApplication.getKeyValue();
        if (keyValue != null) {
            boolean isSyncStart = StringUtil.isSame(keyValue.getSyncState(), TransmitKey.MiningState.Start);
            boolean isMiningStart = StringUtil.isSame(keyValue.getMiningState(), TransmitKey.MiningState.Start);
            ivSyncSwitch.setChecked(isSyncStart);
            ivMiningSwitch.setChecked(isMiningStart);
            if(isSyncStart){
                if(isMiningStart){
                    MyApplication.getRemoteConnector().startSyncAll();
                }
                MyApplication.getRemoteConnector().startBlockForging();
            }
        }
    }

    public void showMiningView(BlockInfo blockInfo){
        showMiningView(blockInfo, true);
    }

    public void showMiningView(BlockInfo blockInfo, boolean isRefreshMined){
        long blockSync = 0;
        long blockHeight = 0;
        long miners = 0;

        if(blockInfo != null){
            blockHeight = blockInfo.getBlockHeight();
            blockSync = blockInfo.getBlockSync();
            miners = StringUtil.getIntString(blockInfo.getMinerNo());

            UserUtil.setMiningConditions(ivMiningPower, ivMiningSync, blockInfo, !isRefreshMined);
        }
        String blockSyncStr = FmtMicrometer.fmtPower(blockSync);
        tvSynchronized.setText(blockSyncStr);
        String blockHeightStr = FmtMicrometer.fmtPower(blockHeight);
        tvChainHeight.setText(blockHeightStr);
        String minersStr = FmtMicrometer.fmtPower(miners);
        tvMiners.setText(minersStr);
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        TxService.startTxService(TransmitKey.ServiceType.GET_BALANCE);
        TxService.startTxService(TransmitKey.ServiceType.GET_BLOCK_HEIGHT);
        handleMiningView();

        if (!UserUtil.isImportKey()) {
            refreshLayout.finishRefresh(1000);
        }
    }

    private void requestWriteLOgPermissions() {
        boolean isAndroidQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
        if(BuildConfig.DEBUG && !isAndroidQ){
            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            EasyPermissions.requestPermissions(getActivity(),
                    this.getString(R.string.permission_tip_upgrade_denied),
                    PermissionUtils.REQUEST_PERMISSIONS_STORAGE, permission);
        }
    }

    public void throttleFirst(View view, long delaySeconds){
        view.setEnabled(false);
        Observable.timer(delaySeconds, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new CommonObserver<Long>() {
                @Override
                public void onComplete() {
                    view.setEnabled(true);
                }
            });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PermissionUtils.REQUEST_PERMISSIONS_STORAGE:
                if (grantResults.length > 0) {
                    if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                        PermissionUtils.checkUserBanPermission(getActivity(), permissions[0], R.string.permission_tip_upgrade_never_ask_again);
                    }
                }
                break;

            default:
                break;
        }
    }
}