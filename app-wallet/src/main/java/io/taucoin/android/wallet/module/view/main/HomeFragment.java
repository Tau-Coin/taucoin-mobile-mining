package io.taucoin.android.wallet.module.view.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.service.events.NextBlockForgedPOTDetail;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseFragment;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.bean.MinerListBean;
import io.taucoin.android.wallet.module.bean.ParticipantListBean;
import io.taucoin.android.wallet.module.presenter.MiningPresenter;
import io.taucoin.android.wallet.module.service.NotifyManager;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.module.view.main.iview.IHomeView;
import io.taucoin.android.wallet.module.view.manage.ImportKeyActivity;
import io.taucoin.android.wallet.net.callback.CommonObserver;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.DialogManager;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.util.ResourcesUtil;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.android.wallet.widget.DashboardLayout;
import io.taucoin.android.wallet.widget.LoadingTextView;
import io.taucoin.android.wallet.widget.ProgressView;
import io.taucoin.android.wallet.widget.ScrollDisabledListView;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.DrawablesUtil;
import io.taucoin.foundation.util.StringUtil;

public class HomeFragment extends BaseFragment implements IHomeView {

    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;
    @BindView(R.id.iv_mining_switch)
    Switch ivMiningSwitch;
    @BindView(R.id.tv_mining_income)
    TextView tvMiningIncome;
    @BindView(R.id.dashboard_layout)
    DashboardLayout dashboardLayout;
    @BindView(R.id.tv_cpu)
    TextView tvCPU;
    @BindView(R.id.tv_storage)
    TextView tvStorage;
    @BindView(R.id.tv_memory)
    TextView tvMemory;
    @BindView(R.id.tv_data_storage)
    TextView tvDataStorage;
    @BindView(R.id.tv_balance)
    TextView tvBalance;
    @BindView(R.id.tv_miners_online)
    TextView tvMinersOnline;
    @BindView(R.id.tv_miners_online_title)
    TextView tvMinersOnlineTitle;
    @BindView(R.id.tv_irreparable_error)
    TextView tvIrreparableError;
    @BindView(R.id.tv_download)
    TextView tvDownload;
    @BindView(R.id.iv_download)
    ProgressView ivDownload;
    @BindView(R.id.tv_verify)
    TextView tvVerify;
    @BindView(R.id.iv_verify)
    ProgressView ivVerify;
    @BindView(R.id.tv_mining_rank)
    TextView tvMiningRank;
    @BindView(R.id.cb_wifi_only)
    CheckBox cbWifiOnly;
    @BindView(R.id.ll_miner)
    View llMiner;
    @BindView(R.id.ll_participant)
    View llParticipant;
    @BindView(R.id.tv_success_requires)
    TextView tvSuccessRequires;
    @BindView(R.id.tv_participant_history)
    TextView tvParticipantHistory;
    @BindView(R.id.tv_mining_history)
    TextView tvMiningHistory;
    @BindView(R.id.tv_next_block_no)
    TextView tvNextBlockNo;
    @BindView(R.id.tv_forged_time)
    LoadingTextView tvForgedTime;
    @BindView(R.id.tv_forged_time_title)
    TextView tvForgedTimeTitle;
    @BindView(R.id.tv_current_condition)
    TextView tvCurrentCondition;
    @BindView(R.id.ll_current_condition)
    View llCurrentCondition;
    @BindView(R.id.rb_miner)
    RadioButton rbMiner;
    @BindView(R.id.part_list_view)
    ScrollDisabledListView partListView;
    @BindView(R.id.miner_list_view)
    ScrollDisabledListView minerListView;
    @BindView(R.id.tv_block_chain_data)
    TextView tvBlockChainData;
    @BindView(R.id.tv_median_fee)
    TextView tvMedianFee;
    @BindView(R.id.tv_txs_pool)
    TextView tvTxsPool;
    @BindView(R.id.tv_circulation)
    TextView tvCirculation;
    @BindView(R.id.tv_history_miner_reward)
    TextView tvHistoryMinerReward;
    @BindView(R.id.tv_history_tx_reward)
    TextView tvHistoryTxReward;
    @BindView(R.id.rl_mining_time)
    View rlMiningTime;
    @BindView(R.id.rl_avg_time)
    View rlAvgTime;
    @BindView(R.id.tv_hit_tip)
    TextView tvHitTip;

    private RewardAdapter minerRewardAdapter;
    private RewardAdapter partRewardAdapter;
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
        loadRewardData();
        refreshNextBlockView(null);
        TxService.startTxService(TransmitKey.ServiceType.GET_HOME_DATA);
        TxService.startTxService(TransmitKey.ServiceType.GET_INFO);
        TxService.startTxService(TransmitKey.ServiceType.GET_BLOCK_HEIGHT);
        return view;
    }

    @OnClick({R.id.iv_mining_switch, R.id.cb_wifi_only, R.id.iv_right, R.id.rb_miner, R.id.rb_participant,
            R.id.tv_participant_history, R.id.tv_mining_history, R.id.tv_next_block_no_title,
            R.id.tv_history_tx_reward_title, R.id.tv_history_miner_reward_title,})
    public void onClick(View view) {
        if (!UserUtil.isImportKey() && view.getId() != R.id.cb_wifi_only) {
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
            case R.id.cb_wifi_only:
                SharedPreferencesHelper.getInstance().putBoolean(TransmitKey.FORGING_WIFI_ONLY, cbWifiOnly.isChecked());
                break;
            case R.id.iv_right:
                ProgressManager.showProgressDialog(getActivity());
                mIsToast = true;
                onRefresh(null);
                break;
            case R.id.rb_miner:
            case R.id.rb_participant:
                if(rbMiner != null){
                    boolean isMiner = rbMiner.isChecked();
                    llMiner.setVisibility(isMiner ? View.VISIBLE : View.GONE);
                    llParticipant.setVisibility(!isMiner ? View.VISIBLE : View.GONE);
                }
                break;
            case R.id.tv_mining_history:
                if(UserUtil.isImportKey()){
                    String address = MyApplication.getKeyValue().getAddress();
                    String uriStr = TransmitKey.ExternalUrl.MINER_HISTORY + address;
                    ActivityUtil.openUri(getActivity(), uriStr);
                }else{
                    ActivityUtil.startActivity(getActivity(), ImportKeyActivity.class);
                }
                break;
            case R.id.tv_participant_history:
                if(UserUtil.isImportKey()){
                    String address = MyApplication.getKeyValue().getAddress();
                    String uriStr = TransmitKey.ExternalUrl.PARTICIPANT_HISTORY + address;
                    ActivityUtil.openUri(getActivity(), uriStr);
                }else{
                    ActivityUtil.startActivity(getActivity(), ImportKeyActivity.class);
                }
                break;
            case R.id.tv_next_block_no_title:
                DialogManager.showTipDialog(getActivity(), R.string.home_average_block_time_tip);
                break;
            case R.id.tv_history_miner_reward_title:
                DialogManager.showTipDialog(getActivity(), R.string.home_history_miner_reward_tip);
                break;
            case R.id.tv_history_tx_reward_title:
                DialogManager.showTipDialog(getActivity(), R.string.home_history_tx_reward_tip);
                break;
            default:
                break;
        }
    }

    private void starOrStopMining() {
        if(ivMiningSwitch == null){
            return;
        }
        boolean isOn = ivMiningSwitch.isChecked();
        String miningState = isOn ? TransmitKey.MiningState.Start : TransmitKey.MiningState.Stop;
        miningPresenter.updateMiningState(miningState);
        NotifyManager.getInstance().sendNotify(miningState);
    }

    @Override
    public void handleMiningSwitch(){
        if(UserUtil.isImportKey()){
            boolean isOn = StringUtil.isSame(TransmitKey.MiningState.Start,
                    MyApplication.getKeyValue().getMiningState());
            if(isOn){
                MyApplication.getRemoteConnector().init();
                MyApplication.getRemoteConnector().startBlockForging();
            }else{
                refreshNextBlockView(null);
                MyApplication.getRemoteConnector().stopBlockForging();
            }
        }
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
                    refreshLayout.finishRefresh(200);
                }
                UserUtil.setHitTip(tvHitTip);
                UserUtil.setMiningIncome(tvMiningIncome);
                UserUtil.setBalance(tvBalance, true);
                // If the connection with the mining process is interrupted, restore the connection
                MyApplication.getRemoteConnector().restoreConnection();
                break;
            case BLOCK_HEIGHT:
            case MINING_INFO:
            case MINING_STATE:
                handleMiningView();
                break;
            case MINING_REWARD:
                if(tvNextBlockNo != null){
                    tvNextBlockNo.setTag(0);
                    loadRewardData();
                }
                break;
            case MINING_NOTIFY:
                if(ivMiningSwitch != null && object.getData() != null){
                    ivMiningSwitch.setChecked((Boolean) object.getData());
                }
                break;
            case APPLICATION_INFO:
                UserUtil.setApplicationInfo(tvCPU, tvMemory, tvDataStorage, tvStorage, object.getData());
                break;
            case MINING_INCOME:
                BlockInfo blockInfo = (BlockInfo) object.getData();
                showMiningView(blockInfo, false);
                break;
            case IRREPARABLE_ERROR:
                if(object.getData() != null && tvIrreparableError != null){
                    tvIrreparableError.setVisibility(View.VISIBLE);
                    tvIrreparableError.setTag(object.getData());
                }
                break;
            case FORGED_POT_DETAIL:
                refreshNextBlockView(object.getData());
                break;
            default:
                break;
        }
    }

    @Override
    public void initView() {
        refreshLayout.setEnableLoadmore(false);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setEnableNestedScroll(true);
        onEvent(EventBusUtil.getMessageEvent(MessageEvent.EventCode.ALL));
        initMiningSwitch();
        UserUtil.initSuccessRequires(tvSuccessRequires);
        UserUtil.setHitTip(tvHitTip);
        DrawablesUtil.setUnderLine(tvParticipantHistory);
        DrawablesUtil.setUnderLine(tvMiningHistory);
        minerRewardAdapter = new RewardAdapter();
        partRewardAdapter = new RewardAdapter();
        minerListView.setAdapter(minerRewardAdapter);
        partListView.setAdapter(partRewardAdapter);
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

    public void loadRewardData(){
        miningPresenter.getParticipantInfo();
        miningPresenter.getMinerHistory(new LogicObserver<List<MinerListBean.MinerBean>>(){
            @Override
            public void handleData(List<MinerListBean.MinerBean> data) {
                if(minerRewardAdapter != null && data != null){
                    minerRewardAdapter.setMinerListData(data);
                }
            }
        });
        miningPresenter.getParticipantHistory(new LogicObserver<List<ParticipantListBean.ParticipantBean>>(){
            @Override
            public void handleData(List<ParticipantListBean.ParticipantBean> data) {
                if(partRewardAdapter != null && data != null){
                    partRewardAdapter.setPartListData(data);
                }
            }
        });
    }

    private void initMiningSwitch() {
        KeyValue keyValue = MyApplication.getKeyValue();
        if (keyValue != null) {
            boolean isMiningStart = StringUtil.isSame(keyValue.getMiningState(), TransmitKey.MiningState.Start);
            ivMiningSwitch.setChecked(isMiningStart);
            if(isMiningStart){
                MyApplication.getRemoteConnector().startBlockForging();
            }
        }

        boolean isWifiOnly = SharedPreferencesHelper.getInstance().getBoolean(TransmitKey.FORGING_WIFI_ONLY, false);
        cbWifiOnly.setChecked(isWifiOnly);
    }

    public void showMiningView(BlockInfo blockInfo){
        showMiningView(blockInfo, true);
    }

    public void showMiningView(BlockInfo blockInfo, boolean isRefreshMined){
        UserUtil.setMiningConditions(tvVerify, ivVerify, blockInfo);
        UserUtil.setPowerConditions(dashboardLayout, blockInfo, !isRefreshMined);
        UserUtil.setDownloadConditions(tvDownload, ivDownload, tvBlockChainData, blockInfo);
        UserUtil.setMinersOnline(tvMinersOnline, tvMinersOnlineTitle, blockInfo);
        UserUtil.setMiningRankAndOther(tvMiningRank, tvTxsPool, tvMedianFee, tvCirculation, blockInfo);

        long blockSync = blockInfo != null ? blockInfo.getBlockSync() : 0;
        int errorBlock = StringUtil.getIntTag(tvIrreparableError);
        if(blockSync >= errorBlock && tvIrreparableError.getVisibility() != View.GONE){
            tvIrreparableError.setVisibility(View.GONE);
        }

        if(blockInfo != null){
            long blockHeight = StringUtil.getIntTag(tvNextBlockNo);
            if(blockHeight != 0 && blockInfo.getBlockHeight() > blockHeight){
                loadRewardData();
            }
            UserUtil.setHistoryParticipantReward(tvHistoryMinerReward, tvHistoryTxReward);
            UserUtil.setNextBlockNo(tvNextBlockNo, blockInfo);
        }
    }

    private void refreshNextBlockView(Object data){
        if(llCurrentCondition != null){
            llCurrentCondition.setVisibility(data == null ? View.GONE : View.VISIBLE);
        }
        if(tvForgedTime != null && tvCurrentCondition != null){
            rlAvgTime.setVisibility(data == null ? View.VISIBLE : View.GONE);
            rlMiningTime.setVisibility(data == null ? View.GONE : View.VISIBLE);
            if(data != null){
                String title = ResourcesUtil.getText(R.string.home_estimated_block_time);
                title = String.format(title, UserUtil.getLastThreeAddress());
                tvForgedTimeTitle.setText(title);

                NextBlockForgedPOTDetail detail = (NextBlockForgedPOTDetail) data;
                tvCurrentCondition.setTag(data);
                long timeInternal = detail.timePoint - detail.previousBlockTime;
                UserUtil.setCurrentCondition(tvCurrentCondition, timeInternal);
                tvForgedTime.setCountDown(timeInternal, count -> {
                    count = detail.timePoint - count - detail.previousBlockTime;
                    UserUtil.setCurrentCondition(tvCurrentCondition, count);
                });
            }
        }
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        TxService.startTxService(TransmitKey.ServiceType.GET_BALANCE);
        TxService.startTxService(TransmitKey.ServiceType.GET_BLOCK_HEIGHT);
        handleMiningView();
        loadRewardData();

        if (!UserUtil.isImportKey()) {
            refreshLayout.finishRefresh(1000);
        }
    }

    public void throttleFirst(View view, long delaySeconds){
        view.setEnabled(false);
        Observable.timer(delaySeconds, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(new CommonObserver<Long>() {
                @Override
                public void onComplete() {
                    view.setEnabled(true);
                }
            });
    }
}