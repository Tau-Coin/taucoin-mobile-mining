package io.taucoin.android.service.events;


import android.os.Parcel;
import android.os.Parcelable;

import io.taucoin.core.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockForgedInternalEventData extends EventData {

    public long blockForgedInternal;

    public BlockForgedInternalEventData(long blockForgedInternal) {

        super();
        this.blockForgedInternal = blockForgedInternal;
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        super.writeToParcel(parcel, i);
        parcel.writeLong(blockForgedInternal);
    }

    public static final Parcelable.Creator<BlockForgedInternalEventData> CREATOR
            = new Parcelable.Creator<BlockForgedInternalEventData>() {

        public BlockForgedInternalEventData createFromParcel(Parcel in) {

            return new BlockForgedInternalEventData(in);
        }

        public BlockForgedInternalEventData[] newArray(int size) {

            return new BlockForgedInternalEventData[size];
        }
    };

    protected BlockForgedInternalEventData(Parcel in) {

        super(in);
        this.blockForgedInternal = in.readLong();
    }
}

