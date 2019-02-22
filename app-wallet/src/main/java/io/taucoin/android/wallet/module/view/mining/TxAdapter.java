package io.taucoin.android.wallet.module.view.mining;

import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mofei.tau.R;

import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnLongClick;
import io.taucoin.android.wallet.util.CopyManager;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.SpanUtils;
import io.taucoin.android.wallet.util.ToastUtils;
import io.taucoin.core.Transaction;
import io.taucoin.foundation.util.StringUtil;

public class TxAdapter extends BaseAdapter {


    private List<Transaction> list = new ArrayList<>();


    void setListData(List<Transaction> list) {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tx, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        Transaction bean = list.get(position);
        String title = parent.getContext().getText(R.string.block_transaction_id).toString();
        String txId = Hex.toHexString(bean.getHash());
        SpannableStringBuilder titleSequence = new SpanUtils()
                .append(title)
                .setForegroundColor(ContextCompat.getColor(parent.getContext(), R.color.color_grey_dark))
                .append(txId)
                .create();
        viewHolder.tvTitle.setTag(txId);
        viewHolder.tvTitle.setText(titleSequence);

        String subheading = parent.getContext().getText(R.string.bloc_transaction_fee).toString();
        BigInteger fee = new BigInteger(bean.getFee());
        String feeStr = FmtMicrometer.fmtFormat(fee.toString());
        subheading = String.format(subheading, feeStr);
        viewHolder.tvSubheading.setText(subheading);
        return convertView;
    }

    class ViewHolder {
        @BindView(R.id.tv_title)
        TextView tvTitle;
        @BindView(R.id.tv_subheading)
        TextView tvSubheading;

        @OnLongClick({R.id.tv_title})
        boolean copyData(View view) {
            CopyManager.copyText( StringUtil.getTag(view));
            ToastUtils.showShortToast(R.string.tx_id_copy);
            return false;
        }

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}