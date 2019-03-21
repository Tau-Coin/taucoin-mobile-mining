package io.taucoin.android.wallet.module.view.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mofei.tau.BuildConfig;
import com.mofei.tau.R;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.taucoin.android.wallet.MyApplication;
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
import io.taucoin.android.wallet.module.view.manage.ProfileActivity;
import io.taucoin.android.wallet.module.view.mining.BlockListActivity;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.PermissionUtils;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.android.wallet.widget.ItemTextView;
import io.taucoin.android.wallet.widget.LoadingTextView;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;
import io.taucoin.foundation.util.permission.EasyPermissions;

public class HomeFragment extends BaseFragment implements IHomeView {

    @BindView(R.id.tv_nick)
    TextView tvNick;
    @BindView(R.id.tv_balance)
    TextView tvBalance;
    @BindView(R.id.tv_power)
    TextView tvPower;
    @BindView(R.id.btn_mining)
    Button btnMining;
    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;
    @BindView(R.id.ll_mining)
    View llMining;
    @BindView(R.id.tv_block_height)
    ItemTextView tvBlockHeight;
    @BindView(R.id.tv_block_synchronized)
    ItemTextView tvBlockSynchronized;
    @BindView(R.id.tv_block_mined)
    ItemTextView tvBlockMined;
    @BindView(R.id.tv_mining_income)
    ItemTextView tvMiningIncome;
    @BindView(R.id.tv_mining_details)
    ItemTextView tvMiningDetails;
    @BindView(R.id.tv_mining_msg)
    LoadingTextView tvMiningMsg;

    private MiningPresenter miningPresenter;
    private AlertDialog mDialog;
    private ProgressViewHolder mViewHolder;
    private long time = -1;

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        UserUtil.setNickName(tvNick);
    }

    @OnClick({R.id.tv_nick, R.id.btn_mining, R.id.tv_mining_details})
    public void onClick(View view) {
        if (!UserUtil.isImportKey()) {
            Intent intent = new Intent(getActivity(), ImportKeyActivity.class);
            startActivityForResult(intent, 100);
            return;
        }
        switch (view.getId()) {
            case R.id.tv_nick:
                ActivityUtil.startActivity(getActivity(), ProfileActivity.class);
                break;
            case R.id.btn_mining:
                starOrStopMining(false);
                break;
            case R.id.tv_mining_details:
                ActivityUtil.startActivity(getActivity(), BlockListActivity.class);
                break;
            default:
                break;
        }
    }

    private void starOrStopMining(boolean isNotice) {
        if(!isNotice){
            requestWriteLOgPermissions();
            miningPresenter.updateMiningState();
        }
        time = -1;
        waitStartOrStop();
        TxService.startTxService(TransmitKey.ServiceType.GET_BLOCK_HEIGHT);
    }

    private void waitStartOrStop() {
        NotifyManager.getInstance().sendNotify(TransmitKey.MiningState.LOADING);
        btnMining.setEnabled(false);
        btnMining.setBackgroundResource(R.drawable.grey_rect_round_bg);
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent object) {
        if (object == null) {
            return;
        }
        switch (object.getCode()) {
            case ALL:
                UserUtil.setBalance(tvBalance);
                UserUtil.setPower(tvPower);
                UserUtil.setNickName(tvNick);
                break;
            case BALANCE:
                if (refreshLayout != null && refreshLayout.isRefreshing()) {
                    refreshLayout.finishRefresh();
                }
                UserUtil.setBalance(tvBalance);
                UserUtil.setPower(tvPower);
                break;
            case NICKNAME:
                UserUtil.setNickName(tvNick);
                break;
            case BLOCK_HEIGHT:
                MiningUtil.setBlockHeight(tvBlockHeight);
                break;
            case MINING_INIT:
                handleMiningView();
            case MINING_INFO:
                handleMiningView(false);
                break;
            case MINING_STATE:
                btnMining.setEnabled(true);
                handleMiningView(false);
                break;
            case FORGED_TIME:
                if(object.getData() != null){
                    time = (long) object.getData();
                    showMiningMsg();
                }
                break;
            case NOTIFY_MINING:
                starOrStopMining(true);
                break;
            case MINING_INIT_PROGRESS:
                if(object.getData() != null){
                    int progress = (int) object.getData();
                    showInitProgressDialog(progress);
                }
                break;
            default:
                break;
        }
    }

    private synchronized void showMiningMsg() {
        if(tvMiningMsg != null){
            if(UserUtil.isImportKey()){
                int msgReid = MiningUtil.getMiningMsg();
                String msg = getString(msgReid);
                if(msgReid == R.string.mining_in_progress){
                    if(time > -1){
                        msg += "\n";
                        msg += getString(R.string.mining_mining_internal);
                        tvMiningMsg.setLoadingText(msg, time);
                    }else{
                        tvMiningMsg.setNormalText(msg);
                    }
                }else{
                    tvMiningMsg.setLoadingText(msg);
                }
            }else{
                tvMiningMsg.setNormalText(R.string.mining_generation_rate);
            }
        }
    }

    @Override
    public void initView() {
        refreshLayout.setEnableLoadmore(false);
        refreshLayout.setOnRefreshListener(this);
        onEvent(EventBusUtil.getMessageEvent(MessageEvent.EventCode.ALL));
    }

    @Override
    public void handleMiningView() {
        handleMiningView(true);
    }

    public synchronized void handleMiningView(boolean isNeedInit) {
        showMiningMsg();
        if (UserUtil.isImportKey() && btnMining != null) {
            miningPresenter.getMiningInfo(new LogicObserver<BlockInfo>() {
                @Override
                public void handleData(BlockInfo blockInfo) {
                    boolean isStart = false;
                    KeyValue keyValue = MyApplication.getKeyValue();
                    if (keyValue != null) {
                        isStart = StringUtil.isSame(keyValue.getMiningState(), TransmitKey.MiningState.Start);
                        boolean isMiner = StringUtil.isNotEmpty(keyValue.getMiningState());
                        llMining.setVisibility(isMiner ? View.VISIBLE : View.GONE);

                        tvBlockHeight.setRightText(blockInfo.getBlockHeight());
                        tvBlockSynchronized.setRightText(blockInfo.getBlockSynchronized());

                        tvBlockMined.setRightText(MiningUtil.parseMinedBlocks(blockInfo));
                        tvMiningIncome.setRightText(MiningUtil.parseMiningIncome(blockInfo));
                    }
                    boolean isInit = MyApplication.getRemoteConnector().isInit();
                    boolean isSyncMe = MyApplication.getRemoteConnector().isSyncMe();
                    tvMiningDetails.setEnable(isStart && !isNeedInit && isInit && isSyncMe);

                    if(isStart && isNeedInit){
                        if(!isInit){
                            waitStartOrStop();
                        }
                        MyApplication.getRemoteConnector().init();
                    }else if(!isStart){
                        MyApplication.getRemoteConnector().cancelRemoteConnector();
                    }
                    btnMining.setText(isStart ? R.string.home_mining_stop : R.string.home_mining_start);
                    if(btnMining.isEnabled()){
                        btnMining.setBackgroundResource(isStart ? R.drawable.black_rect_round_bg : R.drawable.yellow_rect_round_bg);
                    }
                    if(!isStart){
                        tvMiningMsg.setNormalText(R.string.mining_generation_rate);
                        if(!btnMining.isEnabled()){
                            tvMiningMsg.setNormalText(R.string.mining_generation_rate);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        TxService.startTxService(TransmitKey.ServiceType.GET_BALANCE);
        TxService.startTxService(TransmitKey.ServiceType.GET_BLOCK_HEIGHT);

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
        if(tvMiningMsg != null){
            tvMiningMsg.closeLoading();
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

    private void showInitProgressDialog(int progress){
        if(mDialog == null){
            View view = LinearLayout.inflate(getActivity(), R.layout.dialog_mining_init_progress, null);
            mViewHolder = new ProgressViewHolder(view);
            mViewHolder.progressBar.setMax(100);
            mViewHolder.tvFailMsg.setVisibility(View.VISIBLE);
            mViewHolder.tvFailMsg.setText(R.string.mining_init_progress_tip);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setCancelable(false)
                    .setView(view);

            mDialog = builder.create();
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.setCancelable(false);
            mDialog.show();
        }

        if(progress > 100){
            progress = 100;
        }else if(progress < 0){
            progress = 0;
        }

        mViewHolder.progressBar.setProgress(progress);
        String progressStr = progress + "%";
        mViewHolder.tvProgress.setText(progressStr);

        if(progress == 100){
            mDialog.cancel();
            mDialog = null;
        }else {
            final int pro = progress + 1;
            new Handler().postDelayed(() -> {
                MessageEvent messageEvent = new MessageEvent();
                messageEvent.setData(pro);
                messageEvent.setCode(MessageEvent.EventCode.MINING_INIT_PROGRESS);
                EventBusUtil.post(messageEvent);
            }, 500);
        }
    }

    class ProgressViewHolder {
        @BindView(R.id.progress_bar)
        ProgressBar progressBar;
        @BindView(R.id.tv_progress)
        TextView tvProgress;
        @BindView(R.id.tv_fail_msg)
        TextView tvFailMsg;

        ProgressViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

}