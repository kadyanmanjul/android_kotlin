package com.joshtalks.joshskills.di

import android.app.Application
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Module
class ApplicationModule {
    @AppScope
    @Provides
    fun provideScope() : CoroutineScope {
        return CoroutineScope(Dispatchers.IO)
    }
}