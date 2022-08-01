package com.joshtalks.joshskills.di.module

import com.joshtalks.joshskills.di.annotation.AppScope
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