package com.filament.sense.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.filament.sense.data.local.AppDatabase
import com.filament.sense.data.local.dao.MeasurementDao
import com.filament.sense.data.local.dao.SpoolDao
import com.filament.sense.data.repository.DeviceRepositoryImpl
import com.filament.sense.data.repository.SpoolRepositoryImpl
import com.filament.sense.domain.repository.DeviceRepository
import com.filament.sense.domain.repository.SpoolRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindSpoolRepository(impl: SpoolRepositoryImpl): SpoolRepository

    companion object {

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "filament_sense.db",
            ).fallbackToDestructiveMigration().build()

        @Provides
        @Singleton
        fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
            context.getSharedPreferences("filament_prefs", Context.MODE_PRIVATE)

        @Provides
        @Singleton
        fun provideSpoolDao(db: AppDatabase): SpoolDao = db.spoolDao()

        @Provides
        @Singleton
        fun provideMeasurementDao(db: AppDatabase): MeasurementDao = db.measurementDao()
    }
}