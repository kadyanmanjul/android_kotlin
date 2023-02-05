package com.joshtalks.joshskills.premium.core.countdowntimer;

public interface CountDownTimerListener {
    void onTimerTick(long timeRemaining);

    void onTimerFinish();
}
