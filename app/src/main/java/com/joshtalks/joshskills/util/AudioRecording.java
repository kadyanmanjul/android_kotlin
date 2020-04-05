package com.joshtalks.joshskills.util;

import android.media.MediaRecorder;

import java.io.File;

public class AudioRecording {
    public static final AudioRecording audioRecording = new AudioRecording();
    private MediaRecorder recorder;

    public void startPlayer(File recordFile) {
        recorder = new MediaRecorder();
        //recorder.setAudioChannels(2);
        recorder.setAudioSamplingRate(16000);
        recorder.setAudioEncodingBitRate(32000);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
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
