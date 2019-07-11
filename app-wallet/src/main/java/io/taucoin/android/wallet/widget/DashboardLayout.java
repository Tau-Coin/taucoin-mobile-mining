/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.ResourcesUtil;

public class DashboardLayout extends RelativeLayout {
    private ViewHolder viewHolder;
    private long maxValue;
    private long value;
    private boolean isError;

    public DashboardLayout(Context context) {
        this(context, null);
    }

    public DashboardLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DashboardLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        loadView();
    }

    private void loadView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_layout_dashboard, this, true);
        viewHolder = new ViewHolder(view);
        setValue(0);
        setPercentage(0);
        viewHolder.dashboardView.setValueUpdateListener((value, maxValue) -> {
            setValue((int)value);
            double percentage = 0;
            if(maxValue != 0){
                percentage = value * 100 / maxValue;
            }
            setPercentage(percentage);
        });
    }

    private void setValue(long value){
        if(viewHolder != null){
            String valueStr = FmtMicrometer.fmtPower(value);
            viewHolder.tvValue.setText(valueStr);
        }
    }

    private void setPercentage(double percentage){
        if(viewHolder != null){
            String unit = ResourcesUtil.getText(R.string.common_percentage);
            String percentageStr = (int)percentage + unit;
            viewHolder.tvPercentage.setText(percentageStr);
        }
    }

    public void changeValue(long value, long maxValue) {
        if(this.value != value || this.maxValue != maxValue){
            this.value = value;
            this.maxValue = maxValue;
            viewHolder.dashboardView.setMaxValue(maxValue);
            viewHolder.dashboardView.setValue(value);
            if(value > 0 && !isError){
                viewHolder.ivMiningPower.setOn();
            }else{
                viewHolder.ivMiningPower.setOff();
            }
        }
    }

    public void setError(boolean isError) {
        this.isError = isError;
    }

    class ViewHolder {
        @BindView(R.id.dashboard_view)
        DashboardView dashboardView;
        @BindView(R.id.tv_value)
        TextView tvValue;
        @BindView(R.id.tv_percentage)
        TextView tvPercentage;
        @BindView(R.id.iv_mining_power)
        ProgressView ivMiningPower;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    interface ValueUpdateListener{
        void update(float value, float maxValue);
    }
}