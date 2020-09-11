package com.joshtalks.joshskills.ui.voip.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.core.AppObjectController;
import java.io.FileInputStream;
import java.io.IOException;

public class AudioPlayer {

    static final String LOG_TAG = AudioPlayer.class.getSimpleName();
    private final static int SAMPLE_RATE = 16000;
    private static AudioPlayer instance;
    private static AudioTrack mProgressTone;

    private AudioPlayer() {
    }

    public static AudioPlayer getInstance() {
        if (instance == null)
            instance = new AudioPlayer();
        return instance;
    }

    public void playProgressTone() {
        stopProgressTone();
        try {
            AudioManager audioManager = (AudioManager) AppObjectController.getJoshApplication().getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(false);
            mProgressTone = createProgressTone();
            mProgressTone.play();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not play progress tone", e);
        }
    }

    public void stopProgressTone() {
        if (mProgressTone != null) {
            mProgressTone.stop();
            mProgressTone.release();
            mProgressTone = null;
        }
    }

    private static AudioTrack createProgressTone() throws IOException {
        AssetFileDescriptor fd = AppObjectController.getJoshApplication().getResources().openRawResourceFd(R.raw.progress_tone);
        int length = (int) fd.getLength();

        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, length, AudioTrack.MODE_STATIC);

        byte[] data = new byte[length];
        readFileToBytes(fd, data);

        audioTrack.write(data, 0, data.length);
        audioTrack.setLoopPoints(0, data.length / 2, 30);
        return audioTrack;
    }

    private static void readFileToBytes(AssetFileDescriptor fd, byte[] data) throws IOException {
        FileInputStream inputStream = fd.createInputStream();

        int bytesRead = 0;
        while (bytesRead < data.length) {
            int res = inputStream.read(data, bytesRead, (data.length - bytesRead));
            if (res == -1) {
                break;
            }
            bytesRead += res;
        }
    }
}
