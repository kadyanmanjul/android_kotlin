package com.joshtalks.joshskills.core.custom_ui.recorder;

public interface OnAudioRecordListener {

    void onRecordFinished(RecordingItem recordingItem);

    void onError(int errorCode);

    void onRecordingStarted();

}
