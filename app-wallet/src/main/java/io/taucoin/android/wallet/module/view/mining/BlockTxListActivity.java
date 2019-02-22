package io.taucoin.android.wallet.module.view.mining;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import com.mofei.tau.R;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.service.events.BlockEventData;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.widget.ToolbarView;
import io.taucoin.core.Block;

public class BlockTxListActivity extends BaseActivity {

    @BindView(R.id.tool_bar)
    ToolbarView toolBar;
    @BindView(R.id.list_view_help)
    ListView listViewHelp;
    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;

    private TxAdapter mAdapter;
    private BlockEventData blockEvent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_list);
        ButterKnife.bind(this);
        initView();
        loadData();
    }

    public void loadData() {
        Intent intent = getIntent();
        if(intent != null){
            intent.setExtrasClassLoader(BlockEventData.class.getClassLoader());
            blockEvent = intent.getParcelableExtra(TransmitKey.BEAN);
            if(blockEvent != null && blockEvent.block != null){
                Block blockBean = blockEvent.block;
                if(blockBean.getTransactionsList() != null){
                    mAdapter.setListData(blockBean.getTransactionsList());
                }
            }
        }
    }

    private void initView() {
        toolBar.setTitle(R.string.block_transaction);
        mAdapter = new TxAdapter();
        listViewHelp.setAdapter(mAdapter);
        refreshLayout.setEnableRefresh(false);
        refreshLayout.setEnableLoadmore(false);
        refreshLayout.finishRefresh();
    }
}