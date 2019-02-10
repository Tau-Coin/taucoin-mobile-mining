package io.taucoin.android.service.events;


import android.os.Parcel;
import android.os.Parcelable;

import io.taucoin.core.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockEventData extends EventData {

    public Block block;

    public BlockEventData(Block block) {

        super();
        this.block = block;
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        super.writeToParcel(parcel, i);
        parcel.writeParcelable(new io.taucoin.android.interop.Block(block), i);
    }

    public static final Parcelable.Creator<BlockEventData> CREATOR = new Parcelable.Creator<BlockEventData>() {

        public BlockEventData createFromParcel(Parcel in) {

            return new BlockEventData(in);
        }

        public BlockEventData[] newArray(int size) {

            return new BlockEventData[size];
        }
    };

    protected BlockEventData(Parcel in) {

        super(in);
        block = in.readParcelable(io.taucoin.android.interop.Block.class.getClassLoader());
    }
}
