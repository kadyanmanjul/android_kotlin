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
        recorder.setAudioEncodingBitRate(128000);
        recorder.setAudioSamplingRate(44100);
        /*
        recorder.setAudioSamplingRate(16000);
        recorder.setAudioEncodingBitRate(32000);
        *///recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
        recorder.setOutputFile(recordFile.getAbsolutePath());
        MediaRecorder.OnErrorListener errorListener = (arg0, what, extra) -> {
            arg0.release();
            LogException.INSTANCE.catchError(ErrorTag.AUDIO_RECORDER, what + " , " + extra);
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
            recorder.reset();
            recorder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        recorder = null;
    }
}
