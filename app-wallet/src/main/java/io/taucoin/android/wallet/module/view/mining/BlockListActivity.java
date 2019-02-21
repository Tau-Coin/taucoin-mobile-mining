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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.db.entity.MiningInfo;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.widget.ToolbarView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_list);
        ButterKnife.bind(this);
        initView();
        getData();
    }

    private void getData() {
        KeyValue keyValue = MyApplication.getKeyValue();
        if(keyValue != null){
            // TODO test delete
            keyValue.setBlockSynchronized(101);
            if(mPageNo == 0){
                mDataSize = keyValue.getBlockSynchronized();
                // TODO test delete
                keyValue.getMiningInfos().clear();
                for (int i = 100; i >= 0; i--) {
                    if(i%2 == 1){
                        MiningInfo entry = new MiningInfo();
                        entry.setBlockNo("" + i);
                        keyValue.getMiningInfos().add(entry);
                    }
                }

                if(keyValue.getMiningInfos() != null){
                    mAdapter.setListData(keyValue.getMiningInfos());
                    if(mIsMe){
                        mDataSize = keyValue.getMiningInfos().size();
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
        Intent intent = new Intent();
        intent.putExtra(TransmitKey.ID, position);
        Object tagObj = view.getTag(R.id.block_list_tag);
        String tag = tagObj == null ? "" : tagObj.toString();
        intent.putExtra(TransmitKey.TITLE, tag);

        ActivityUtil.startActivity(intent, this, BlockDetailActivity.class);
    }
}