package org.schabi.newpipe.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.schabi.newpipe.database.stream.dao.StreamDAO
import org.schabi.newpipe.database.stream.dao.StreamStateDAO
import org.schabi.newpipe.database.stream.model.StreamEntity
import org.schabi.newpipe.database.stream.model.StreamStateEntity

@TypeConverters(Converters::class)
@Database(
    version = Migrations.DB_VER_9,
    entities = [
        StreamEntity::class,
        StreamStateEntity::class,
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun streamDAO(): StreamDAO
    abstract fun streamStateDAO(): StreamStateDAO

    companion object {
        const val DATABASE_NAME: String = "newpipe.db"
    }
}
