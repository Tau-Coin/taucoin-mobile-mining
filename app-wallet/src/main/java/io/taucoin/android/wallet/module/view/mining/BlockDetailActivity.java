package io.taucoin.android.wallet.module.view.mining;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.mofei.tau.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.module.bean.BlockBean;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.CopyManager;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.widget.ItemTextView;
import io.taucoin.android.wallet.widget.ToolbarView;
import io.taucoin.foundation.util.StringUtil;

public class BlockDetailActivity extends BaseActivity {

    @BindView(R.id.tool_bar)
    ToolbarView toolBar;
    @BindView(R.id.tv_mining_income)
    ItemTextView tvMiningIncome;
    @BindView(R.id.tv_miner)
    ItemTextView tvMiner;
    @BindView(R.id.tv_total_transaction)
    ItemTextView tvTotalTransaction;

    private BlockBean blockBean;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_detail);
        ButterKnife.bind(this);
        initView();
    }

    private void initView() {
        int blockNo = getIntent().getIntExtra(TransmitKey.ID, -1);
        if(blockNo > -1){
            blockBean = new BlockBean();
            blockBean.setPublicKey("131231231312312313123123131231231312312313123123");
            blockBean.setReward("100.00");
            blockBean.setTotal(100);
        }
        String title = getIntent().getStringExtra(TransmitKey.TITLE);
        if(StringUtil.isNotEmpty(title)){
            toolBar.setTitle(title);
        }
        if(blockBean != null){
            tvMiningIncome.setRightText(blockBean.getReward() + "TAU");
            tvMiner.setRightText(blockBean.getPublicKey());
            tvTotalTransaction.setRightText(blockBean.getTotal());
        }
    }

    @OnLongClick({R.id.tv_miner})
    boolean copyData(View view) {
        if(blockBean != null){
            CopyManager.copyText(blockBean.getPublicKey());
            ToastUtils.showShortToast(R.string.tx_address_copy);
        }
        return false;
    }

    @OnClick(R.id.tv_total_transaction)
    public void onViewClicked() {
        if(blockBean != null){
            Intent intent = new Intent();
            intent.putExtra(TransmitKey.ID, blockBean.getPublicKey());
            ActivityUtil.startActivity(this, BlockTxListActivity.class);
        }
    }
}