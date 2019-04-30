package io.taucoin.android.service.events;
import android.os.Parcel;
import android.os.Parcelable;
import io.taucoin.forge.ForgeStatus;

public class BlockForgeExceptionStopEvent extends EventData{
    private int code;
    private String msg;

    public  BlockForgeExceptionStopEvent(ForgeStatus status){
        super();
        this.code = status.getCode();
        this.msg = status.getMsg();
    }
    @Override
    public int describeContents() {

        return 0;
    }
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(code);
        parcel.writeString(msg);
    }

    public static final Parcelable.Creator<BlockForgeExceptionStopEvent> CREATOR
            = new Parcelable.Creator<BlockForgeExceptionStopEvent>() {

        public BlockForgeExceptionStopEvent createFromParcel(Parcel in) {

            return new BlockForgeExceptionStopEvent(in);
        }

        public BlockForgeExceptionStopEvent[] newArray(int size) {

            return new BlockForgeExceptionStopEvent[size];
        }
    };

    private BlockForgeExceptionStopEvent(Parcel in) {
        super(in);
        this.code = in.readInt();
        this.msg = in.readString();
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
