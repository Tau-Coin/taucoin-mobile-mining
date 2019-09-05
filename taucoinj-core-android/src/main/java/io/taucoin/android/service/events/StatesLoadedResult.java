package io.taucoin.android.service.events;

import android.os.Parcel;
import android.os.Parcelable;

public class StatesLoadedResult extends EventData {

    public boolean success;

    public long tagHeight;

    public StatesLoadedResult(boolean success, long tagHeight) {

        super();
        this.success = success;
        this.tagHeight = tagHeight;
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        super.writeToParcel(parcel, i);
        parcel.writeLong(success ? 1L : 0L);
        parcel.writeLong(tagHeight);
    }

    public static final Parcelable.Creator<StatesLoadedResult> CREATOR = new Parcelable.Creator<StatesLoadedResult>() {

        public StatesLoadedResult createFromParcel(Parcel in) {

            return new StatesLoadedResult(in);
        }

        public StatesLoadedResult[] newArray(int size) {

            return new StatesLoadedResult[size];
        }
    };

    private StatesLoadedResult(Parcel in) {

        super(in);
        success = in.readLong() == 0 ? false : true;
        tagHeight = in.readLong();
    }
}
