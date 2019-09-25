package com.joshtalks.joshskills.core.service.video_download;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;

public class ExoPlayerDownloader {

    volatile private static SimpleExoPlayer downloadExoPlayer;

    public static final int PLAYER_MAX_CACHE = 100 * 1024 * 1024;//200 MB TOTAL CACHE
    public static final int PLAYER_MAX_FILE_SIZE = 20 * 1024 * 1024;//30 MB MAX FILE SIZE

    public static void download(Context context,String url){
/*
        TrackSelector trackSelector = new DefaultTrackSelector();
        downloadExoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
        downloadExoPlayer.setVolume(0);

        CacheDataSourceFactory dataSourceFactory = new CacheDataSourceFactory();

        MediaSource videoSource;


        if (url.endsWith("m3u8")) {
            videoSource = new HlsMediaSource
                    .Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(url));
        } else {
            videoSource = new ExtractorMediaSource
                    .Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(url));
        }

        downloadExoPlayer.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {

                Log.e("player",""+playbackState);
                if (playbackState == com.google.android.exoplayer2.Player.STATE_IDLE
                        || playbackState == com.google.android.exoplayer2.Player.STATE_ENDED
                        || !playWhenReady) {

                }



            }

        });

        downloadExoPlayer.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason) {

            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            }

            @Override
            public void onLoadingChanged(boolean isLoading) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {

            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {

            }

            @Override
            public void onPositionDiscontinuity(int reason) {

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

            }

            @Override
            public void onSeekProcessed() {

            }
        });
        downloadExoPlayer.prepare(videoSource);
        downloadExoPlayer.setPlayWhenReady(true);*/

    }
}
