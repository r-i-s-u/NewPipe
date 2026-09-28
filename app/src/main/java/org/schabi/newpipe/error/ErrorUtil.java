package org.schabi.newpipe.error;

import android.content.Context;
import androidx.fragment.app.Fragment;

public class ErrorUtil {
    public static void showSnackbar(Object ctx, ErrorInfo info) {}
    public static void showUiErrorSnackbar(Object ctx, String s, Throwable t) {}
    public static void createNotification(Context ctx, ErrorInfo info) {}
    public static void openActivity(Context ctx, ErrorInfo info) {}

    public static class Companion {
        public static void createNotification(Context ctx, ErrorInfo info) {}
    }
}
