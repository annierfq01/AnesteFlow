package com.anestesia.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.anestesia.app.data.local.dao.ActiveTimerDao
import com.anestesia.app.data.local.dao.DrugDao
import com.anestesia.app.data.local.entity.ActiveTimerEntity
import com.anestesia.app.data.local.entity.DrugEntity

@Database(
    entities = [DrugEntity::class, ActiveTimerEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AnestesiaDatabase : RoomDatabase() {
    abstract fun drugDao(): DrugDao
    abstract fun activeTimerDao(): ActiveTimerDao

    companion object {
        const val DATABASE_NAME = "anestesia_db"
    }
}
