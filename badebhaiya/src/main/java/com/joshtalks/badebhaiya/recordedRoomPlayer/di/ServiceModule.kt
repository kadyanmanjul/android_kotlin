package com.joshtalks.badebhaiya.recordedRoomPlayer.di

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.exoplayer2.C.*
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

//    @ServiceScoped
//    @Provides
//    fun provideMusicDatabase() = MusicDatabase()

//    @ServiceScoped
//    @Provides
//    fun provideAudioAttributes() = AudioAttributes.Builder()
//        .setContentType(AUDIO_CONTENT_TYPE_MUSIC)
//        .setUsage(C.USAGE_MEDIA)
//        .build()

    @SuppressLint("WrongConstant")
    @ServiceScoped
    @Provides
    fun provideAudioAttributes() = AudioAttributes.Builder()
        .setContentType(2)
        .setUsage(USAGE_MEDIA)
        .build()

    @ServiceScoped
    @Provides
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes
    ) = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(audioAttributes, true)
        setHandleAudioBecomingNoisy(true)
    }

    @ServiceScoped
    @Provides
    fun provideDataSourceFactory(
        @ApplicationContext context: Context
    ) = DefaultDataSourceFactory(context, Util.getUserAgent(context, "BadeBhaiya"))



//    @ServiceScoped
//    @Provides
//    fun provideDataSourceFactory(
//        @ApplicationContext context: Context
//    ) = DefaultHttpDataSource.Factory().apply {
//
//    }
}