package com.joshtalks.badebhaiya.di

import com.joshtalks.badebhaiya.repository.service.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideRoomApi(): ConversationRoomNetworkService {
        return RetrofitInstance.conversationRoomNetworkService
    }

    @Singleton
    @Provides
    fun provideSignUpApi(): SignUpNetworkService {
        return RetrofitInstance.signUpNetworkService
    }

    @Singleton
    @Provides
    fun provideCommonApi(): CommonNetworkService {
        return RetrofitInstance.commonNetworkService
    }

    @Singleton
    @Provides
    fun provideProfileApi(): ProfileNetworkService {
        return RetrofitInstance.profileNetworkService
    }

    @Singleton
    @Provides
    fun provideMediaDUApi(): MediaDUNetworkService {
        return RetrofitInstance.mediaDUNetworkService
    }

    @Singleton
    @Provides
    fun provideRetrofit(): Retrofit {
        return RetrofitInstance.retrofit
    }



}