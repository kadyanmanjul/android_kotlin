package com.joshtalks.joshskills.premium.core.custom_ui.m4aRecorder;

public class RecorderInitException extends AppException {
    @Override
    public int getType() {
        return AppException.RECORDER_INIT_EXCEPTION;
    }
}
