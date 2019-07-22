package io.taucoin.android.wallet.module.view.manage;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.github.naturs.logger.Logger;

import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.module.bean.RewardInfoBean;
import io.taucoin.android.wallet.module.service.NotifyManager;
import io.taucoin.android.wallet.module.view.SplashActivity;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.widget.CongratulationDialog;
import io.taucoin.foundation.util.ActivityManager;
import io.taucoin.foundation.util.AppUtil;

/**
 * Description: reward congratulation activity
 * Author: yang
 * Date: 2019/1/5 09:08
 */

public class CongratulationActivity extends BaseActivity{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(null != savedInstanceState){
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ActivityUtil.startActivity(intent, this, SplashActivity.class);

        }
        Logger.d("Upgrade show dialog start");
        if(ActivityManager.getInstance().getActivitySize() > 0 &&
                AppUtil.isOnForeground(this)){
            RewardInfoBean rewardInfo = getIntent().getParcelableExtra(TransmitKey.BEAN);
            showCongratulationDialog(rewardInfo);
        }else{
            Logger.d("Upgrade stop on onCreate");
            finish();
        }
    }

    private void showCongratulationDialog(RewardInfoBean rewardInfo) {
        if(rewardInfo == null){
            this.finish();
            return;
        }
        String rewardTitle = getText(R.string.main_congratulation).toString();
        String rewardMsg = getText(R.string.main_congratulation_msg).toString();
        String blocksNo = FmtMicrometer.fmtPower(rewardInfo.getBlockNo());
        String reward = FmtMicrometer.fmtDecimal(rewardInfo.getReward());
        String hours = FmtMicrometer.fmtPower(rewardInfo.getTime());
        rewardMsg = String.format(rewardMsg, blocksNo, reward, hours);

        String notifyMsg = rewardTitle + " " + rewardMsg;
        NotifyManager.getInstance().sendCongratulationNotify(notifyMsg);
        new CongratulationDialog.Builder(this)
            .setCanceledOnTouchOutside(false)
            .setMsg(rewardMsg)
            .setPositiveButton(R.string.common_continue, (dialog, which) ->{
                dialog.cancel();
                this.finish();
            }).create().show();
    }
}