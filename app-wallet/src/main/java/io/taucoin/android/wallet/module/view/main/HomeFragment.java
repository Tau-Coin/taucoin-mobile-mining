package io.taucoin.android.wallet.module.view.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import io.taucoin.android.wallet.BuildConfig;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseFragment;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.MiningReward;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.presenter.MiningPresenter;
import io.taucoin.android.wallet.module.service.NotifyManager;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.module.view.main.iview.IHomeView;
import io.taucoin.android.wallet.module.view.manage.ImportKeyActivity;
import io.taucoin.android.wallet.module.view.mining.BlockListActivity;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.PermissionUtils;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.util.ResourcesUtil;
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
    ProgressView ivMiningSwitch;
    @BindView(R.id.tv_mining_switch)
    LoadingTextView tvMiningSwitch;
    @BindView(R.id.tv_mining_income)
    TextView tvMiningIncome;
    @BindView(R.id.tv_power)
    TextView tvPower;
    @BindView(R.id.tv_synchronized)
    TextView tvSynchronized;
    @BindView(R.id.tv_mined)
    TextView tvMined;
    @BindView(R.id.tv_mining_transaction)
    TextView tvMiningTransaction;
    @BindView(R.id.ll_mining_tx)
    View llMiningTx;
    @BindView(R.id.list_view_mining_tx)
    ListView listViewMiningTx;
    @BindView(R.id.tv_error_msg)
    TextView tvErrorMsg;

    private MiningPresenter miningPresenter;
    private MiningRewardAdapter mMiningRewardAdapter;
    public static boolean mIsToast = false;
    private int mPageNo = 1;
    private String mTime;
//    private long time = -1;

    @Override
    public View getViewLayout(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        ButterKnife.bind(this, view);
        miningPresenter = new MiningPresenter(this);
        initView();
        handleMiningView();
        handleMiningRewardView();
        TxService.startTxService(TransmitKey.ServiceType.GET_HOME_DATA);
        TxService.startTxService(TransmitKey.ServiceType.GET_INFO);
        return view;
    }


    @OnClick({R.id.iv_mining_switch, R.id.tv_mining_transaction, R.id.tv_synchronized, R.id.tv_mined, R.id.iv_right})
    public void onClick(View view) {
        if (!UserUtil.isImportKey()) {
            Intent intent = new Intent(getActivity(), ImportKeyActivity.class);
            startActivityForResult(intent, 100);
            return;
        }
        switch (view.getId()) {
            case R.id.iv_mining_switch:
                if(UserUtil.isImportKey()){
                    starOrStopMining(false);
                }else{
                    ActivityUtil.startActivity(getActivity(), ImportKeyActivity.class);
                }
                break;
            case R.id.tv_mining_transaction:
                showMiningTransactionView();
                break;
            case R.id.tv_synchronized:
            case R.id.tv_mined:
                if(MyApplication.getRemoteConnector().isInit()){
                    Intent intent = new Intent();
                    intent.putExtra(TransmitKey.TYPE, view.getId() == R.id.tv_mined ? 1 : 0);
                    ActivityUtil.startActivity(intent, getActivity(), BlockListActivity.class);
                }
                break;
            case R.id.iv_right:
                ProgressManager.showProgressDialog(getActivity());
                mIsToast = true;
                onRefresh(null);
                break;
            default:
                break;
        }
    }

    @OnItemClick(R.id.list_view_mining_tx)
    void onItemClick(AdapterView<?> parent, View view, int position, long id){
        String txId = mMiningRewardAdapter.getTxHash(position);
        String tauExplorerTxUr = TransmitKey.ExternalUrl.TAU_EXPLORER_TX_URL;
        tauExplorerTxUr += txId;
        ActivityUtil.openUri(view.getContext(), tauExplorerTxUr);
    }

    private void starOrStopMining(boolean isNotice) {
        if(!isNotice){
            requestWriteLOgPermissions();
            miningPresenter.updateMiningState();
        }
//        time = -1;
        waitStartOrStop();
    }

    private void waitStartOrStop() {
        NotifyManager.getInstance().sendNotify(TransmitKey.MiningState.LOADING);
        ivMiningSwitch.setEnabled(false);
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
                UserUtil.setPower(tvPower);
                break;
            case BLOCK_HEIGHT:
                MiningUtil.setBlockHeight(tvSynchronized);
                break;
            case MINING_INIT:
                handleMiningView();
            case MINING_INFO:
                handleMiningView(false);
                break;
            case MINING_STATE:
                ivMiningSwitch.setEnabled(true);
                handleMiningView(false);
                break;
//            case FORGED_TIME:
//                if(object.getData() != null){
//                    time = (long) object.getData();
//                    if(time > 0){
////                        showMiningMsg();
//                    }
//                }
//                break;
            case NOTIFY_MINING:
                starOrStopMining(true);
                break;
            case MINING_REWARD:
                handleMiningRewardView();
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
        DrawablesUtil.setEndDrawable(tvMiningTransaction, R.mipmap.icon_tx_down, 16);

        mMiningRewardAdapter = new MiningRewardAdapter();
        listViewMiningTx.setAdapter(mMiningRewardAdapter);

    }

    private void showMiningTransactionView() {
        if(tvMiningTransaction == null){
            return;
        }
        int tag = StringUtil.getIntTag(tvMiningTransaction);
        tag = tag == 0 ? 1 : 0;
        tvMiningTransaction.setTag(tag);
        int mipmap = tag == 1 ? R.mipmap.icon_tx_up : R.mipmap.icon_tx_down;
        DrawablesUtil.setEndDrawable(tvMiningTransaction, mipmap, 16);
        llMiningTx.setVisibility(tag == 1 ? View.VISIBLE : View.GONE);
        handleMiningRewardView();

    }

    public void showMiningView(BlockInfo blockInfo){
        int blockSync  = 0;
        int blockMined  = 0;
        if(blockInfo != null){
            blockSync = blockInfo.getBlockSync();
            blockMined = MiningUtil.parseMinedBlocks(blockInfo);
        }
        tvSynchronized.setText(String.valueOf(blockSync));
        tvMined.setText(String.valueOf(blockMined));
    }

    @Override
    public void handleMiningView() {
        handleMiningView(true);
    }

    public synchronized void handleMiningView(boolean isNeedInit) {
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
                    boolean isInit = MyApplication.getRemoteConnector().isInit();
                    boolean isError = MyApplication.getRemoteConnector().isError();

                    if(isStart && isNeedInit){
                        if(!isInit){
                            waitStartOrStop();
                        }
                        MyApplication.getRemoteConnector().init();
                        refreshOffOnView(TransmitKey.MiningState.LOADING);
                    }else if(isStart && !isInit){
                        refreshOffOnView(TransmitKey.MiningState.LOADING);
                    }else if(isStart && isError){
                        refreshOffOnView(TransmitKey.MiningState.ERROR);
                    }else if(!isStart){
                        MyApplication.getRemoteConnector().cancelRemoteConnector();
                        refreshOffOnView(TransmitKey.MiningState.Stop);
                        ivMiningSwitch.setEnabled(true);
                    }else{
                        refreshOffOnView(TransmitKey.MiningState.Start);
                    }
                }
            });
        }else{
            showMiningView(null);
        }
    }

    private void handleMiningRewardView(){
        if(mPageNo == 1){
            mTime = DateUtil.getCurrentTime();
        }
        miningPresenter.getMiningRewards(mPageNo, mTime);
    }

    @Override
    public void handleMiningRewardView(List<MiningReward> miningRewards){
        String miningIncome = MiningUtil.parseMiningIncome(miningRewards);
        tvMiningIncome.setText(miningIncome);

        int tag = StringUtil.getIntTag(tvMiningTransaction);
        if(tag == 1){
            mMiningRewardAdapter.setListData(miningRewards, mPageNo != 1);
            refreshLayout.setEnableLoadmore(miningRewards.size() % TransmitKey.PAGE_SIZE == 0 && miningRewards.size() > 0);
        }else{
            refreshLayout.setEnableLoadmore(false);
        }
    }

    private void refreshOffOnView(String miningState) {
        int state = R.string.home_mining_off;
        int color = R.color.color_grey;
        tvErrorMsg.setVisibility(View.INVISIBLE);
        tvErrorMsg.setText("");
        if(StringUtil.isSame(miningState, TransmitKey.MiningState.Start)){
            ivMiningSwitch.setOn();
            state = R.string.home_mining_on;
            color = R.color.color_young;
            tvMiningSwitch.setNormalText(state);
        }else if(StringUtil.isSame(miningState, TransmitKey.MiningState.LOADING)){
            ivMiningSwitch.setConnecting();
            state = R.string.home_mining_connecting;
            color = R.color.color_young;
            tvMiningSwitch.setLoadingText(ResourcesUtil.getText(state));
        }else if(StringUtil.isSame(miningState, TransmitKey.MiningState.ERROR)){
            ivMiningSwitch.setError();
            tvErrorMsg.setText(MyApplication.getRemoteConnector().getErrorMsg());
            tvErrorMsg.setVisibility(View.VISIBLE);
            state = R.string.home_mining_on;
            color = R.color.color_red;
            tvMiningSwitch.setNormalText(state);
        }else {
            ivMiningSwitch.setOff();
            tvMiningSwitch.setNormalText(state);
        }
        tvMiningSwitch.setTextColor(ResourcesUtil.getColor(color));
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        TxService.startTxService(TransmitKey.ServiceType.GET_BALANCE);
        handleMiningView(false);
        mPageNo = 1;
        handleMiningRewardView();

        if (!UserUtil.isImportKey()) {
            refreshLayout.finishRefresh(1000);
        }
    }

    @Override
    public void onLoadmore(RefreshLayout refreshlayout) {
        mPageNo += 1;
        handleMiningRewardView();
    }

    @Override
    public void onResume() {
        super.onResume();
        requestWriteLOgPermissions();
    }

    @Override
    public void onDestroy() {
        if(tvMiningSwitch != null){
            tvMiningSwitch.closeLoading();
            ivMiningSwitch.closeLoading();
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