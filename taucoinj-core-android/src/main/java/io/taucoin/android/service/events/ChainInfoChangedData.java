package io.taucoin.android.service.events;

import android.os.Parcel;
import android.os.Parcelable;

public class ChainInfoChangedData extends EventData {

    public long height;

    public ChainInfoChangedData(long height) {

        super();
        this.height = height;
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        super.writeToParcel(parcel, i);
        parcel.writeLong(height);
    }

    public static final Parcelable.Creator<ChainInfoChangedData> CREATOR = new Parcelable.Creator<ChainInfoChangedData>() {

        public ChainInfoChangedData createFromParcel(Parcel in) {

            return new ChainInfoChangedData(in);
        }

        public ChainInfoChangedData[] newArray(int size) {

            return new ChainInfoChangedData[size];
        }
    };

    private ChainInfoChangedData(Parcel in) {

        super(in);
        height = in.readLong();
    }
}
