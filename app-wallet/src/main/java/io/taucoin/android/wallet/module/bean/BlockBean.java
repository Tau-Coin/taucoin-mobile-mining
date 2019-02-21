package io.taucoin.android.wallet.module.bean;

import android.os.Parcel;
import android.os.Parcelable;

import io.taucoin.android.wallet.db.entity.MiningInfo;

public class BlockBean extends MiningInfo implements Parcelable {


    public BlockBean() {
    }
    protected BlockBean(Parcel in) {
    }

    public static final Creator<BlockBean> CREATOR = new Creator<BlockBean>() {
        @Override
        public BlockBean createFromParcel(Parcel in) {
            return new BlockBean(in);
        }

        @Override
        public BlockBean[] newArray(int size) {
            return new BlockBean[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}