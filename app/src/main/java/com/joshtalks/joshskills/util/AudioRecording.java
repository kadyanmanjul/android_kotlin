package com.joshtalks.joshskills.util;

import android.media.MediaRecorder;
import com.joshtalks.joshskills.core.analytics.ErrorTag;
import com.joshtalks.joshskills.core.analytics.LogException;
import java.io.File;


public class AudioRecording {
    public static final AudioRecording audioRecording = new AudioRecording();
    private MediaRecorder recorder;

    public void startPlayer(File recordFile) {
        recorder = new MediaRecorder();

        recorder.setAudioChannels(1);
        recorder.setAudioSamplingRate(16000);
        recorder.setAudioEncodingBitRate(32000);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(recordFile.getAbsolutePath());
        MediaRecorder.OnErrorListener errorListener = new MediaRecorder.OnErrorListener() {
            public void onError(MediaRecorder arg0, int arg1, int arg2) {
                LogException.INSTANCE.catchError(ErrorTag.AUDIO_RECORDER, arg1 + " , " + arg2);
            }
        };
        recorder.setOnErrorListener(errorListener);
        try {
            recorder.prepare();
            Thread.sleep(500);
            recorder.start();
        } catch (Exception e) {
            e.printStackTrace();
            //LogException.INSTANCE.catchException(e);

        }
    }

    public void stopPlaying() {
        try {
            recorder.stop();
            recorder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        recorder = null;
    }
}
