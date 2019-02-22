package io.taucoin.android.wallet.module.view.main;

import android.content.res.Configuration;
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
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.MiningInfo;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.presenter.MiningPresenter;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.module.view.main.iview.IHomeView;
import io.taucoin.android.wallet.module.view.manage.ImportKeyActivity;
import io.taucoin.android.wallet.module.view.manage.ProfileActivity;
import io.taucoin.android.wallet.module.view.mining.BlockListActivity;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.ProgressManager;
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

    private MiningPresenter miningPresenter;

    @Override
    public View getViewLayout(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        ButterKnife.bind(this, view);
        miningPresenter = new MiningPresenter(this);
        initView();
        handleMiningView();
        ProgressManager.showProgressDialog(getActivity());
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
                miningPresenter.updateMiningState();
                break;
            case R.id.tv_mining_details:
                ActivityUtil.startActivity(getActivity(), BlockListActivity.class);
                break;
            default:
                break;
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
            default:
                break;
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
        if (UserUtil.isImportKey() && btnMining != null) {
            miningPresenter.getMiningInfo(new LogicObserver<KeyValue>() {
                @Override
                public void handleData(KeyValue keyValue) {
                    boolean isStart = false;
                    if (keyValue != null) {
                        if(StringUtil.isNotEmpty(keyValue.getMiningState())){
                            TxService.startTxService(TransmitKey.ServiceType.GET_BLOCK_HEIGHT);
                        }
                        isStart = StringUtil.isSame(keyValue.getMiningState(), TransmitKey.MiningState.Start);
                        llMining.setVisibility(StringUtil.isNotEmpty(keyValue.getMiningState()) ? View.VISIBLE : View.GONE);

                        tvBlockHeight.setRightText(keyValue.getBlockHeight());
                        tvBlockSynchronized.setRightText(keyValue.getBlockSynchronized());

                        tvBlockMined.setRightText(MiningUtil.parseMinedBlocks(keyValue));
                        tvMiningIncome.setRightText(MiningUtil.parseMiningIncome(keyValue));
                    }
                    tvMiningDetails.setEnable(isStart && !isNeedInit);

                    if(isStart && isNeedInit){
                        MyApplication.getRemoteConnector().init();
                    }
                    btnMining.setText(isStart ? R.string.home_mining_stop : R.string.home_mining_start);
                    btnMining.setBackgroundResource(isStart ? R.drawable.black_rect_round_bg : R.drawable.yellow_rect_round_bg);
                }
            });
        }
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        TxService.startTxService(TransmitKey.ServiceType.GET_BALANCE);

        if (!UserUtil.isImportKey()) {
            refreshLayout.finishRefresh(1000);
        }
    }
}