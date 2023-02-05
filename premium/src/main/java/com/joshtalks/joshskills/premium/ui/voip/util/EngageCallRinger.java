package com.joshtalks.joshskills.premium.ui.voip.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.joshtalks.joshskills.premium.R;

import java.io.IOException;

public class EngageCallRinger {

    private final Context context;
    private MediaPlayer mediaPlayer;

    public EngageCallRinger(@NonNull Context context) {
        this.context = context;
    }

    public void start() {
        int soundId = R.raw.voip_reconnect;
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();

        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .build());
        mediaPlayer.setLooping(true);

        String packageName = context.getPackageName();
        Uri dataUri = Uri.parse("android.resource://" + packageName + "/" + soundId);

        try {
            mediaPlayer.setDataSource(context, dataUri);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (mediaPlayer == null) return;
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}