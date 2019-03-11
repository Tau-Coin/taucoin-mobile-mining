package io.taucoin.android.service.events;
import android.os.Parcel;
import android.os.Parcelable;

public class BlockForgeExceptionStopEvent extends EventData{
    private String reason;

    public  BlockForgeExceptionStopEvent(String reason){
        super();
        this.reason = reason;
    }
    @Override
    public int describeContents() {

        return 0;
    }
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeString(reason);
    }

    public BlockForgeExceptionStopEvent(Parcel in) {
        super(in);
        this.reason = in.readString();
    }
}
