package io.taucoin.android.service.events;

import android.os.Parcel;
import android.os.Parcelable;

public class BlocksDownloadedData extends EventData {

    public long from;

    public long end;

    public BlocksDownloadedData(long from, long end) {

        super();
        this.from = from;
        this.end = end;
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        super.writeToParcel(parcel, i);
        parcel.writeLong(from);
        parcel.writeLong(end);
    }

    public static final Parcelable.Creator<BlocksDownloadedData> CREATOR = new Parcelable.Creator<BlocksDownloadedData>() {

        public BlocksDownloadedData createFromParcel(Parcel in) {

            return new BlocksDownloadedData(in);
        }

        public BlocksDownloadedData[] newArray(int size) {

            return new BlocksDownloadedData[size];
        }
    };

    private BlocksDownloadedData(Parcel in) {

        super(in);
        from = in.readLong();
        end = in.readLong();
    }
}
