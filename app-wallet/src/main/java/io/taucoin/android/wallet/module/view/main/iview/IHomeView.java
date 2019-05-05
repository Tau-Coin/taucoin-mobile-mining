package io.taucoin.android.wallet.module.view.main.iview;

import java.util.List;

import io.taucoin.android.wallet.db.entity.MiningReward;

public interface IHomeView {
    void initView();
    void handleMiningView();
    void handleMiningRewardView(List<MiningReward> miningRewards);
}
