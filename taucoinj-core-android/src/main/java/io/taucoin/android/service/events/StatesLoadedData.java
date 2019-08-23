package io.taucoin.android.service.events;

import android.os.Parcel;
import android.os.Parcelable;

public class StatesLoadedData extends EventData {

    public long hasLoaded;

    public long total;

    public StatesLoadedData(long hasLoaded, long total) {

        super();
        this.hasLoaded = hasLoaded;
        this.total = total;
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        super.writeToParcel(parcel, i);
        parcel.writeLong(hasLoaded);
        parcel.writeLong(total);
    }

    public static final Parcelable.Creator<StatesLoadedData> CREATOR = new Parcelable.Creator<StatesLoadedData>() {

        public StatesLoadedData createFromParcel(Parcel in) {

            return new StatesLoadedData(in);
        }

        public StatesLoadedData[] newArray(int size) {

            return new StatesLoadedData[size];
        }
    };

    private StatesLoadedData(Parcel in) {

        super(in);
        hasLoaded = in.readLong();
        total = in.readLong();
    }
}
