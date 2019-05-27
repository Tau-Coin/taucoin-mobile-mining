package io.taucoin.android.wallet.module.bean;

import java.util.Comparator;

import io.taucoin.android.wallet.db.entity.MiningBlock;
import io.taucoin.foundation.util.StringUtil;

public class BlockNoComparator implements Comparator<MiningBlock> {
    @Override
    public int compare(MiningBlock h1, MiningBlock h2) {
        try {
            int blockNo1 = StringUtil.getIntString(h1.getBlockNo());
            int blockNo2 = StringUtil.getIntString(h2.getBlockNo());
            if(blockNo1 < blockNo2) {
                return 1;
            } else if (blockNo1 == blockNo2) {
                return 0;
            } else {
                return -1;
            }
        }catch (Exception ignore){}
        return 0;
    }
}