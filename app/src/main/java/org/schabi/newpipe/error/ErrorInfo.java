package org.schabi.newpipe.error;

import android.os.Parcel;
import android.os.Parcelable;

public class ErrorInfo implements Parcelable {
    public ErrorInfo(Throwable t, UserAction a, String s) {}
    public ErrorInfo(Throwable t, UserAction a, String s, int id) {}

    protected ErrorInfo(Parcel in) {}

    public static final Creator<ErrorInfo> CREATOR = new Creator<ErrorInfo>() {
        @Override
        public ErrorInfo createFromParcel(Parcel in) { return new ErrorInfo(in); }
        @Override
        public ErrorInfo[] newArray(int size) { return new ErrorInfo[size]; }
    };

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {}
}
