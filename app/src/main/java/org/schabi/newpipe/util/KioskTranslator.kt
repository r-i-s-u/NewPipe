/*
 * SPDX-FileCopyrightText: 2017-2025 NewPipe contributors <https://newpipe.net>
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.util

import android.content.Context
import org.schabi.newpipe.R

object KioskTranslator {
    @JvmStatic
    fun getTranslatedKioskName(kioskId: String, context: Context): String {
        return when (kioskId) {
            "Trending" -> context.getString(R.string.trending)
            "Top 50" -> context.getString(R.string.top_50)
            "New & hot" -> context.getString(R.string.new_and_hot)
            "Local" -> context.getString(R.string.local)
            "Recently added" -> context.getString(R.string.recently_added)
            "Most liked" -> context.getString(R.string.most_liked)
            "conferences" -> context.getString(R.string.conferences)
            "recent" -> context.getString(R.string.recent)
            "live" -> context.getString(R.string.duration_live)
            "Featured" -> context.getString(R.string.featured)
            "Radio" -> context.getString(R.string.radio)
            "trending_gaming" -> context.getString(R.string.trending_gaming)
            "trending_music" -> context.getString(R.string.trending_music)
            "trending_movies_and_shows" -> context.getString(R.string.trending_movies)
            "trending_podcasts_episodes" -> context.getString(R.string.trending_podcasts)
            else -> kioskId
        }
    }

    @JvmStatic
    fun getKioskIcon(kioskId: String): Int {
        return when (kioskId) {
            "Trending", "Top 50", "New & hot", "conferences" -> 0
            "Local" -> 0
            "Recently added", "recent" -> 0
            "Most liked" -> 0
            "live" -> 0
            "Featured" -> 0
            "Radio" -> 0
            "trending_gaming" -> 0
            "trending_music" -> 0
            "trending_movies_and_shows" -> 0
            "trending_podcasts_episodes" -> 0
            else -> 0
        }
    }
}
