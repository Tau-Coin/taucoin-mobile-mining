package io.taucoin.android.wallet.module.view.manage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.MyApplication;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.util.ResourcesUtil;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.foundation.util.DrawablesUtil;
import io.taucoin.foundation.util.StringUtil;

public class AddressAdapter extends BaseAdapter {

    private List<KeyValue> list = new ArrayList<>();
    private AddressBookActivity activity;

    AddressAdapter(AddressBookActivity activity){
        this.activity = activity;
    }

    void setListData(List<KeyValue> list) {
        this.list = list;
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
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        KeyValue keyValue = list.get(position);
        viewHolder.tvName.setText(UserUtil.parseNickName(keyValue));
        DrawablesUtil.setEndDrawable(viewHolder.tvName, R.mipmap.icon_edit, 14);
        viewHolder.tvAddress.setText(keyValue.getAddress());
        int color = R.color.color_black;

        if(UserUtil.isImportKey() && StringUtil.isSame(keyValue.getAddress(),
                MyApplication.getKeyValue().getAddress())){
            color = R.color.color_yellow;
        }
        viewHolder.tvAddress.setTextColor(ResourcesUtil.getColor(color));
        final boolean isSelf = color == R.color.color_yellow;
        viewHolder.ivDelete.setVisibility(isSelf ? View.INVISIBLE : View.VISIBLE);
        viewHolder.tvSwitchAddress.setVisibility(isSelf ? View.INVISIBLE : View.VISIBLE);
        viewHolder.ivDelete.setOnClickListener(v -> activity.deleteAddress(keyValue, isSelf));
        viewHolder.tvName.setOnClickListener(v -> activity.editName(keyValue, isSelf));
        viewHolder.tvSwitchAddress.setOnClickListener(v -> activity.switchAddress(keyValue));
        return convertView;
    }

    class ViewHolder {
        @BindView(R.id.tv_name)
        TextView tvName;
        @BindView(R.id.iv_delete)
        ImageView ivDelete;
        @BindView(R.id.tv_address)
        TextView tvAddress;
        @BindView(R.id.tv_switch_address)
        TextView tvSwitchAddress;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
