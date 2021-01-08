package com.joshtalks.joshskills.core.custom_ui.m4aRecorder;

public class RecorderInitException extends AppException {
    @Override
    public int getType() {
        return AppException.RECORDER_INIT_EXCEPTION;
    }
}
