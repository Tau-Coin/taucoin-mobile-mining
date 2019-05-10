package io.taucoin.android.wallet.module.view.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.db.entity.MiningReward;
import io.taucoin.android.wallet.util.FmtMicrometer;

public class MiningRewardAdapter extends BaseAdapter {

    private List<MiningReward> list = new ArrayList<>();

    void setListData(List<MiningReward> data, boolean isAdd) {
        if (!isAdd) {
           list.clear();
        }
        if (data != null) {
            list.addAll(data);
        }
        notifyDataSetChanged();
    }

    String getTxHash(int pos) {
        if(pos < 0 || pos > getCount()){
            return "";
        }
        return list.get(pos).getTxHash();
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
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reward, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        MiningReward bean = list.get(position);
        viewHolder.tvHash.setText(bean.getTxHash());
        long fee = bean.getMinerFee() + bean.getPartFee();
        String feeStr = String.valueOf(fee);
        viewHolder.tvFee.setText(FmtMicrometer.fmtFormat(feeStr));
        int reStatus = R.string.home_mining_miner_participant;
        if(bean.getMinerFee() == 0){
            reStatus = R.string.home_mining_participant;
        }else if(bean.getPartFee() == 0){
            reStatus = R.string.home_mining_miner;
        }
        viewHolder.tvStatus.setText(reStatus);
        return convertView;
    }

    class ViewHolder {
        @BindView(R.id.tv_hash)
        TextView tvHash;
        @BindView(R.id.tv_fee)
        TextView tvFee;
        @BindView(R.id.tv_status)
        TextView tvStatus;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
