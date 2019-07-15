package io.taucoin.android.wallet.module.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ParticipantListBean extends BaseBean{

    @SerializedName(value = "pphistory")
    private List<ParticipantBean> partHistory;

    public List<ParticipantBean> getPartHistory() {
        return partHistory;
    }

    public void setPartHistory(List<ParticipantBean> partHistory) {
        this.partHistory = partHistory;
    }

    public class ParticipantBean{
        @SerializedName(value = "pprole")
        private int role;
        @SerializedName(value = "txhash")
        private String txHash;
        @SerializedName(value = "ppincome")
        private String income;

        public int getRole() {
            return role;
        }

        public void setRole(int role) {
            this.role = role;
        }

        public String getTxHash() {
            return txHash;
        }

        public void setTxHash(String txHash) {
            this.txHash = txHash;
        }

        public String getIncome() {
            return income;
        }

        public void setIncome(String income) {
            this.income = income;
        }
    }
}