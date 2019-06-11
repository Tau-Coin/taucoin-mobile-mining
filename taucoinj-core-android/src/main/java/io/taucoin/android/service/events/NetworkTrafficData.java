package io.taucoin.android.service.events;

import android.os.Parcel;
import android.os.Parcelable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.TimeZone;

public class NetworkTrafficData extends EventData {

    private static final Logger logger = LoggerFactory.getLogger("TaucoinRemoteService");

    private static long sAdjust = 0;
    private static int sDay = -1;
    private static long sDayTrafficStatistics = 0;
    private static Calendar sCalendar = Calendar.getInstance(TimeZone.getDefault());
    static {
        sDay = sCalendar.get(Calendar.DAY_OF_MONTH);
    }

    public int trafficSize;

    public NetworkTrafficData(int trafficSize) {

        super();
        this.trafficSize = trafficSize;

        printTrafficStatistics(trafficSize);
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

    private static void printTrafficStatistics(int trafficSize) {
        int currentYear = sCalendar.get(Calendar.YEAR);
        int currentMonth = sCalendar.get(Calendar.MONTH) + 1;
        int currentDay = sCalendar.get(Calendar.DAY_OF_MONTH);

        if (currentDay == sDay) {
            sAdjust += 1;
            sDayTrafficStatistics += (long)trafficSize;
        } else {
            sAdjust = 0;
            sDay = currentDay;
            sDayTrafficStatistics = (long)trafficSize;
        }

        if (sAdjust % 10 == 0) {
            logger.info("Taucoin traffic statistics: {} B {}/{}/{}",
                    sDayTrafficStatistics, currentDay, currentMonth, currentYear);
        }
    }
}
