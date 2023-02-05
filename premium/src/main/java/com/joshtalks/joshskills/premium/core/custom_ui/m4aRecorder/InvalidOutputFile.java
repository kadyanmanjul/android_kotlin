
package com.joshtalks.joshskills.premium.core.custom_ui.m4aRecorder;

public class InvalidOutputFile extends AppException {

    @Override
    public int getType() {
        return AppException.INVALID_OUTPUT_FILE;
    }
}
