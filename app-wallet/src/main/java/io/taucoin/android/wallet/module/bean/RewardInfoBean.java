package io.taucoin.android.wallet.module.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class RewardInfoBean extends BaseBean implements Parcelable {

    @SerializedName(value = "mbno")
    private String blockNo;
    @SerializedName(value = "mreward")
    private String reward;
    @SerializedName(value = "mtime")
    private String time;

    protected RewardInfoBean(Parcel in) {
        blockNo = in.readString();
        reward = in.readString();
        time = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(blockNo);
        dest.writeString(reward);
        dest.writeString(time);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RewardInfoBean> CREATOR = new Creator<RewardInfoBean>() {
        @Override
        public RewardInfoBean createFromParcel(Parcel in) {
            return new RewardInfoBean(in);
        }

        @Override
        public RewardInfoBean[] newArray(int size) {
            return new RewardInfoBean[size];
        }
    };

    public String getBlockNo() {
        return blockNo;
    }

    public void setBlockNo(String blockNo) {
        this.blockNo = blockNo;
    }

    public String getReward() {
        return reward;
    }

    public void setReward(String reward) {
        this.reward = reward;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
