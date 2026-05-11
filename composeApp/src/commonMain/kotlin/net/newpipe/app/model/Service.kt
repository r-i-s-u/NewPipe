/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.model

import androidx.compose.material3.ColorScheme
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
