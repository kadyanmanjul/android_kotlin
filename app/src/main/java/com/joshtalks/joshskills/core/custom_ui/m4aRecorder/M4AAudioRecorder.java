package com.joshtalks.joshskills.core.custom_ui.m4aRecorder;


import android.media.MediaRecorder;
import android.os.Build;

import java.io.File;
import java.io.IOException;

import timber.log.Timber;

public class M4AAudioRecorder implements RecorderContract.Recorder {

    public static final int SHORT_RECORD_DP_PER_SECOND = 25;
    public final static int VISUALIZATION_INTERVAL = 1000 / SHORT_RECORD_DP_PER_SECOND;
    private MediaRecorder recorder = null;
    private File recordFile = null;
    private boolean isPrepared = false;
    private boolean isRecording = false;
    private boolean isPaused = false;
    //   private Timer timerProgress;
    private long progress = 0;
    private RecorderContract.RecorderCallback recorderCallback;

    private M4AAudioRecorder() {
    }

    public static M4AAudioRecorder getInstance() {
        return RecorderSingletonHolder.getSingleton();
    }

    @Override
    public void setRecorderCallback(RecorderContract.RecorderCallback callback) {
        this.recorderCallback = callback;
    }

    @Override
    public void prepare(String outputFile, int channelCount, int sampleRate, int bitrate) {
        recordFile = new File(outputFile);
        if (recordFile.exists() && recordFile.isFile()) {

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioChannels(channelCount);
            recorder.setAudioSamplingRate(sampleRate);
            recorder.setAudioEncodingBitRate(bitrate);
            recorder.setMaxDuration(-1); //Duration unlimited or use RECORD_MAX_DURATION
            recorder.setOutputFile(recordFile.getAbsolutePath());
            try {
                recorder.prepare();
                isPrepared = true;
                if (recorderCallback != null) {
                    recorderCallback.onPrepareRecord();
                }
            } catch (IOException | IllegalStateException e) {
                Timber.e(e, "prepare() failed");
                if (recorderCallback != null) {
                    recorderCallback.onError(new RecorderInitException());
                }
            }
        } else {
            if (recorderCallback != null) {
                recorderCallback.onError(new InvalidOutputFile());
            }
        }
    }

    @Override
    public void startRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isPaused) {
            try {
                recorder.resume();
                startRecordingTimer();
                if (recorderCallback != null) {
                    recorderCallback.onStartRecord(recordFile);
                }
                isPaused = false;
            } catch (IllegalStateException e) {
                Timber.e(e, "unpauseRecording() failed");
                if (recorderCallback != null) {
                    recorderCallback.onError(new RecorderInitException());
                }
            }
        } else {
            if (isPrepared) {
                try {
                    recorder.start();
                    isRecording = true;
                    startRecordingTimer();
                    if (recorderCallback != null) {
                        recorderCallback.onStartRecord(recordFile);
                    }
                } catch (RuntimeException e) {
                    Timber.e(e, "startRecording() failed");
                    if (recorderCallback != null) {
                        recorderCallback.onError(new RecorderInitException());
                    }
                }
            } else {
                Timber.e("Recorder is not prepared!!!");
            }
            isPaused = false;
        }
    }

    ///storage/emulated/0/Android/data/com.dimowner.audiorecorder.debug/files/Music/records/Record-3.m4a
    @Override
    public void pauseRecording() {
        if (isRecording) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    recorder.pause();
                    pauseRecordingTimer();
                    if (recorderCallback != null) {
                        recorderCallback.onPauseRecord();
                    }
                    isPaused = true;
                } catch (IllegalStateException e) {
                    Timber.e(e, "pauseRecording() failed");
                    if (recorderCallback != null) {
                        //TODO: Fix exception
                        recorderCallback.onError(new RecorderInitException());
                    }
                }
            } else {
                stopRecording();
            }
        }
    }

    @Override
    public void stopRecording() {
        try {
            if (isRecording) {
                stopRecordingTimer();
                try {
                    recorder.stop();
                } catch (RuntimeException e) {
                    Timber.e(e, "stopRecording() problems");
                }
                recorder.release();
                if (recorderCallback != null) {
                    recorderCallback.onStopRecord(recordFile);
                }
                recordFile = null;
                isPrepared = false;
                isRecording = false;
                isPaused = false;
                recorder = null;
            } else {
                Timber.e("Recording has already stopped or hasn't started");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRecordingTimer() {
        //   timerProgress = new Timer();
     /*   timerProgress .schedule(new TimerTask() {
            @Override
            public void run() {
                if (recorderCallback != null && recorder != null) {
                    try {
                        recorderCallback.onRecordProgress(progress, recorder.getMaxAmplitude());
                    } catch (IllegalStateException e) {
                        Timber.e(e);
                    }
                    progress += VISUALIZATION_INTERVAL;
                }
            }
        }, 0, VISUALIZATION_INTERVAL);*/
    }

    private void stopRecordingTimer() {
        //    timerProgress.cancel();
        //   timerProgress.purge();
        progress = 0;
    }

    private void pauseRecordingTimer() {
        //       timerProgress.cancel();
        //    timerProgress.purge();
    }

    @Override
    public boolean isRecording() {
        return isRecording;
    }

    @Override
    public boolean isPaused() {
        return isPaused;
    }

    private static class RecorderSingletonHolder {
        private static final M4AAudioRecorder singleton = new M4AAudioRecorder();

        public static M4AAudioRecorder getSingleton() {
            return RecorderSingletonHolder.singleton;
        }
    }
}

