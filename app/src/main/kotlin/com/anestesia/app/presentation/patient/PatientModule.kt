package com.anestesia.app.presentation.patient

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * PatientViewModel is also used as a plain singleton service (not a @HiltViewModel)
 * so that MainViewModel can inject it directly. This avoids the complexity of
 * sharing state through a shared StateFlow repository.
 */
@Module
@InstallIn(SingletonComponent::class)
object PatientModule {

    @Provides
    @Singleton
    fun providePatientViewModel(
        @ApplicationContext context: Context
    ): PatientViewModel = PatientViewModel(context)
}
