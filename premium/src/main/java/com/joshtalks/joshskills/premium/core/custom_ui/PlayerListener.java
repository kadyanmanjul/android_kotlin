package com.joshtalks.joshskills.premium.core.custom_ui;

public interface PlayerListener {

    void onPlayerReady();

    void onBufferingUpdated(boolean isBuffering);

    void onCurrentTimeUpdated(long time);

    void onPlayerReleased();

    void onPositionDiscontinuity(int reason, long lastPos);

}
