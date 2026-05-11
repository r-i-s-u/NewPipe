/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import com.russhwolf.settings.Settings
import net.newpipe.app.Constants.KEY_STREAMING_SERVICE
import net.newpipe.app.model.Service
import net.newpipe.app.model.Service.YOUTUBE
import org.koin.compose.koinInject

val youTubeLightScheme = lightColorScheme(
    primaryContainer = Color(0xFFE53935),
    onPrimaryContainer = Color(0xFFFFFFFF)
)

val youTubeDarkScheme = darkColorScheme(
    primaryContainer = Color(0xFF992722),
    onPrimaryContainer = Color(0xFFFFFFFF)
)

val soundCLoudLightScheme = lightColorScheme(
    primaryContainer = Color(0xFFF57C00),
    onPrimaryContainer = Color(0xFFFFFFFF)
)

val soundCloudDarkScheme = darkColorScheme(
    primaryContainer =  Color(0xFFA35300),
    onPrimaryContainer = Color(0xFFFFFFFF)
)

val mediaCCCLightScheme = lightColorScheme(
    primaryContainer = Color(0xFF9E9E9E),
    onPrimaryContainer = Color(0xFFFFFFFF)
)

val mediaCCCDarkScheme = darkColorScheme(
    primaryContainer = Color(0xFF992722),
    onPrimaryContainer = Color(0xFF878787)
)

val peerTubeLightScheme = lightColorScheme(
    primaryContainer = Color(0xFFFF6F00),
    onPrimaryContainer = Color(0xFFFFFFFF)
)

val peerTubeDarkScheme = darkColorScheme(
    primaryContainer = Color(0xFF992722),
    onPrimaryContainer = Color(0xFFA34700)
)

val bandCampLightScheme = lightColorScheme(
    primaryContainer = Color(0xFF17A0C4),
    onPrimaryContainer = Color(0xFFFFFFFF)
)

val bandCampDarkScheme = darkColorScheme(
    primaryContainer = Color(0xFF1383A1),
    onPrimaryContainer = Color(0xFFFFFFFF)
)

/**
 * Currently active/selected service by user
 */
@Composable
fun currentService(settings: Settings = koinInject()): Service {
    return Service.entries.find { service ->
        service.serviceName == settings.getString(KEY_STREAMING_SERVICE, YOUTUBE.serviceName)
    }!!
}

/**
 * Currently active/selected service's color that can be used to represent it.
 * Fallbacks to YouTube on preview.
 */
@Composable
fun currentServiceScheme(
    isPreview: Boolean = LocalInspectionMode.current,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    service: Service = if (isPreview) YOUTUBE else currentService()
): ColorScheme {
    return when {
        useDarkTheme -> service.darkScheme
        else -> service.lightScheme
    }
}

/**
 * Top app bar colors to represent the currently active service.
 * Fallbacks to YouTube on preview.
 */
@Composable
fun currentServiceTopAppBarColors(
    isPreview: Boolean = LocalInspectionMode.current,
    serviceScheme: ColorScheme = if (isPreview) youTubeLightScheme else currentServiceScheme()
): TopAppBarColors {
    return TopAppBarDefaults.topAppBarColors(
        containerColor = serviceScheme.primaryContainer,
        scrolledContainerColor = serviceScheme.primaryContainer,
        navigationIconContentColor = serviceScheme.onPrimaryContainer,
        titleContentColor = serviceScheme.onPrimaryContainer,
        subtitleContentColor = serviceScheme.onPrimaryContainer,
        actionIconContentColor = serviceScheme.onPrimaryContainer
    )
}
