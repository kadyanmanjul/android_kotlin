package com.joshtalks.joshskills.core.custom_ui.recorder;

public interface OnAudioRecordListener {

    default void onRecordFinished(RecordingItem recordingItem) {
    }

    default void onError(int errorCode) {
    }

    default void onRecordingStarted() {
    }

}
