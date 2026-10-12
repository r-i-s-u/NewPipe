/*
 * Copyright 2018 Mauricio Colli <mauriciocolli@outlook.com>
 * ThemeHelper.java is part of NewPipe
 *
 * License: GPL-3.0+
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import org.schabi.newpipe.R;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

public final class ThemeHelper {
    private ThemeHelper() {
    }

    /**
     * Apply the selected theme (on NewPipe settings) in the context
     * with the default style (see {@link #setTheme(Context, int)}).
     *
     * ThemeHelper.setDayNightMode should be called before
     * the applying theme for the first time in session
     *
     * @param context context that the theme will be applied
     */
    public static void setTheme(final Context context) {
        setTheme(context, -1);
    }

    /**
     * Apply the selected theme (on NewPipe settings) in the context,
     * themed according with the styles defined for the service .
     *
     * ThemeHelper.setDayNightMode should be called before
     * the applying theme for the first time in session
     *
     * @param context   context that the theme will be applied
     * @param serviceId the theme will be styled to the service with this id,
     *                  pass -1 to get the default style
     */
    public static void setTheme(final Context context, final int serviceId) {
        context.setTheme(getThemeForService(context, serviceId));
    }

    /**
     * Return true if the selected theme (on NewPipe settings) is the Light theme.
     *
     * @param context context to get the preference
     * @return whether the light theme is selected
     */
    public static boolean isLightThemeSelected(final Context context) {
        final String selectedThemeKey = getSelectedThemeKey(context);
        final Resources res = context.getResources();

        return selectedThemeKey.equals(res.getString(R.string.light_theme_key))
                || (selectedThemeKey.equals(res.getString(R.string.auto_device_theme_key))
                && !isDeviceDarkThemeEnabled(context));
    }

    /**
     * Return a dialog theme styled according to the (default) selected theme.
     *
     * @param context context to get the selected theme
     * @return the dialog style (the default one)
     */
    @StyleRes
    public static int getDialogTheme(final Context context) {
        return isLightThemeSelected(context) ? R.style.LightDialogTheme : R.style.DarkDialogTheme;
    }

    /**
     * Return the selected theme styled according to the serviceId.
     *
     * @param context   context to get the selected theme
     * @param serviceId return a theme styled to this service,
     *                  -1 to get the default
     * @return the selected style (styled)
     */
    @StyleRes
    public static int getThemeForService(final Context context, final int serviceId) {
        final Resources res = context.getResources();
        final String lightThemeKey = res.getString(R.string.light_theme_key);
        final String blackThemeKey = res.getString(R.string.black_theme_key);
        final String automaticDeviceThemeKey = res.getString(R.string.auto_device_theme_key);

        final String selectedThemeKey = getSelectedThemeKey(context);

        int baseTheme = R.style.DarkTheme; // default to dark theme
        if (selectedThemeKey.equals(lightThemeKey)) {
            baseTheme = R.style.LightTheme;
        } else if (selectedThemeKey.equals(blackThemeKey)) {
            baseTheme = R.style.BlackTheme;
        } else if (selectedThemeKey.equals(automaticDeviceThemeKey)) {

            if (isDeviceDarkThemeEnabled(context)) {
                // use the dark theme variant preferred by the user
                final String selectedNightThemeKey = getSelectedNightThemeKey(context);
                if (selectedNightThemeKey.equals(blackThemeKey)) {
                    baseTheme = R.style.BlackTheme;
                } else {
                    baseTheme = R.style.DarkTheme;
                }
            } else {
                // there is only one day theme
                baseTheme = R.style.LightTheme;
            }
        }

        if (serviceId <= -1) {
            return baseTheme;
        }

        final StreamingService service;
        try {
            service = NewPipe.getService(serviceId);
        } catch (final ExtractionException ignored) {
            return baseTheme;
        }

        String themeName = "DarkTheme"; // default
        if (baseTheme == R.style.LightTheme) {
            themeName = "LightTheme";
        } else if (baseTheme == R.style.BlackTheme) {
            themeName = "BlackTheme";
        }

        themeName += "." + service.getServiceInfo().getName();
        final int resourceId = getThemeOrDefault(themeName, baseTheme);

        if (resourceId > 0) {
            return resourceId;
        }
        return baseTheme;
    }

    @StyleRes
    public static int getSettingsThemeStyle(final Context context) {
        final Resources res = context.getResources();
        final String lightTheme = res.getString(R.string.light_theme_key);
        final String blackTheme = res.getString(R.string.black_theme_key);
        final String automaticDeviceTheme = res.getString(R.string.auto_device_theme_key);

        final String selectedTheme = getSelectedThemeKey(context);

        if (selectedTheme.equals(lightTheme)) {
            return R.style.LightSettingsTheme;
        } else if (selectedTheme.equals(blackTheme)) {
            return R.style.BlackSettingsTheme;
        } else if (selectedTheme.equals(automaticDeviceTheme)) {
            if (isDeviceDarkThemeEnabled(context)) {
                // use the dark theme variant preferred by the user
                final String selectedNightTheme = getSelectedNightThemeKey(context);
                if (selectedNightTheme.equals(blackTheme)) {
                    return R.style.BlackSettingsTheme;
                } else {
                    return R.style.DarkSettingsTheme;
                }
            } else {
                // there is only one day theme
                return R.style.LightSettingsTheme;
            }
        } else {
            // default to dark theme
            return R.style.DarkSettingsTheme;
        }
    }

    private static String getSelectedThemeKey(final Context context) {
        final String themeKey = context.getString(R.string.theme_key);
        final String defaultTheme = context.getString(R.string.default_theme_value);
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(themeKey, defaultTheme);
    }

    private static String getSelectedNightThemeKey(final Context context) {
        final String nightThemeKey = context.getString(R.string.night_theme_key);
        final String defaultNightTheme = context.getResources()
                .getString(R.string.default_night_theme_value);
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(nightThemeKey, defaultNightTheme);
    }

    /**
     * Sets the title to the activity, if the activity is an {@link AppCompatActivity} and has an
     * action bar.
     *
     * @param activity the activity to set the title of
     * @param title    the title to set to the activity
     */
    public static void setTitleToAppCompatActivity(@Nullable final Activity activity,
                                                   final CharSequence title) {
        if (activity instanceof AppCompatActivity) {
            final ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
            }
        }
    }

    /**
     * Get the device theme
     * <p>
     * It will return true if the device 's theme is dark, false otherwise.
     * <p>
     * From https://developer.android.com/guide/topics/ui/look-and-feel/darktheme#java
     *
     * @param context the context to use
     * @return true:dark theme, false:light or unknown
     */
    public static boolean isDeviceDarkThemeEnabled(final Context context) {
        final int deviceTheme = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        switch (deviceTheme) {
            case Configuration.UI_MODE_NIGHT_YES:
                return true;
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
            case Configuration.UI_MODE_NIGHT_NO:
            default:
                return false;
        }
    }

    public static void setDayNightMode(final Context context) {
        setDayNightMode(context, ThemeHelper.getSelectedThemeKey(context));
    }

    public static void setDayNightMode(final Context context, final String selectedThemeKey) {
        final Resources res = context.getResources();

        if (selectedThemeKey.equals(res.getString(R.string.light_theme_key))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (selectedThemeKey.equals(res.getString(R.string.dark_theme_key))
                || selectedThemeKey.equals(res.getString(R.string.black_theme_key))) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    @StyleRes
    private static int getThemeOrDefault(final String name, @StyleRes final int baseTheme) {
        return switch (name) {
            case "LightTheme.YouTube" -> R.style.LightTheme_YouTube;
            case "DarkTheme.YouTube" -> R.style.DarkTheme_YouTube;
            case "BlackTheme.YouTube" -> R.style.BlackTheme_YouTube;
            case "LightTheme.SoundCloud" -> R.style.LightTheme_SoundCloud;
            case "DarkTheme.SoundCloud" -> R.style.DarkTheme_SoundCloud;
            case "BlackTheme.SoundCloud" -> R.style.BlackTheme_SoundCloud;
            case "LightTheme.PeerTube" -> R.style.LightTheme_PeerTube;
            case "DarkTheme.PeerTube" -> R.style.DarkTheme_PeerTube;
            case "BlackTheme.PeerTube" -> R.style.BlackTheme_PeerTube;
            case "LightTheme.media.ccc.de" -> R.style.LightTheme_media_ccc_de;
            case "DarkTheme.media.ccc.de" -> R.style.DarkTheme_media_ccc_de;
            case "BlackTheme.media.ccc.de" -> R.style.BlackTheme_media_ccc_de;
            case "LightTheme.Bandcamp" -> R.style.LightTheme_Bandcamp;
            case "DarkTheme.Bandcamp" -> R.style.DarkTheme_Bandcamp;
            case "BlackTheme.Bandcamp" -> R.style.BlackTheme_Bandcamp;
            default -> baseTheme;
        };
    }
}
