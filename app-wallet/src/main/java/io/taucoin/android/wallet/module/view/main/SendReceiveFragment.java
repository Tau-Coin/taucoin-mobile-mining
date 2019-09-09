package io.taucoin.android.wallet.module.view.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.taucoin.android.wallet.R;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.BaseFragment;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.presenter.TxPresenter;
import io.taucoin.android.wallet.module.presenter.UserPresenter;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.module.view.main.iview.ISendReceiveView;
import io.taucoin.android.wallet.module.view.manage.AddressBookActivity;
import io.taucoin.android.wallet.module.view.manage.ImportKeyActivity;
import io.taucoin.android.wallet.module.view.tx.IncreaseActivity;
import io.taucoin.android.wallet.module.view.tx.SendActivity;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.CopyManager;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.android.wallet.util.DialogManager;
import io.taucoin.android.wallet.util.EventBusUtil;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.android.wallet.widget.EmptyLayout;
import io.taucoin.android.wallet.widget.banner.BannerLayout;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.DimensionsUtil;
import io.taucoin.foundation.util.StringUtil;

public class SendReceiveFragment extends BaseFragment implements ISendReceiveView {

    @BindView(R.id.btn_send)
    Button btnSend;
    @BindView(R.id.iv_tx_log_tips)
    ImageView ivTxLogTips;
    @BindView(R.id.list_view_log)
    ExpandableListView listViewLog;
    @BindView(R.id.empty_layout)
    EmptyLayout emptyLayout;
    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;
    @BindView(R.id.ll_tip)
    LinearLayout llTip;
    @BindView(R.id.banner_layout)
    BannerLayout bannerLayout;
    View seeMoreView;

    private TxPresenter mTxPresenter;
    private HistoryExpandableListAdapter mAdapter;
    private WebBannerAdapter mBannerAdapter;
    private UserPresenter mUserPresenter;
    private int mPageNo = 1;
    private String mTime;
    private boolean mIsToast = false;

    @Override
    public View getViewLayout(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send_receive, container, false);
        butterKnifeBinder(this, view);
        mUserPresenter = new UserPresenter();
        mTxPresenter = new TxPresenter(this);
        initView();
        initListener();
        initData();
        return view;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        loadTopThreeAddressView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loadTopThreeAddressView();
    }

    @Override
    public void initData() {
        if(UserUtil.isImportKey()){
            ProgressManager.showProgressDialog(getActivity());
        }
        onEvent(EventBusUtil.getMessageEvent(MessageEvent.EventCode.BALANCE));
        onRefresh(null);
    }

    @Override
    public void initView() {
        mAdapter = new HistoryExpandableListAdapter();
        listViewLog.setAdapter(mAdapter);
        refreshLayout.setEnableLoadmore(false);
        refreshLayout.setEnableAutoLoadmore(false);
        refreshLayout.setEnableLoadmoreWhenContentNotFull(true);

        mBannerAdapter = new WebBannerAdapter();
        mBannerAdapter.setOnBannerItemClickListener(new BannerLayout.OnBannerItemClickListener(){

            @Override
            public void onViewClick(int position, View view) {
                KeyValue keyValue = mBannerAdapter.getItemData(position);
                if(keyValue == null){
                    ActivityUtil.startActivity(getActivity(), ImportKeyActivity.class);
                }else{
                    copyData(keyValue);
                }
            }

            @Override
            public void onItemClick(int position) {
                KeyValue keyValue = mBannerAdapter.getItemData(position);
                if(keyValue != null){
                    mUserPresenter.switchAddress(getActivity(), keyValue);
                }
            }
        });

        bannerLayout.setAdapter(mBannerAdapter);
        loadTopThreeAddressView();

        seeMoreView = LinearLayout.inflate(getActivity(), R.layout.list_view_footer, null);
        listViewLog.addFooterView(seeMoreView);
        TextView tvSeeMore = seeMoreView.findViewById(R.id.tv_see_more);
        tvSeeMore.setOnClickListener(v -> {
            String address = SharedPreferencesHelper.getInstance().getString(TransmitKey.ADDRESS, "");
            String seeMoreUrl = TransmitKey.ExternalUrl.TAU_EXPLORER_SEE_MORE + address;
            ActivityUtil.openUri(v.getContext(), seeMoreUrl);
        });
    }

    @OnClick({R.id.btn_send, R.id.iv_tx_log_tips, R.id.iv_right, R.id.btn_increase, R.id.tv_manage_address})
    void onClick(View view) {
        KeyValue keyValue = MyApplication.getKeyValue();
        if (keyValue == null && view.getId() != R.id.iv_tx_log_tips) {
            Intent intent = new Intent(getActivity(), ImportKeyActivity.class);
            startActivityForResult(intent, 100);
            return;
        }
        switch (view.getId()) {
            case R.id.btn_send:
                Intent intent = new Intent(getActivity(), SendActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_increase:
                intent = new Intent(getActivity(), IncreaseActivity.class);
                startActivity(intent);
                break;
            case R.id.iv_tx_log_tips:
                DialogManager.showTipDialog(getActivity(), R.string.tx_log_tips);
                break;
            case R.id.iv_right:
                mIsToast = true;
                ProgressManager.showProgressDialog(getActivity());
                onRefresh(null);
                break;
            case R.id.tv_manage_address:
                intent = new Intent(getActivity(), AddressBookActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }

    }

    private void copyData(KeyValue keyValue) {
        if(keyValue != null){
            CopyManager.copyText(keyValue.getAddress());
            ToastUtils.showShortToast(R.string.keys_address_copy);
        }
    }

    @Override
    public void initListener() {
        listViewLog.setOnGroupClickListener((parent, v, groupPosition, id) -> false);
        listViewLog.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> false);
        listViewLog.setOnGroupExpandListener(groupPosition -> {
            int groupCount = listViewLog.getExpandableListAdapter().getGroupCount();
            for (int i = 0; i < groupCount; i++) {
                if (groupPosition != i) {
                    listViewLog.collapseGroup(i);
                }
            }
        });
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setOnLoadmoreListener(this);
    }

    @Override
    public void loadTransactionHistory(List<TransactionHistory> txHistories) {
        if(mAdapter == null){
            return;
        }
        if(txHistories == null){
            txHistories = new ArrayList<>();
        }
        mAdapter.setHistoryList(txHistories, mPageNo != 1);
        emptyLayout.setVisibility(mAdapter.getData().size() == 0 ? View.VISIBLE : View.GONE);
        llTip.setVisibility(mAdapter.getData().size() != 0 ? View.VISIBLE : View.GONE);
        boolean isLoadMore = txHistories.size() % TransmitKey.PAGE_SIZE == 0 && txHistories.size() > 0;
        refreshLayout.setEnableLoadmore(isLoadMore);

        if(seeMoreView != null){
            boolean isMoreShow = !isLoadMore && mAdapter.getData().size() > 0;
            seeMoreView.setVisibility(isMoreShow ? View.VISIBLE : View.GONE);
            AbsListView.LayoutParams layoutParams = (AbsListView.LayoutParams) seeMoreView.getLayoutParams();
            if(layoutParams != null){
                layoutParams.height = 0;
                if(isMoreShow){
                    layoutParams.height = DimensionsUtil.dip2px(seeMoreView.getContext(), 40);
                }
                seeMoreView.setLayoutParams(layoutParams);
            }
        }
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent object) {
        if(object == null){
            ProgressManager.closeProgressDialog();
            return;
        }
        switch (object.getCode()){
            case ALL:
            case BALANCE:
                boolean isRecover = true;
                if(object.getData() != null && object.getData() instanceof Boolean){
                    isRecover = (boolean) object.getData();
                }
                loadTopThreeAddressView(isRecover);
                break;
            case TRANSACTION:
                startRefresh();
                break;
            default:
                break;
        }
    }

    @Override
    public void onLoadmore(RefreshLayout refreshlayout) {
        mPageNo += 1;
        mTxPresenter.queryTransactionHistory(mPageNo, mTime);
    }

    private synchronized void startRefresh() {
        mPageNo = 1;
        mTime = DateUtil.getCurrentTime();
        if(mTxPresenter != null){
            mTxPresenter.queryTransactionHistory(mPageNo, mTime);
        }
    }

    private void loadTopThreeAddressView() {
        loadTopThreeAddressView(true);
    }

    private void loadTopThreeAddressView(boolean isRecover) {
        mUserPresenter.getAddressList("", new LogicObserver<List<KeyValue>>(){
            @Override
            public void handleData(List<KeyValue> keyValues) {
                if(bannerLayout == null || mBannerAdapter == null){
                    return;
                }
                mBannerAdapter.setDataList(keyValues);
                bannerLayout.setAdapter(mBannerAdapter);
                bannerLayout.recoverPosition(isRecover ? -1 : 0);
            }
        });
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        if(UserUtil.isImportKey() && mTxPresenter != null){
            TxService.startTxService(TransmitKey.ServiceType.GET_BALANCE);
            TxService.startTxService(TransmitKey.ServiceType.GET_RAW_TX);
            mTxPresenter.getTxRecords(new LogicObserver<Boolean>() {
                @Override
                public void handleData(Boolean isSuccess) {
                    ProgressManager.closeProgressDialog();
                    startRefresh();

                    if(!isSuccess){
                        handleError();
                    }
                }

                synchronized void handleError() {
                    if(mIsToast){
                        ToastUtils.showShortToast(R.string.common_refresh_failed);
                        mIsToast = false;
                    }
                }

                @Override
                public void handleError(int code, String msg) {
                    ProgressManager.closeProgressDialog();
                    startRefresh();
                    handleError();
                }
            });
        }else {
            refreshLayout.finishRefresh(1000);
            ProgressManager.closeProgressDialog();
        }
    }

    @Override
    public void finishRefresh() {
        refreshLayout.finishRefresh();
    }

    @Override
    public void finishLoadMore() {
        refreshLayout.finishLoadmore();
    }

}