package io.taucoin.android.wallet.module.view.mining;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mofei.tau.R;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import io.taucoin.android.interop.Block;
import io.taucoin.android.service.events.BlockEventData;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.MiningInfo;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.presenter.MiningPresenter;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.widget.ToolbarView;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;

public class BlockListActivity extends BaseActivity {

    @BindView(R.id.list_view_help)
    ListView listViewHelp;
    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;
    @BindView(R.id.tool_bar)
    ToolbarView toolBar;
    @BindView(R.id.rl_filter)
    RelativeLayout rlFilter;
    @BindView(R.id.tv_filter)
    TextView tvFilter;
    @BindView(R.id.iv_filter)
    ImageView ivFilter;
    @BindView(R.id.tv_filter_select)
    TextView tvFilterSelect;

    private BlockAdapter mAdapter;
    private int mPageNo = 0;
    private int mDataSize = 0;
    private boolean mIsMe = false;
    private MiningPresenter miningPresenter;
    private KeyValue mKeyValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_list);
        ButterKnife.bind(this);
        initView();
        miningPresenter = new MiningPresenter();
        getData();
    }

    private void getData() {
        if(mPageNo == 0){
            miningPresenter.getMiningInfo(new LogicObserver<KeyValue>() {
                @Override
                public void handleData(KeyValue keyValue) {
                    mKeyValue = keyValue;
                    updateListView();
                }

                @Override
                public void handleError(int code, String msg) {
                    super.handleError(code, msg);
                    updateListView();
                }
            });
        }else{
            updateListView();
        }
    }

    private void updateListView() {
        if(mKeyValue != null){
            if(mPageNo == 0){
                // TODO test delete
                mKeyValue.setBlockSynchronized(10);
                mDataSize = mKeyValue.getBlockSynchronized();
                // TODO test delete
                mKeyValue.getMiningInfos().clear();
                for (int i = mDataSize - 1; i >= mDataSize - 5; i--) {
                    if(i%2 == 1){
                        MiningInfo entry = new MiningInfo();
                        entry.setBlockNo("" + i);
                        mKeyValue.getMiningInfos().add(entry);
                    }
                }

                if(mKeyValue.getMiningInfos() != null){
                    mAdapter.setListData(mKeyValue.getMiningInfos());
                    if(mIsMe){
                        mDataSize = mKeyValue.getMiningInfos().size();
                    }
                }
            }
            int numPageSize = (mPageNo + 1) * TransmitKey.PAGE_SIZE;
            boolean canLoadMore = mDataSize > numPageSize;
            refreshLayout.setEnableLoadmore(canLoadMore);
            int listSize = canLoadMore ? numPageSize : mDataSize;
            mAdapter.setListSize(listSize, mDataSize, mIsMe);
        }
        refreshLayout.finishRefresh();
        refreshLayout.finishLoadmore();
    }

    private void initView() {
        toolBar.setTitle(R.string.block_list_title);
        rlFilter.setVisibility(View.VISIBLE);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setOnLoadmoreListener(this);
        mAdapter = new BlockAdapter();
        listViewHelp.setAdapter(mAdapter);
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        mPageNo = 0;
        getData();
    }

    @Override
    public void onLoadmore(RefreshLayout refreshlayout) {
        mPageNo += 1;
        getData();
    }

    @OnClick({R.id.rl_filter, R.id.tv_filter_select})
    void onClick(View view){
        switch (view.getId()){
            case R.id.rl_filter:
                int visible = View.GONE;
                if(tvFilterSelect.getVisibility() == visible){
                    visible = View.VISIBLE;
                }
                tvFilterSelect.setVisibility(visible);
                tvFilterSelect.setText(!mIsMe ? R.string.block_list_me : R.string.block_list_all);
                ivFilter.setImageResource(visible == View.GONE ? R.mipmap.icon_down : R.mipmap.icon_up);
                break;
            case R.id.tv_filter_select:
                mIsMe = !mIsMe;
                tvFilter.setText(mIsMe ? R.string.block_list_me : R.string.block_list_all);
                tvFilterSelect.setVisibility(View.GONE);
                onRefresh(null);
                break;
        }
    }

    @OnItemClick(R.id.list_view_help)
    void onItemClick(AdapterView<?> parent, View view, int position, long id){
        Object tagObj = view.getTag(R.id.block_list_tag);
        String tag = tagObj == null ? "" : tagObj.toString();
        int number = StringUtil.getIntString(tag);
        ProgressManager.showProgressDialog(this);
        MyApplication.getRemoteConnector().getBlockByNumber(number);
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent msgEvent) {
        if(msgEvent != null && msgEvent.getCode() == MessageEvent.EventCode.GET_BLOCK
                && ProgressManager.isShowing()){
            ToastUtils.showShortToast("onEventBlock");
            ProgressManager.closeProgressDialog();
            Bundle bundle = (Bundle) msgEvent.getData();
            if(bundle != null){
                bundle.setClassLoader(io.taucoin.android.service.events.BlockEventData.class.getClassLoader());
                BlockEventData block = bundle.getParcelable("block");
                if(block != null){
                    Intent intent = new Intent();
                    intent.putExtra(TransmitKey.BEAN, block);
                    ActivityUtil.startActivity(intent, this, BlockDetailActivity.class);
                }
            }
        }
    }
}