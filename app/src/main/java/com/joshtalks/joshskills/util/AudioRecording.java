package com.joshtalks.joshskills.util;

import android.media.MediaRecorder;
import java.io.File;
import java.io.IOException;

public class AudioRecording {
    private MediaRecorder recorder;
    public static AudioRecording audioRecording = new AudioRecording();


    public void startPlayer(File recordFile) {
        recorder = new MediaRecorder();
        recorder.setAudioChannels(1);
        recorder.setAudioSamplingRate(10000);
        recorder.setAudioEncodingBitRate(4750);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(recordFile.getAbsolutePath());
        try {
            recorder.prepare();
            recorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopPlaying() {
        try {
            recorder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        recorder = null;

    }
}
