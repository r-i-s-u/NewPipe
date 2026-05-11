/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.model

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import net.newpipe.app.Constants.KEY_STREAMING_SERVICE
import net.newpipe.app.model.Service.YOUTUBE
import net.newpipe.app.theme.bandCampDarkScheme
import net.newpipe.app.theme.bandCampLightScheme
import net.newpipe.app.theme.mediaCCCDarkScheme
import net.newpipe.app.theme.mediaCCCLightScheme
import net.newpipe.app.theme.peerTubeDarkScheme
import net.newpipe.app.theme.peerTubeLightScheme
import net.newpipe.app.theme.soundCLoudLightScheme
import net.newpipe.app.theme.soundCloudDarkScheme
import net.newpipe.app.theme.youTubeDarkScheme
import net.newpipe.app.theme.youTubeLightScheme
import org.koin.compose.koinInject

/**
 * Supported services in the NewPipe app and minor information about them for UI decisions.
 * @property serviceId ID of the service as defined in NewPipeExtractor
 * @property serviceName Name of the service as defined in NewPipeExtractor
 */
enum class Service(
    val serviceId: Int,
    val serviceName: String,
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme
) {
    YOUTUBE(
        serviceId = 0,
        serviceName = "YouTube",
        lightScheme = youTubeLightScheme,
        darkScheme = youTubeDarkScheme
    ),
    SOUNDCLOUD(
        serviceId = 1,
        serviceName = "SoundCloud",
        lightScheme = soundCLoudLightScheme,
        darkScheme = soundCloudDarkScheme
    ),
    MEDIA_CCC(
        serviceId = 2,
        serviceName = "media.ccc.de",
        lightScheme = mediaCCCLightScheme,
        darkScheme = mediaCCCDarkScheme
    ),
    PEERTUBE(
        serviceId = 3,
        serviceName = "PeerTube",
        lightScheme = peerTubeLightScheme,
        darkScheme = peerTubeDarkScheme
    ),
    BANDCAMP(
        serviceId = 4,
        serviceName = "Bandcamp",
        lightScheme = bandCampLightScheme,
        darkScheme = bandCampDarkScheme
    )
}

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
