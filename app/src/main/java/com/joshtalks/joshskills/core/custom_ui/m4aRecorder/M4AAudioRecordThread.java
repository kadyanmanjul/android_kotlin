package com.joshtalks.joshskills.core.custom_ui.m4aRecorder;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import timber.log.Timber;

public class M4AAudioRecordThread implements Runnable, RecorderContract.RecorderCallback {

    private static final String TAG = M4AAudioRecordThread.class.getSimpleName();


    private final OnRecorderListener onRecorderListener;
    private final M4AAudioRecorder m4AAudioRecorder = M4AAudioRecorder.getInstance();


    M4AAudioRecordThread(File outputStream, OnRecorderListener onRecorderListener) throws IOException {
        this.onRecorderListener = onRecorderListener;
        m4AAudioRecorder.setRecorderCallback(this);
        try {
            int sampleRate = 16000;
            int bitrate = 96000;
            int channelCount = 1;
            m4AAudioRecorder.prepare(outputStream.getAbsolutePath(), channelCount, sampleRate, bitrate);
        } catch (Exception e) {
            Log.w(TAG, e);
            throw new IOException(e);
        }
    }

    @Override
    public void run() {
        if (onRecorderListener != null) {
            Log.d(TAG, "onRecorderStarted");
            onRecorderListener.onRecorderStarted();
        }
        try {
            while (!Thread.interrupted()) {
            }
        } finally {
            m4AAudioRecorder.stopRecording();
        }
    }

    @Override
    public void onPrepareRecord() {
        Timber.tag("Recording").e("onPrepareRecord");
        m4AAudioRecorder.startRecording();
    }

    @Override
    public void onStartRecord(File output) {
        Timber.tag("Recording").e("onStartRecord");
        onRecorderListener.onRecorderStarted();
    }

    @Override
    public void onPauseRecord() {
        Timber.tag("Recording").e("onPauseRecord");
    }

    @Override
    public void onRecordProgress(long mills, int amp) {
        Timber.tag("Recording").e("onRecordProgress");
    }

    @Override
    public void onStopRecord(File output) {
        Timber.tag("Recording").e("onStopRecord");
        onRecorderListener.onRecordingComplete(output);
    }

    @Override
    public void onError(AppException appException) {
        Timber.tag("Recording").e("onError");
        onRecorderListener.onRecorderFailed(appException);
    }

    interface OnRecorderListener {
        void onRecorderFailed(AppException exception);

        void onRecorderStarted();

        void onRecordingComplete(File output);

    }
}
