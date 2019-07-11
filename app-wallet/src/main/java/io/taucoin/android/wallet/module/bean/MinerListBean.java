package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MinerListBean extends BaseBean{

    @SerializedName(value = "minerhistory")
    private List<MinerBean> minerHistory;

    public List<MinerBean> getMinerHistory() {
        return minerHistory;
    }

    public void setMinerHistory(List<MinerBean> minerHistory) {
        this.minerHistory = minerHistory;
    }

    public class MinerBean{
        @SerializedName(value = "blockheight")
        private long blockHeight;
        @SerializedName(value = "blockhash")
        private String blockHash;
        @SerializedName(value = "mincome")
        private String income;

        public long getBlockHeight() {
            return blockHeight;
        }

        public void setBlockHeight(long blockHeight) {
            this.blockHeight = blockHeight;
        }

        public String getBlockHash() {
            return blockHash;
        }

        public void setBlockHash(String blockHash) {
            this.blockHash = blockHash;
        }

        public String getIncome() {
            return income;
        }

        public void setIncome(String income) {
            this.income = income;
        }
    }
}
