package io.taucoin.android.service.events;


import android.os.Parcel;
import android.os.Parcelable;

import io.taucoin.forge.NextBlockForgedDetail;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class NextBlockForgedPOTDetail extends EventData {

    public BigInteger baseTarget;
    public BigInteger generationSignature;
    public BigInteger cumulativeDifficulty;
    public BigInteger forgingPower;
    public BigInteger hitValue;

    public NextBlockForgedPOTDetail(NextBlockForgedDetail detail) {

        super();
        this.baseTarget = detail.getBaseTarget();
        this.generationSignature = detail.getGenerationSignature();
        this.cumulativeDifficulty = detail.getCumulativeDifficulty();
        this.forgingPower = detail.getForgingPower();
        this.hitValue = detail.getHitValue();
    }

    @Override
    public int describeContents() {

        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        super.writeToParcel(parcel, i);
        parcel.writeString(baseTarget.toString(16));
        parcel.writeString(generationSignature.toString(16));
        parcel.writeString(cumulativeDifficulty.toString(16));
        parcel.writeString(forgingPower.toString(16));
        parcel.writeString(hitValue.toString(16));
    }

    public static final Parcelable.Creator<NextBlockForgedPOTDetail> CREATOR
            = new Parcelable.Creator<NextBlockForgedPOTDetail>() {

        public NextBlockForgedPOTDetail createFromParcel(Parcel in) {

            return new NextBlockForgedPOTDetail(in);
        }

        public NextBlockForgedPOTDetail[] newArray(int size) {

            return new NextBlockForgedPOTDetail[size];
        }
    };

    protected NextBlockForgedPOTDetail(Parcel in) {

        super(in);
        this.baseTarget = new BigInteger(in.readString(), 16);
        this.generationSignature = new BigInteger(in.readString(), 16);
        this.cumulativeDifficulty = new BigInteger(in.readString(), 16);
        this.forgingPower = new BigInteger(in.readString(), 16);
        this.hitValue = new BigInteger(in.readString(), 16);
    }
}
