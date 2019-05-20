package io.taucoin.android.service.events;

import android.os.Parcel;
import android.os.Parcelable;

public class NetworkTrafficData extends EventData {

    public int trafficSize;

    public NetworkTrafficData(int trafficSize) {

        super();
        this.trafficSize = trafficSize;
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        super.writeToParcel(parcel, i);
        parcel.writeInt(trafficSize);
    }

    public static final Parcelable.Creator<NetworkTrafficData> CREATOR = new Parcelable.Creator<NetworkTrafficData>() {

        public NetworkTrafficData createFromParcel(Parcel in) {

            return new NetworkTrafficData(in);
        }

        public NetworkTrafficData[] newArray(int size) {

            return new NetworkTrafficData[size];
        }
    };

    private NetworkTrafficData(Parcel in) {

        super(in);
        trafficSize = in.readInt();
    }
}
