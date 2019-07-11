package io.taucoin.android.wallet.module.view.main;//package io.taucoin.android.wallet.module.view.mining;

import android.text.SpannableStringBuilder;
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
import io.taucoin.android.wallet.module.bean.MinerListBean;
import io.taucoin.android.wallet.module.bean.ParticipantListBean;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.ResourcesUtil;
import io.taucoin.android.wallet.util.SpanUtils;
import io.taucoin.foundation.util.StringUtil;

public class RewardAdapter extends BaseAdapter {

    private List<Object> list = new ArrayList<>();

    void setMinerListData(List<MinerListBean.MinerBean> list) {
        this.list.clear();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    void setPartListData(List<ParticipantListBean.ParticipantBean> list) {
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
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_reward, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        String left = "";
        String middle = "";
        String incomeStr = "";
        if(position < list.size()){
            Object bean = list.get(position);
            if(bean instanceof MinerListBean.MinerBean){
                MinerListBean.MinerBean minerBean = (MinerListBean.MinerBean) bean;
                String lastBlockNoStr = ResourcesUtil.getText(R.string.home_no_point);
                long nextBlockNo = minerBean.getBlockHeight();
                left = String.format(lastBlockNoStr, FmtMicrometer.fmtPower(nextBlockNo));
                middle = minerBean.getBlockHash();
                incomeStr = minerBean.getIncome();
            }else if(bean instanceof ParticipantListBean.ParticipantBean){
                ParticipantListBean.ParticipantBean partBean = (ParticipantListBean.ParticipantBean) bean;
                if(partBean.getRole() == 1){
                    left = ResourcesUtil.getText(R.string.home_role_miner);
                }else{
                    left = ResourcesUtil.getText(R.string.home_role_participant);
                }
                middle = partBean.getTxHash();
                incomeStr = partBean.getIncome();
            }
        }
        String right = FmtMicrometer.fmtDecimal(incomeStr);
        SpannableStringBuilder spannable = new SpanUtils()
            .append(right)
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_blue))
            .append(" ")
            .append(ResourcesUtil.getText(R.string.common_balance_unit))
            .setForegroundColor(ResourcesUtil.getColor(R.color.color_home_grey_dark))
            .create();

        viewHolder.tvLeft.setText(left);
        viewHolder.tvMiddle.setText(middle);
        viewHolder.tvRight.setText(spannable);
        return convertView;
    }
    class ViewHolder {
        @BindView(R.id.tv_left)
        TextView tvLeft;
        @BindView(R.id.tv_middle)
        TextView tvMiddle;
        @BindView(R.id.tv_right)
        TextView tvRight;

//        @OnLongClick({R.id.tv_middle})
//        boolean copyData(View view) {
//            CopyManager.copyText(StringUtil.getTag(view));
//            ToastUtils.showShortToast(R.string.tx_id_copy);
//            return false;
//        }

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}