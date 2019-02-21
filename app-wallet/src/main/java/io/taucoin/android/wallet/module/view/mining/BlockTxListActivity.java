package io.taucoin.android.wallet.module.view.mining;

import android.os.Bundle;
import android.widget.ListView;

import com.mofei.tau.R;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.module.bean.TxBean;
import io.taucoin.android.wallet.module.presenter.MiningPresenter;
import io.taucoin.android.wallet.widget.ToolbarView;

public class BlockTxListActivity extends BaseActivity {

    @BindView(R.id.tool_bar)
    ToolbarView toolBar;
    @BindView(R.id.list_view_help)
    ListView listViewHelp;
    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;

    private MiningPresenter mPresenter;
    private TxAdapter mAdapter;
    private List<TxBean> mDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_list);
        ButterKnife.bind(this);
        mPresenter = new MiningPresenter();
        initView();
        getData();
    }

    private void getData() {
        mPresenter.getBlockList();
        List<TxBean> dataList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TxBean txBean = new TxBean();
            txBean.setTxid("02A9AB50632BD35C31ECCDAB986A5462DF2448626F6687DDFADF0EA6036D3A230B");
            txBean.setFee("0.01");
            dataList.add(txBean);
        }
        loadData(dataList);
    }


    public void loadData(List<TxBean> data) {
        if(data != null){
            mDataList.clear();
            mDataList.addAll(data);
            mAdapter.setListData(data);
        }
        refreshLayout.setEnableRefresh(false);
        refreshLayout.setEnableLoadmore(false);
        refreshLayout.finishRefresh();
    }

    private void initView() {
        toolBar.setTitle(R.string.block_transaction);
        mAdapter = new TxAdapter();
        listViewHelp.setAdapter(mAdapter);
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        getData();
    }
}