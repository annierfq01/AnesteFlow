package com.anestesia.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anestesia.app.data.local.dao.ActiveTimerDao
import com.anestesia.app.data.local.dao.DrugDao
import com.anestesia.app.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Migración 1→2: limpia el vademécum para que el seed actualizado (v2)
     * se re-inserte con datos clínicos correctos en el siguiente arranque.
     * Los timers activos se conservan (no se tocan).
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Borrar fármacos del seed antiguo (datos incorrectos)
            db.execSQL("DELETE FROM drugs")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AnestesiaDatabase =
        Room.databaseBuilder(
            context,
            AnestesiaDatabase::class.java,
            AnestesiaDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideDrugDao(db: AnestesiaDatabase): DrugDao = db.drugDao()

    @Provides
    fun provideActiveTimerDao(db: AnestesiaDatabase): ActiveTimerDao = db.activeTimerDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDrugRepository(impl: DrugRepositoryImpl): DrugRepository

    @Binds
    @Singleton
    abstract fun bindTimerRepository(impl: TimerRepositoryImpl): TimerRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
