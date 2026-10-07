/*
 * SPDX-FileCopyrightText: 2020-2023 NewPipe contributors <https://newpipe.net>
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.database.stream

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Ignore
import java.time.OffsetDateTime
import org.schabi.newpipe.database.LocalItem
import org.schabi.newpipe.database.stream.model.StreamEntity
import org.schabi.newpipe.database.stream.model.StreamStateEntity.Companion.STREAM_PROGRESS_MILLIS
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class StreamStatisticsEntry(
    @Embedded
    val streamEntity: StreamEntity,

    @ColumnInfo(name = STREAM_PROGRESS_MILLIS, defaultValue = "0")
    val progressMillis: Long,

    @ColumnInfo(name = "stream_id")
    val streamId: Long,

    @ColumnInfo(name = STREAM_LATEST_DATE)
    val latestAccessDate: OffsetDateTime,

    @ColumnInfo(name = STREAM_WATCH_COUNT)
    val watchCount: Long
) : LocalItem {

    override val localItemType: LocalItem.LocalItemType
        get() = LocalItem.LocalItemType.STATISTIC_STREAM_ITEM

    @Ignore
    fun toStreamInfoItem(): StreamInfoItem {
        return StreamInfoItem(
            streamEntity.serviceId,
            streamEntity.url,
            streamEntity.title,
            streamEntity.streamType
        ).apply {
            duration = streamEntity.duration
            uploaderName = streamEntity.uploader
            uploaderUrl = streamEntity.uploaderUrl
            thumbnails = (streamEntity.thumbnailUrl?.let { listOf(org.schabi.newpipe.extractor.Image(it, org.schabi.newpipe.extractor.Image.HEIGHT_UNKNOWN, org.schabi.newpipe.extractor.Image.WIDTH_UNKNOWN, org.schabi.newpipe.extractor.Image.ResolutionLevel.UNKNOWN)) } ?: emptyList())
        }
    }

    companion object {
        const val STREAM_LATEST_DATE = "latestAccess"
        const val STREAM_WATCH_COUNT = "watchCount"
    }
}
