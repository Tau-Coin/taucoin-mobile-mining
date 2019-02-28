package io.taucoin.android.wallet.module.view.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.module.view.main.iview.IHomeView;
import io.taucoin.android.wallet.module.view.manage.ImportKeyActivity;
import io.taucoin.android.wallet.module.view.manage.ProfileActivity;
import io.taucoin.android.wallet.module.view.mining.BlockListActivity;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.DialogManager;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.android.wallet.widget.ItemTextView;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;

public class HomeFragment extends BaseFragment implements IHomeView {

    @BindView(R.id.tv_nick)
    TextView tvNick;
    @BindView(R.id.tv_balance)
    TextView tvBalance;
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
    TextView tvMiningMsg;

    private MiningPresenter miningPresenter;

    @Override
    public View getViewLayout(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        ButterKnife.bind(this, view);
        miningPresenter = new MiningPresenter(this);
        initView();
        handleMiningView();
        TxService.startTxService(TransmitKey.ServiceType.GET_HOME_DATA);
        TxService.startTxService(TransmitKey.ServiceType.GET_INFO);
        return view;
    }

    @OnClick({R.id.tv_nick, R.id.btn_mining, R.id.tv_mining_details})
    public void onClick(View view) {
        if (!UserUtil.isImportKey()) {
            ActivityUtil.startActivity(getActivity(), ImportKeyActivity.class);
            return;
        }
        switch (view.getId()) {
            case R.id.tv_nick:
                ActivityUtil.startActivity(getActivity(), ProfileActivity.class);
                break;
            case R.id.btn_mining:
                waitStartOrStop();
                miningPresenter.updateMiningState();
                TxService.startTxService(TransmitKey.ServiceType.GET_BLOCK_HEIGHT);
                break;
            case R.id.tv_mining_details:
                if(MyApplication.getRemoteConnector().isSyncMe()){
                    ActivityUtil.startActivity(getActivity(), BlockListActivity.class);
                }else{
                    DialogManager.showSureDialog(getActivity(), R.string.bloc_in_synchronization, R.string.common_ok);
                }
                break;
            default:
                break;
        }
    }

    private void waitStartOrStop() {
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
                UserUtil.setNickName(tvNick);
                break;
            case BALANCE:
                if (refreshLayout != null && refreshLayout.isRefreshing()) {
                    refreshLayout.finishRefresh();
                }
                UserUtil.setBalance(tvBalance);
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
            default:
                break;
        }
    }

    private void showMiningMsg() {
        MyApplication.getRemoteConnector().sendMiningNotify();
        if(tvMiningMsg != null){
            if(UserUtil.isImportKey()){
                int msgReid = MiningUtil.getMiningMsg();
                String msg = getString(msgReid);
                if(msgReid == R.string.mining_in_progress){
                    msg += "\n" + getString(R.string.mining_generation_rate);
                }
                tvMiningMsg.setText(msg);
            }else{
                tvMiningMsg.setText(R.string.mining_generation_rate);
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

    public void handleMiningView(boolean isNeedInit) {
        showMiningMsg();
        if (UserUtil.isImportKey() && btnMining != null) {
            miningPresenter.getMiningInfo(new LogicObserver<BlockInfo>() {
                @Override
                public void handleData(BlockInfo blockInfo) {
                    boolean isStart = false;
                    KeyValue keyValue = MyApplication.getKeyValue();
                    if (keyValue != null) {
                        isStart = StringUtil.isSame(keyValue.getMiningState(), TransmitKey.MiningState.Start);
                        llMining.setVisibility(StringUtil.isNotEmpty(keyValue.getMiningState()) ? View.VISIBLE : View.GONE);

                        tvBlockHeight.setRightText(blockInfo.getBlockHeight());
                        tvBlockSynchronized.setRightText(blockInfo.getBlockSynchronized());

                        tvBlockMined.setRightText(MiningUtil.parseMinedBlocks(blockInfo));
                        tvMiningIncome.setRightText(MiningUtil.parseMiningIncome(blockInfo));
                    }
                    boolean isInit = MyApplication.getRemoteConnector().isInit();
                    tvMiningDetails.setEnable(isStart && !isNeedInit && isInit);

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
                        tvMiningMsg.setText(R.string.mining_generation_rate);
                        if(!btnMining.isEnabled()){
                            tvMiningMsg.setText(R.string.mining_generation_rate);
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
}