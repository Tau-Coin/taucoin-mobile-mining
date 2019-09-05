package io.taucoin.android.wallet.module.view.main;

import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import io.taucoin.android.wallet.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnLongClick;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.util.CopyManager;
import io.taucoin.android.wallet.util.DateUtil;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.ResourcesUtil;
import io.taucoin.android.wallet.util.SharedPreferencesHelper;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.foundation.util.StringUtil;

public class HistoryExpandableListAdapter extends BaseExpandableListAdapter {

    private List<TransactionHistory> historyList = new ArrayList<>();
    private String address;

    HistoryExpandableListAdapter() {
    }

    List<TransactionHistory> getData() {
        return historyList;
    }

    void setHistoryList(List<TransactionHistory> historyList, boolean isAdd) {
        address = SharedPreferencesHelper.getInstance().getString(TransmitKey.ADDRESS, "");
        if (!isAdd) {
            this.historyList.clear();
        }
        this.historyList.addAll(historyList);
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return historyList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return historyList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return historyList.get(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupViewHolder groupViewHolder;
        TransactionHistory tx = historyList.get(groupPosition);
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_group, parent, false);
            groupViewHolder = new GroupViewHolder(convertView);
            convertView.setTag(groupViewHolder);
        } else {
            groupViewHolder = (GroupViewHolder) convertView.getTag();
        }

        boolean isReceiver = isReceiver(tx);
        String total = FmtMicrometer.fmtFormatAdd(tx.getAmount(), tx.getFee());
        String amount = FmtMicrometer.fmtFormat(tx.getAmount());
        total = FmtMicrometer.fmtFormat(total);
        amount = isReceiver ? "+" + amount : "-" + total;
        groupViewHolder.tvAmount.setText(amount);

        String time = DateUtil.formatTime(tx.getCreateTime(), DateUtil.pattern10);
        groupViewHolder.tvTime.setText(time);
        // The user is the sender
        int color = R.color.color_red;
        if(StringUtil.isNotEmpty(tx.getResult())){
            switch (tx.getResult()){
                case TransmitKey.TxResult.BROADCASTING:
                case TransmitKey.TxResult.CONFIRMING:
                    color = R.color.color_blue;
                    break;
                case TransmitKey.TxResult.SUCCESSFUL:
                    color = R.color.color_black;
                    break;
            }
        }
        int textColor = ContextCompat.getColor(parent.getContext(), color);
        groupViewHolder.tvAmount.setTextColor(textColor);
        groupViewHolder.tvTime.setTextColor(textColor);
        return convertView;
    }

    private boolean isReceiver(TransactionHistory tx) {
        return StringUtil.isNotSame(tx.getFromAddress(), address);
    }

    @Override
    public View getChildView(final int groupPosition, int childPosition, boolean isLastChild, View
            convertView, ViewGroup parent) {
        ChildViewHolder childViewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_child, parent, false);
            childViewHolder = new ChildViewHolder(convertView);
            convertView.setTag(childViewHolder);
        } else {
            childViewHolder = (ChildViewHolder) convertView.getTag();
        }

        TransactionHistory tx = historyList.get(groupPosition);
        childViewHolder.tvTransactionId.setText(tx.getTxId());
        String fee = FmtMicrometer.fmtFeeValue(tx.getFee());
        fee += ResourcesUtil.getText(R.string.common_balance_unit);
        childViewHolder.tvTxFee.setText(fee);

        boolean isReceiver = isReceiver(tx);

        childViewHolder.tvAddressTitle.setText(isReceiver ? R.string.tx_from_address : R.string.tx_to_address);
        childViewHolder.tvReceivedAddress.setText(isReceiver ? tx.getFromAddress() : tx.getToAddress());
        childViewHolder.tvFeeTitle.setVisibility(isReceiver ? View.GONE : View.VISIBLE);
        childViewHolder.tvTxFee.setVisibility(isReceiver ? View.GONE : View.VISIBLE);

        childViewHolder.tvTransactionExpiryTitle.setVisibility(isReceiver ? View.GONE : View.VISIBLE);
        childViewHolder.tvTransactionExpiry.setVisibility(isReceiver ? View.GONE : View.VISIBLE);
        String transactionExpiry = UserUtil.getTransExpiryTime(tx.getTransExpiry());
        transactionExpiry += "min";
        childViewHolder.tvTransactionExpiry.setText(transactionExpiry);

        childViewHolder.tvMemo.setText(tx.getMemo());
        boolean isHaveMemo = StringUtil.isNotEmpty(tx.getMemo());
        childViewHolder.tvMemo.setVisibility(!isHaveMemo  || isReceiver? View.GONE : View.VISIBLE);
        childViewHolder.tvMemoTitle.setVisibility(!isHaveMemo || isReceiver ? View.GONE : View.VISIBLE);

        // The user is the sender
        String statusMsg = tx.getMessage();
        int color = R.color.color_red;
        if(StringUtil.isNotEmpty(tx.getResult())){
            switch (tx.getResult()){
                case TransmitKey.TxResult.BROADCASTING:
                case TransmitKey.TxResult.CONFIRMING:
                    statusMsg = ResourcesUtil.getText(R.string.send_tx_status_pending);
                    color = R.color.color_blue;
                    break;
                case TransmitKey.TxResult.SUCCESSFUL:
                    statusMsg = ResourcesUtil.getText(R.string.send_tx_status_success);
                    color = R.color.color_black;
                    break;
            }
        }
        int textColor = ContextCompat.getColor(parent.getContext(), color);
        childViewHolder.tvStatusMsg.setTextColor(textColor);
        childViewHolder.tvStatusMsg.setText(statusMsg);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    class GroupViewHolder {
        @BindView(R.id.tv_amount)
        TextView tvAmount;
        @BindView(R.id.tv_time)
        TextView tvTime;

        GroupViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    class ChildViewHolder {
        @BindView(R.id.tv_received_address)
        TextView tvReceivedAddress;
        @BindView(R.id.tv_transaction_id)
        TextView tvTransactionId;
        @BindView(R.id.tv_tx_fee)
        TextView tvTxFee;
        @BindView(R.id.tv_status_msg)
        TextView tvStatusMsg;
        @BindView(R.id.tv_address_title)
        TextView tvAddressTitle;
        @BindView(R.id.tv_fee_title)
        TextView tvFeeTitle;
        @BindView(R.id.tv_memo_title)
        TextView tvMemoTitle;
        @BindView(R.id.tv_memo)
        TextView tvMemo;
        @BindView(R.id.tv_transaction_expiry)
        TextView tvTransactionExpiry;
        @BindView(R.id.tv_transaction_expiry_title)
        TextView tvTransactionExpiryTitle;

        ChildViewHolder(View view) {
            ButterKnife.bind(this, view);
        }

        @OnLongClick(R.id.tv_transaction_id)
        boolean copyTransactionId() {
            CopyManager.copyText(StringUtil.getText(tvTransactionId));
            ToastUtils.showShortToast(R.string.tx_id_copy);
            return false;
        }

        @OnLongClick(R.id.tv_received_address)
        boolean copyAddress() {
            CopyManager.copyText(StringUtil.getText(tvReceivedAddress));
            ToastUtils.showShortToast(R.string.tx_address_copy);
            return false;
        }
    }
}