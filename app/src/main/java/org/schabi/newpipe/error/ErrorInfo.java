package org.schabi.newpipe.error;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ErrorInfo implements Parcelable {
    private final Throwable throwable;
    private final UserAction userAction;
    private final String message;
    private final int serviceId;
    private final String url;
    private final List<Throwable> errors;
    private final String recaptchaUrl;

    /**
     * Constructor with throwable, action, and message
     */
    public ErrorInfo(@NonNull final Throwable t, @NonNull final UserAction a, @NonNull final String s) {
        this(t, a, s, -1);
    }

    /**
     * Constructor with throwable, action, message, and service ID
     */
    public ErrorInfo(@NonNull final Throwable t, @NonNull final UserAction a, 
                    @NonNull final String s, final int id) {
        this(t, a, s, id, null);
    }

    /**
     * Constructor with throwable, action, message, service ID, and URL
     */
    public ErrorInfo(@NonNull final Throwable t, @NonNull final UserAction a,
                    @NonNull final String s, final int id, @Nullable final String url) {
        this.throwable = t;
        this.userAction = a;
        this.message = s;
        this.serviceId = id;
        this.url = url;
        this.errors = new ArrayList<>();
        this.recaptchaUrl = null;
    }

    /**
     * Constructor with list of errors
     */
    public ErrorInfo(@NonNull final List<Throwable> errors, @NonNull final UserAction a,
                    @NonNull final String s, final int id) {
        this(errors, a, s, id, null);
    }

    /**
     * Constructor with list of errors and URL
     */
    public ErrorInfo(@NonNull final List<Throwable> errors, @NonNull final UserAction a,
                    @NonNull final String s, final int id, @Nullable final String url) {
        this.throwable = null;
        this.userAction = a;
        this.message = s;
        this.serviceId = id;
        this.url = url;
        this.errors = new ArrayList<>(errors);
        this.recaptchaUrl = null;
    }

    protected ErrorInfo(Parcel in) {
        this.message = in.readString();
        this.serviceId = in.readInt();
        this.url = in.readString();
        this.userAction = UserAction.valueOf(in.readString());
        this.throwable = null;
        this.errors = new ArrayList<>();
        this.recaptchaUrl = in.readString();
    }

    public static final Creator<ErrorInfo> CREATOR = new Creator<ErrorInfo>() {
        @Override
        public ErrorInfo createFromParcel(Parcel in) {
            return new ErrorInfo(in);
        }

        @Override
        public ErrorInfo[] newArray(int size) {
            return new ErrorInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(message);
        dest.writeInt(serviceId);
        dest.writeString(url);
        dest.writeString(userAction.name());
        dest.writeString(recaptchaUrl);
    }

    // Getters
    @Nullable
    public Throwable getThrowable() {
        return throwable;
    }

    @NonNull
    public UserAction getUserAction() {
        return userAction;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    public int getServiceId() {
        return serviceId;
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    @NonNull
    public List<Throwable> getErrors() {
        return errors;
    }

    @Nullable
    public String getRecaptchaUrl() {
        return recaptchaUrl;
    }

    // Companion object equivalent for Java
    public static class Companion {
        @NonNull
        public static List<String> throwableToStringList(@Nullable final Throwable throwable) {
            final List<String> list = new ArrayList<>();
            if (throwable != null) {
                list.add(throwable.getMessage() != null ? throwable.getMessage() : throwable.toString());
            }
            return list;
        }
    }
}
