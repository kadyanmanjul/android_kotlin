package com.joshtalks.joshskills.core.inapp_update;

public interface Constants {

    enum UpdateMode {
        FLEXIBLE,
        IMMEDIATE
    }

    int UPDATE_ERROR_START_APP_UPDATE_FLEXIBLE = 100;
    int UPDATE_ERROR_START_APP_UPDATE_IMMEDIATE = 101;

}
