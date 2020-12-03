package com.joshtalks.joshskills.ui.voip.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.joshtalks.joshskills.R;

import static android.content.Context.AUDIO_SERVICE;

public class SoundPoolManager {

    private static SoundPoolManager instance;
    private boolean playing = false;
    private boolean loaded = false;
    private boolean playingCalled = false;
    private final float volume;
    private SoundPool soundPool;
    private final int ringingSoundId;
    private int ringingStreamId;
    private final int disconnectSoundId;

    private SoundPoolManager(Context context) {
        // AudioManager audio settings for adjusting the volume
        AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume = maxVolume;

        // Load the sounds
        int maxStreams = 1;
        soundPool = new SoundPool.Builder()
                .setMaxStreams(maxStreams)
                .build();

        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            loaded = true;
            if (playingCalled) {
                playRinging();
                playingCalled = false;
            }
        });
        ringingSoundId = soundPool.load(context, R.raw.incoming, 1);
        disconnectSoundId = soundPool.load(context, R.raw.disconnect, 1);
    }

    public void playRinging() {
        stopRinging();
        if (loaded && !playing) {
            ringingStreamId = soundPool.play(ringingSoundId, volume, volume, 1, -1, 1f);
            playing = true;
        } else {
            playingCalled = true;
        }
    }

    public void stopRinging() {
        if (playing) {
            soundPool.stop(ringingStreamId);
            playing = false;
        }
    }

    public static SoundPoolManager getInstance(Context context) {
        try {
            if (instance == null) {
                instance = new SoundPoolManager(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return instance;
    }

    public void playDisconnect() {
        if (loaded && !playing) {
            soundPool.play(disconnectSoundId, volume, volume, 1, 0, 1f);
            playing = false;
        }
    }

    public void release() {
        if (soundPool != null) {
            soundPool.unload(ringingSoundId);
            soundPool.unload(disconnectSoundId);
            soundPool.release();
            soundPool = null;
        }
        instance = null;
    }

}