package com.joshtalks.joshskills.util;

import android.media.MediaPlayer;

import java.io.IOException;

public class MediaPlayerManager {
    private static MediaPlayerManager manager;

    public static MediaPlayerManager getInstance() {
        if (manager == null) {
            manager = new MediaPlayerManager();
        }
        return manager;
    }

    private MediaPlayer mediaPlayer;


    public void play(String src, MediaPlayer.OnCompletionListener completionListener, MediaPlayer.OnErrorListener errorListener) throws IOException {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(src);
        mediaPlayer.prepareAsync();
        mediaPlayer.setOnCompletionListener(completionListener);
        mediaPlayer.setOnErrorListener(errorListener);
        mediaPlayer.setOnPreparedListener(MediaPlayer::start);
        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {

            }
        });

    }


    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    public void play() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void reset() {
        mediaPlayer.reset();
    }

    public void load(String resourceId) {

        try {
            mediaPlayer.setDataSource(resourceId);
        } catch (Exception e) {
        }

        try {
            mediaPlayer.prepare();
        } catch (Exception e) {
        }
    }

    public void jump(int time) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(time);
        }
    }

}
