package com.joshtalks.joshskills.common.core.countdowntimer;

public interface CountDownTimerListener {
    void onTimerTick(long timeRemaining);

    void onTimerFinish();
}
