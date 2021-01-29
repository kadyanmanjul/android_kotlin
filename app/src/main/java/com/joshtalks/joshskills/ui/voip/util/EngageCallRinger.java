package com.joshtalks.joshskills.ui.voip.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.joshtalks.joshskills.R;

import java.io.IOException;

public class EngageCallRinger {

    private static final String TAG = EngageCallRinger.class.getSimpleName();

    private final Context context;

    private MediaPlayer mediaPlayer;

    public EngageCallRinger(@NonNull Context context) {
        this.context = context;
    }

    public void start() {
        int soundId = R.raw.voip_reconnect;
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();

        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
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
        if (mediaPlayer == null) return;
        mediaPlayer.release();
        mediaPlayer = null;
    }
}