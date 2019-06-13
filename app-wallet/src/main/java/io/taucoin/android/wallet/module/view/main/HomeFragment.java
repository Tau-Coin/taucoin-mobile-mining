package io.taucoin.android.wallet.module.view.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
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
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.DialogManager;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.PermissionUtils;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.util.ResourcesUtil;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.android.wallet.widget.LoadingTextView;
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
    @BindView(R.id.tv_mining_switch)
    TextView tvMiningSwitch;
    @BindView(R.id.tv_mining_income)
    TextView tvMiningIncome;
    @BindView(R.id.tv_power)
    TextView tvPower;
    @BindView(R.id.tv_synchronized)
    TextView tvSynchronized;
    @BindView(R.id.tv_synchronized_title)
    TextView tvSynchronizedTitle;
    @BindView(R.id.tv_mined)
    LoadingTextView tvMined;
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
    @BindView(R.id.tv_booting)
    LoadingTextView tvBooting;

    private MiningPresenter miningPresenter;
    public static boolean mIsToast = false;

    @Override
    public View getViewLayout(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        ButterKnife.bind(this, view);
        miningPresenter = new MiningPresenter(this);
        initView();
        handleMiningView();
        TxService.startTxService(TransmitKey.ServiceType.GET_HOME_DATA);
        TxService.startTxService(TransmitKey.ServiceType.GET_INFO);
        TxService.startTxService(TransmitKey.ServiceType.GET_BLOCK_HEIGHT);
        return view;
    }

    @OnClick({R.id.iv_mining_switch, R.id.tv_synchronized, R.id.tv_mined,
            R.id.tv_synchronized_title, R.id.tv_mined_title, R.id.iv_right, R.id.tv_power_title,
            R.id.iv_mining_power, R.id.iv_mining_sync, })
    public void onClick(View view) {
        if (!UserUtil.isImportKey() && view.getId() != R.id.tv_power_title) {
            Intent intent = new Intent(getActivity(), ImportKeyActivity.class);
            startActivityForResult(intent, 100);
            return;
        }
        switch (view.getId()) {
            case R.id.iv_mining_switch:
                if(UserUtil.isImportKey()){
                    starOrStopMining();
                }else{
                    ActivityUtil.startActivity(getActivity(), ImportKeyActivity.class);
                }
                break;
            case R.id.tv_synchronized_title:
            case R.id.tv_synchronized:
            case R.id.tv_mined_title:
            case R.id.tv_mined:
                if(UserUtil.isImportKey()){
                    ActivityUtil.openUri(getActivity(), TransmitKey.ExternalUrl.BLOCKS_INFO);
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

    @OnCheckedChanged({R.id.iv_mining_switch})
    void onMiningSwitchChanged(boolean isChecked){
        if(tvMiningSwitch != null){
            tvMiningSwitch.setText(isChecked ? R.string.home_mining_on : R.string.home_mining_off);
            int colorRes = isChecked ? R.color.color_yellow : R.color.color_grey;
            tvMiningSwitch.setTextColor(ResourcesUtil.getColor(colorRes));
        }
    }

    private void starOrStopMining() {
        if(ivMiningSwitch == null){
            return;
        }
        boolean isOn = ivMiningSwitch.isChecked();
        handleMiningView();

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
                break;
            case BLOCK_HEIGHT:
                MiningUtil.setBlockHeight(tvChainHeight);
                break;
            case MINING_INIT:
                if(ivMiningSwitch != null){
                    ivMiningSwitch.setEnabled(true);
                    tvBooting.setVisibility(View.INVISIBLE);
                }
                handleMiningView();
            case MINING_INFO:
                handleMiningView();
                break;
            case MINING_STATE:
                handleMiningView();
                break;
            case MINING_REWARD:
                onRefresh(null);
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
        DrawablesUtil.setEndDrawable(tvSynchronizedTitle, R.mipmap.icon_right_grey, 8);
        DrawablesUtil.setEndDrawable(tvMinedTitle, R.mipmap.icon_right_grey, 8);
        DrawablesUtil.setEndDrawable(tvPowerTitle, R.mipmap.icon_log_help, 12);

        KeyValue keyValue = MyApplication.getKeyValue();
        if (keyValue != null) {
            boolean isStart = StringUtil.isSame(keyValue.getMiningState(), TransmitKey.MiningState.Start);
            if(isStart){
                ivMiningSwitch.setChecked(true);
                MyApplication.getRemoteConnector().init();
            }
        }
    }

    public void showMiningView(BlockInfo blockInfo){
        showMiningView(blockInfo, true);
    }

    public void showMiningView(BlockInfo blockInfo, boolean isRefreshMined){
        int blockSync = 0;
        int blockHeight = 0;
        int miners = 0;

        if(blockInfo != null){
            blockHeight = blockInfo.getBlockHeight();
            blockSync = blockInfo.getBlockSync();
            miners = StringUtil.getIntString(blockInfo.getMinerNo());

            boolean isStopped = ivMiningPower.isEnabled() || ivMiningSync.isEnabled();
            UserUtil.setMiningConditions(ivMiningPower, ivMiningSync, ivMiningSwitch, blockInfo, !isRefreshMined);
            boolean isNeedRestart = !ivMiningPower.isEnabled() && !ivMiningSync.isEnabled();
            if(MyApplication.getRemoteConnector().isInit() && isStopped && isNeedRestart){
                MyApplication.getRemoteConnector().startBlockForging();
            }
        }
        tvSynchronized.setText(String.valueOf(blockSync));
        tvChainHeight.setText(String.valueOf(blockHeight));
        tvMiners.setText(String.valueOf(miners));
    }

    public synchronized void handleMiningView() {
        if (UserUtil.isImportKey()) {
            miningPresenter.getMiningInfo(new LogicObserver<BlockInfo>() {
                @Override
                public void handleData(BlockInfo blockInfo) {
                    showMiningView(blockInfo);

                    boolean isStart = false;
                    KeyValue keyValue = MyApplication.getKeyValue();
                    if (keyValue != null) {
                        isStart = StringUtil.isSame(keyValue.getMiningState(), TransmitKey.MiningState.Start);
                    }
                    ivMiningSwitch.setChecked(isStart);
                    if(isStart){
                        boolean isCanInit = MyApplication.getRemoteConnector().isCanInit();
                        if(isCanInit){
                            MyApplication.getRemoteConnector().init();
                        }
                        if(!MyApplication.getRemoteConnector().isInit()){
                            ivMiningSwitch.setEnabled(false);
                            String booting = ResourcesUtil.getText(R.string.home_mining_booting);
                            tvBooting.setLoadingText(booting);
                            tvBooting.setVisibility(View.VISIBLE);
                        }
                    }else{
                        MyApplication.getRemoteConnector().cancelRemoteConnector();
                    }
                }
            });
        }else{
            showMiningView(null);
        }
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

    @Override
    public void onResume() {
        super.onResume();
        requestWriteLOgPermissions();
    }

    @Override
    public void onDestroy() {
        if(tvMiningSwitch != null){
            tvMined.closeLoading();
            tvBooting.closeLoading();
        }
        super.onDestroy();
    }

    private void requestWriteLOgPermissions() {
        if(BuildConfig.DEBUG){
            String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            EasyPermissions.requestPermissions(getActivity(),
                    this.getString(R.string.permission_tip_upgrade_denied),
                    PermissionUtils.REQUEST_PERMISSIONS_STORAGE, permission);
        }
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