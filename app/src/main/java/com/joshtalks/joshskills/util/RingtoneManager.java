package com.joshtalks.joshskills.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import java.io.FileInputStream;
import java.io.IOException;

import static com.facebook.FacebookSdk.getApplicationContext;

public class RingtoneManager {

    static final String LOG_TAG = RingtoneManager.class.getSimpleName();
    private final static int SAMPLE_RATE = 16000;
    private static RingtoneManager instance;
    private Context mContext;
    private MediaPlayer mPlayer;
    private AudioTrack mProgressTone;

    public RingtoneManager(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static RingtoneManager getInstance(Context context) {
        if (instance == null)
            instance = new RingtoneManager(context);
        return instance;
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

    public void playRingtone() {
        android.media.AudioManager audioManager = (android.media.AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        if (mPlayer != null)
            System.out.println("AudioPlayer.playRingtone " + mPlayer.isPlaying() + " " + mPlayer.isLooping());
        if (mPlayer != null && mPlayer.isPlaying())
            stopRingtone();
        // Honour silent mode
        if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            mPlayer = new MediaPlayer();
            mPlayer.setAudioStreamType(AudioManager.STREAM_RING);

            try {
                mPlayer.setDataSource(mContext,
                        android.media.RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), android.media.RingtoneManager.TYPE_RINGTONE));
                mPlayer.prepare();
            } catch (IOException e) {
                mPlayer = null;
                return;
            }
            mPlayer.setLooping(true);
            mPlayer.start();
        }
    }

    public void stopRingtone() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }
}
