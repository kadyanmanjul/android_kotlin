package com.joshtalks.joshskills.util;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class AudioPlayerManager {
    volatile private static AudioPlayerManager manager;
    volatile private static SimpleExoPlayer exoPlayer;
    volatile private static Context context;
    volatile private static String AUDIO_TAG = "";
    volatile private static String LAST_ID = "";

    private volatile static List<WeakReference<ExoPlayer.EventListener>> weakReferenceArrayList = new ArrayList<>();


    public static AudioPlayerManager getInstance(Context context) {
        if (manager == null) {
            manager = new AudioPlayerManager();
            TrackSelector trackSelector = new DefaultTrackSelector();
            exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
            AudioPlayerManager.context = context;


        }
        return manager;
    }


    public void play(Uri uri, ExoPlayer.EventListener eventListener, String audioId) {
        if (uri == null) {
            return;
        }

        if (getAudioTag(uri).equalsIgnoreCase(AUDIO_TAG)) {
            if (exoPlayer.getPlaybackState() == Player.STATE_READY && exoPlayer.getPlayWhenReady()) {
                exoPlayer.setPlayWhenReady(false);
                return;
            } else if (exoPlayer.getPlaybackState() == Player.STATE_ENDED && exoPlayer.getPlayWhenReady()) {
                exoPlayer.setPlayWhenReady(false);
                exoPlayer.seekTo(0);
                // EngagementNetworkHelper.engageAudioApi(new AudioEngage(new ArrayList<>(), audioId, exoPlayer.getDuration()));
                return;
            } else {

            }
        } else {
            if (LAST_ID != null) {
                // EngagementNetworkHelper.engageAudioApi(new AudioEngage(new ArrayList<>(), LAST_ID, exoPlayer.getCurrentPosition()));
            }
            exoPlayer.seekTo(0);
            exoPlayer.setPlayWhenReady(false);
            if (weakReferenceArrayList != null && weakReferenceArrayList.size() > 0) {
                for (WeakReference<ExoPlayer.EventListener> weakReference : weakReferenceArrayList) {
                    weakReference.get().onPlayerStateChanged(false, Player.STATE_ENDED);
                }
            }
            weakReferenceArrayList.clear();
        }
        LAST_ID = audioId;
        weakReferenceArrayList.add(new WeakReference<>(eventListener));
        AUDIO_TAG = uri.getPathSegments().get(uri.getPathSegments().size() - 1);
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "exoplayer2example"), null);
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        MediaSource audioSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null, "TT");
        exoPlayer.addListener(eventListener);
        exoPlayer.prepare(audioSource);
        exoPlayer.setPlayWhenReady(true);
        weakReferenceArrayList.add(new WeakReference<>(eventListener));


    }


    String getAudioTag(Uri uri) {
        return uri.getPathSegments().get(uri.getPathSegments().size() - 1);
    }


    public void pause() {
        exoPlayer.setPlayWhenReady(false);
        if (getDuration() == getCurrentPosition()) {
            seekTo(0);
        }
    }

    public int getDuration() {
        return (int) (exoPlayer.getDuration());
    }

    public void seekTo(int i) {
        exoPlayer.seekTo(i);
    }

    public int getCurrentPosition() {
        return (int) exoPlayer.getCurrentPosition();
    }
}