package io.taucoin.android.wallet.module.view.mining;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.mofei.tau.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import io.taucoin.android.service.events.BlockEventData;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.module.bean.BlockBean;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.CopyManager;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.widget.ItemTextView;
import io.taucoin.android.wallet.widget.ToolbarView;
import io.taucoin.core.Block;
import io.taucoin.core.Transaction;

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

        BlockEventData blockEvent = getIntent().getParcelableExtra(TransmitKey.BEAN);
        if(blockEvent != null){
            Block block = blockEvent.block;
            tvMiningIncome.setRightText(block + "TAU");
            tvMiner.setRightText(blockBean.getPublicKey());
            List<Transaction> txList = block.getTransactionsList();
            if(txList != null){
                int total = txList.size();
                tvTotalTransaction.setRightText(total);
            }
            String title = getText(R.string.block_no).toString();
            title = String.format(title, block.getNumber());
            toolBar.setTitle(title);
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