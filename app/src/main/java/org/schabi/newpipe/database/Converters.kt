package org.schabi.newpipe.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.schabi.newpipe.extractor.stream.StreamType

class Converters {
    /**
     * Convert a long value to a [OffsetDateTime].
     *
     * @param value the long value
     * @return the `OffsetDateTime`
     */
    @TypeConverter
    fun offsetDateTimeFromTimestamp(value: Long?): OffsetDateTime? {
        return value?.let { OffsetDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) }
    }

    /**
     * Convert a [OffsetDateTime] to a long value.
     *
     * @param offsetDateTime the `OffsetDateTime`
     * @return the long value
     */
    @TypeConverter
    fun offsetDateTimeToTimestamp(offsetDateTime: OffsetDateTime?): Long? {
        return offsetDateTime?.withOffsetSameInstant(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    }

    @TypeConverter
    fun streamTypeOf(value: String): StreamType {
        return StreamType.valueOf(value)
    }

    @TypeConverter
    fun stringOf(streamType: StreamType): String {
        return streamType.name
    }


}
