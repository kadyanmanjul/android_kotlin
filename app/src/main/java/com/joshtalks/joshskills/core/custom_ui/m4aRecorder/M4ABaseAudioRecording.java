package com.joshtalks.joshskills.core.custom_ui.m4aRecorder;

import android.util.Log;

import com.joshtalks.joshskills.core.custom_ui.recorder.OnAudioRecordListener;
import com.joshtalks.joshskills.core.custom_ui.recorder.RecordingItem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import timber.log.Timber;

public class M4ABaseAudioRecording {

    public static final int FILE_NULL = 3;
    private static final String TAG = "AudioRecording";
    private static final int IO_ERROR = 1;
    private static final int RECORDER_ERROR = 2;
    private File file;
    private OnAudioRecordListener onAudioRecordListener;
    private long mStartingTimeMillis = 0;
    private Thread mRecordingThread;

    public void setOnAudioRecordListener(OnAudioRecordListener onAudioRecordListener) {
        this.onAudioRecordListener = onAudioRecordListener;
    }

    public void setFile(String filePath) {
        this.file = new File(filePath);
    }

    // Call this method from Activity onStartButton Click to start recording
    public synchronized void startRecording() {
        if (file == null) {
            onAudioRecordListener.onError(FILE_NULL);
            return;
        }
        mStartingTimeMillis = System.currentTimeMillis();
        try {
            if (mRecordingThread != null) stopRecording(true);
            mRecordingThread = new Thread(new M4AAudioRecordThread(file, new M4AAudioRecordThread.OnRecorderListener() {

                @Override
                public void onRecorderFailed(AppException exception) {
                    onAudioRecordListener.onError(RECORDER_ERROR);
                    stopRecording(true);
                }

                @Override
                public void onRecorderStarted() {
                    onAudioRecordListener.onRecordingStarted();
                }

                @Override
                public void onRecordingComplete(File output) {
                    long mElapsedMillis = (System.currentTimeMillis() - mStartingTimeMillis);

                    RecordingItem recordingItem = new RecordingItem();
                    recordingItem.setFilePath(file.getAbsolutePath());
                    recordingItem.setName(file.getName());
                    recordingItem.setLength((int) mElapsedMillis);
                    recordingItem.setTime(System.currentTimeMillis());
                    onAudioRecordListener.onRecordFinished(recordingItem);
                }
            }));
            mRecordingThread.setName("AudioRecordingThread");
            mRecordingThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Call this method from Activity onStopButton Click to stop recording
    public synchronized void stopRecording(boolean cancel) {
        Log.d(TAG, "Recording stopped ");
        if (mRecordingThread != null) {
            mRecordingThread.interrupt();
            mRecordingThread = null;
        }
    }

    private OutputStream outputStream(File file) {
        if (file == null) {
            throw new RuntimeException("file is null !");
        }
        OutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(
                    "could not build OutputStream from" + " this file " + file.getName(), e);
        }
        return outputStream;
    }

    private void deleteFile() {
        if (file != null && file.exists())
            Timber.d(String.format("deleting file success %b ", file.delete()));
    }
}
