package com.joshtalks.joshskills.di

import com.joshtalks.joshskills.core.AppObjectController
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit

@Module
class NetworkModule {
    @AppScope
    @Provides
    fun provideRetrofit() : Retrofit {
        return AppObjectController.retrofit
    }
}