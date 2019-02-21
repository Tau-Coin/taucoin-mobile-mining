package io.taucoin.android.wallet.module.view.mining;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.mofei.tau.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.db.entity.MiningInfo;
import io.taucoin.android.wallet.widget.ItemTextView;
import io.taucoin.foundation.util.StringUtil;

public class BlockAdapter extends BaseAdapter {

    private int listSize = 0;
    private int dataSize = 0;
    private boolean isMe = false;
    private List<MiningInfo> list = new ArrayList<>();

    void setListData(List<MiningInfo> list) {
        this.list = list;
    }

    void setListSize(int listSize, int dataSize, boolean isMe) {
        this.listSize = listSize;
        this.dataSize = dataSize;
        this.isMe = isMe;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return listSize;
    }

    @Override
    public Object getItem(int position) {
        return dataSize - position - 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_block, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        String title = parent.getContext().getText(R.string.block_no).toString();
        int num = dataSize - position - 1;
        if(isMe){
            MiningInfo bean = list.get(position);
            num = StringUtil.getIntString(bean.getBlockNo());
        }
        title = String.format(title, num);
        viewHolder.tvHelpTitle.setLeftText(title);
        convertView.setTag(R.id.block_list_tag, num);
        viewHolder.tvHelpTitle.setLeftTextColor(isMe || isMe(num) ? R.color.color_yellow : R.color.color_grey_dark);
        return convertView;
    }

    private boolean isMe(int no) {
        boolean isMe = false;
        for (int i = 0; i < list.size() ; i++) {
            String blockNoStr = list.get(i).getBlockNo();
            int blockNo = StringUtil.getIntString(blockNoStr);
            if(no > blockNo){
                break;
            }else if(no == blockNo){
                isMe = true;
                break;
            }
        }
        return isMe;
    }

    class ViewHolder {
        @BindView(R.id.tv_help_title)
        ItemTextView tvHelpTitle;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
