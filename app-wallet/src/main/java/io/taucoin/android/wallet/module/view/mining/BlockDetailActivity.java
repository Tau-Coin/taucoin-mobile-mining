package io.taucoin.android.wallet.module.view.mining;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.spongycastle.util.encoders.Hex;

import io.taucoin.android.wallet.R;

import io.taucoin.android.wallet.util.ResourcesUtil;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import io.taucoin.android.service.events.BlockEventData;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.CopyManager;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.widget.ItemTextView;
import io.taucoin.android.wallet.widget.ToolbarView;
import io.taucoin.core.Block;
import io.taucoin.core.Transaction;
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

    private BlockEventData blockEvent;
    private String publicKey;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_detail);
        ButterKnife.bind(this);
        initView();
    }

    private void initView() {
        blockEvent = getIntent().getParcelableExtra(TransmitKey.BEAN);
        if(blockEvent != null && blockEvent.block != null){
            Block blockBean = blockEvent.block;

            publicKey = Hex.toHexString(blockBean.getGeneratorPublicKey());

            tvMiner.setRightText(publicKey);
            List<Transaction> txList = blockBean.getTransactionsList();
            if(txList != null){
                tvTotalTransaction.setRightText(txList.size());
                String totalFee = MiningUtil.parseBlockTxFee(txList);
                String income = FmtMicrometer.fmtFormat(totalFee);
                income += ResourcesUtil.getText(R.string.common_balance_unit);
                tvMiningIncome.setRightText(income);
            }
            String title = getText(R.string.block_no).toString();
            title = String.format(title, blockBean.getNumber());
            toolBar.setTitle(title);
        }
    }

    @OnLongClick({R.id.tv_miner})
    boolean copyData(View view) {
        if(StringUtil.isNotEmpty(publicKey)){
            CopyManager.copyText(publicKey);
            ToastUtils.showShortToast(R.string.tx_address_copy);
        }
        return false;
    }

    @OnClick(R.id.tv_total_transaction)
    public void onViewClicked() {
        if(blockEvent != null){
            Intent intent = new Intent();
            intent.putExtra(TransmitKey.BEAN, blockEvent);
            ActivityUtil.startActivity(intent,this, BlockTxListActivity.class);
        }
    }
}