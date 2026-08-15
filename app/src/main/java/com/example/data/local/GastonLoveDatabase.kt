package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserEntity::class,
        LikeEntity::class,
        MatchEntity::class,
        MessageEntity::class,
        NotificationEntity::class,
        ReportEntity::class,
        BlockedUserEntity::class,
        PlatformSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GastonLoveDatabase : RoomDatabase() {
    abstract fun dao(): GastonLoveDao

    companion object {
        @Volatile
        private var INSTANCE: GastonLoveDatabase? = null

        fun getDatabase(context: Context): GastonLoveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GastonLoveDatabase::class.java,
                    "gaston_love_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
