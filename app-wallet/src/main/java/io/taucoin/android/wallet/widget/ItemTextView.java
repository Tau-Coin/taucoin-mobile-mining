/**
 * Copyright 2018 Taucoin Core Developers.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mofei.tau.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.foundation.util.StringUtil;

public class ItemTextView extends RelativeLayout {
    private String leftText;
    private int leftTextColor;
    private String rightText;
    private int rightTextColor;
    private int rightImage;
    private int imageRotation;
    private ViewHolder viewHolder;

    public ItemTextView(Context context) {
        this(context, null);
    }

    public ItemTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ItemTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData(attrs);

    }

    private void initData(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ItemTextView);
        this.rightImage = a.getResourceId(R.styleable.ItemTextView_itemRightImage, -1);
        this.imageRotation = a.getInt(R.styleable.ItemTextView_itemImageRotation, 0);
        this.leftText = a.getString(R.styleable.ItemTextView_itemLeftText);
        this.leftTextColor = a.getColor(R.styleable.ItemTextView_itemLeftTextColor, getResources().getColor(R.color.color_grey_dark));
        this.rightText = a.getString(R.styleable.ItemTextView_itemRightText);
        this.rightTextColor = a.getColor(R.styleable.ItemTextView_itemRightTextColor, getResources().getColor(R.color.color_yellow));
        a.recycle();
        loadView();
    }

    private void loadView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.view_item_text, this, true);
        viewHolder = new ViewHolder(view);
        if (rightImage != -1) {
            viewHolder.ivRight.setImageResource(rightImage);
            viewHolder.ivRight.setRotation(imageRotation);
        }
        viewHolder.ivRight.setVisibility(rightImage != -1 ? View.VISIBLE : View.GONE);
        if (StringUtil.isNotEmpty(rightText)) {
            viewHolder.tvRight.setText(rightText);
        }
        viewHolder.tvRight.setTextColor(rightTextColor);
        viewHolder.tvRight.setVisibility(StringUtil.isNotEmpty(rightText) ? View.VISIBLE : View.GONE);
        if (StringUtil.isNotEmpty(leftText)) {
            viewHolder.tvLeft.setText(leftText);
        }
        viewHolder.tvLeft.setTextColor(leftTextColor);
        viewHolder.tvLeft.setVisibility(StringUtil.isNotEmpty(leftText) ? View.VISIBLE : View.GONE);
    }

    public void setRightText(long text) {
        setRightText("" + text);
    }

    public void setRightText(int text) {
        setRightText("" + text);
    }

    public void setRightText(String rightText) {
        this.rightText = rightText;
        viewHolder.tvRight.setText(rightText);
        viewHolder.tvRight.setVisibility(View.VISIBLE);
    }

    public void setLeftText(int text) {
        setLeftText("" + text);
    }

    public void setEnable(boolean isEnable) {
        this.setEnabled(isEnable);
        int color = isEnable ? R.color.color_grey_dark : R.color.color_grey;
        viewHolder.tvLeft.setTextColor(getResources().getColor(color));
    }

    public void setLeftText(String leftText) {
        this.leftText = leftText;
        viewHolder.tvLeft.setText(leftText);
        viewHolder.tvLeft.setVisibility(View.VISIBLE);
    }

    public void setLeftTextColor(int leftTextColor) {
        this.leftTextColor = getResources().getColor(leftTextColor);
        viewHolder.tvLeft.setTextColor(this.leftTextColor);
    }

    class ViewHolder {
        @BindView(R.id.iv_right)
        ImageView ivRight;
        @BindView(R.id.tv_right)
        TextView tvRight;
        @BindView(R.id.tv_left)
        TextView tvLeft;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
