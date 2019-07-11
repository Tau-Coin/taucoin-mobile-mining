//package io.taucoin.android.wallet.module.view.mining;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.AdapterView;
//import android.widget.ImageView;
//import android.widget.ListView;
//import android.widget.RelativeLayout;
//import android.widget.TextView;
//
//import io.taucoin.android.wallet.R;
//import com.scwang.smartrefresh.layout.SmartRefreshLayout;
//import com.scwang.smartrefresh.layout.api.RefreshLayout;
//
//import org.greenrobot.eventbus.Subscribe;
//import org.greenrobot.eventbus.ThreadMode;
//
//import butterknife.BindView;
//import butterknife.ButterKnife;
//import butterknife.OnClick;
//import butterknife.OnItemClick;
//import io.taucoin.android.service.events.BlockEventData;
//import io.taucoin.android.wallet.MyApplication;
//import io.taucoin.android.wallet.base.BaseActivity;
//import io.taucoin.android.wallet.base.TransmitKey;
//import io.taucoin.android.wallet.db.entity.BlockInfo;
//import io.taucoin.android.wallet.module.bean.MessageEvent;
//import io.taucoin.android.wallet.module.presenter.MiningPresenter;
//import io.taucoin.android.wallet.util.ActivityUtil;
//import io.taucoin.android.wallet.util.ProgressManager;
//import io.taucoin.android.wallet.widget.ToolbarView;
//import io.taucoin.foundation.net.callback.LogicObserver;
//import io.taucoin.foundation.util.StringUtil;
//
//public class BlockListActivity extends BaseActivity {
//
//    @BindView(R.id.list_view_help)
//    ListView listViewHelp;
//    @BindView(R.id.refresh_layout)
//    SmartRefreshLayout refreshLayout;
//    @BindView(R.id.tool_bar)
//    ToolbarView toolBar;
//    @BindView(R.id.rl_filter)
//    RelativeLayout rlFilter;
//    @BindView(R.id.tv_filter)
//    TextView tvFilter;
//    @BindView(R.id.iv_filter)
//    ImageView ivFilter;
//    @BindView(R.id.tv_filter_select)
//    TextView tvFilterSelect;
//
//    private BlockAdapter mAdapter;
//    private int mPageNo = 0;
//    private int mDataSize = 0;
//    private boolean mIsMe = false;
//    private MiningPresenter miningPresenter;
//    private BlockInfo mBlockInfo;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_block_list);
//        ButterKnife.bind(this);
//        miningPresenter = new MiningPresenter();
//        initView();
//        getData();
//    }
//
//    private void getData() {
//        if(mPageNo == 0){
//            miningPresenter.getMiningInfo(new LogicObserver<BlockInfo>() {
//                @Override
//                public void handleData(BlockInfo blockInfo) {
//                    mBlockInfo = blockInfo;
//                    updateListView();
//                }
//
//                @Override
//                public void handleError(int code, String msg) {
//                    super.handleError(code, msg);
//                    updateListView();
//                }
//            });
//        }else{
//            updateListView();
//        }
//    }
//
//    private void updateListView() {
//        if(mBlockInfo != null){
//            if(mPageNo == 0){
//                mDataSize = mBlockInfo.getBlockSync() + 1;
//
//                if(mBlockInfo.getMiningInfo() != null){
//                    mAdapter.setListData(mBlockInfo.getMiningInfo());
//                    if(mIsMe){
//                        mDataSize = mBlockInfo.getMiningInfo().size();
//                    }
//                }
//            }
//            int numPageSize = (mPageNo + 1) * TransmitKey.PAGE_SIZE;
//            boolean canLoadMore = mDataSize > numPageSize;
//            refreshLayout.setEnableLoadmore(canLoadMore);
//            int listSize = canLoadMore ? numPageSize : mDataSize;
//            mAdapter.setListSize(listSize, mDataSize, mIsMe);
//        }
//        refreshLayout.finishRefresh();
//        refreshLayout.finishLoadmore();
//    }
//
//    private void initView() {
//        toolBar.setTitle(R.string.block_list_title);
//        rlFilter.setVisibility(View.VISIBLE);
//        refreshLayout.setOnRefreshListener(this);
//        refreshLayout.setOnLoadmoreListener(this);
//        mAdapter = new BlockAdapter();
//        listViewHelp.setAdapter(mAdapter);
//
//        int type = getIntent().getIntExtra(TransmitKey.TYPE, 0);
//        if(type == 1){
//            onClick(tvFilterSelect);
//        }else{
//            getData();
//        }
//    }
//
//    @Override
//    public void onRefresh(RefreshLayout refreshlayout) {
//        if(refreshlayout == null){
//            mAdapter = new BlockAdapter();
//            listViewHelp.setAdapter(mAdapter);
//        }
//        mPageNo = 0;
//        getData();
//    }
//
//    @Override
//    public void onLoadmore(RefreshLayout refreshlayout) {
//        mPageNo += 1;
//        getData();
//    }
//
//    @OnClick({R.id.rl_filter, R.id.tv_filter_select})
//    void onClick(View view){
//        switch (view.getId()){
//            case R.id.rl_filter:
//                int visible = View.GONE;
//                if(tvFilterSelect.getVisibility() == visible){
//                    visible = View.VISIBLE;
//                }
//                tvFilterSelect.setVisibility(visible);
//                tvFilterSelect.setText(!mIsMe ? R.string.block_list_me : R.string.block_list_all);
//                ivFilter.setImageResource(visible == View.GONE ? R.mipmap.icon_rank_down : R.mipmap.icon_up);
//                break;
//            case R.id.tv_filter_select:
//                mIsMe = !mIsMe;
//                tvFilter.setText(mIsMe ? R.string.block_list_me : R.string.block_list_all);
//                tvFilterSelect.setVisibility(View.GONE);
//                ivFilter.setImageResource(R.mipmap.icon_rank_down);
//                onRefresh(null);
//                break;
//        }
//    }
//
//    @OnItemClick(R.id.list_view_help)
//    void onItemClick(AdapterView<?> parent, View view, int position, long id){
//        Object tagObj = view.getTag(R.id.block_list_tag);
//        String tag = tagObj == null ? "" : tagObj.toString();
//        int number = StringUtil.getIntString(tag);
//        ProgressManager.showProgressDialog(this);
//        MyApplication.getRemoteConnector().getBlockByNumber(number);
//    }
//
//    @Override
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEvent(MessageEvent msgEvent) {
//        if(msgEvent != null && msgEvent.getCode() == MessageEvent.EventCode.GET_BLOCK
//                && ProgressManager.isShowing()){
//            ProgressManager.closeProgressDialog();
//            Bundle bundle = (Bundle) msgEvent.getData();
//            if(bundle != null){
//                bundle.setClassLoader(io.taucoin.android.service.events.BlockEventData.class.getClassLoader());
//                BlockEventData block = bundle.getParcelable("block");
//                if(block != null){
//                    Intent intent = new Intent();
//                    intent.putExtra(TransmitKey.BEAN, block);
//                    ActivityUtil.startActivity(intent, this, BlockDetailActivity.class);
//                }
//            }
//        }
//    }
//}