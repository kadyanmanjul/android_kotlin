package com.joshtalks.joshskills.core.countdowntimer;

public interface CountDownTimerListener {
    void onTimerTick(long timeRemaining);

    void onTimerFinish();
}
