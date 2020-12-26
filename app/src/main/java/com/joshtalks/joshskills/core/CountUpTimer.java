package com.joshtalks.joshskills.core;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;

public class CountUpTimer {

    public static final String TAG = "CountUpTimer";
    private static final int MSG = 1;
    // Tick listener
    int interval;
    TickListener tickListener;
    Handler tickHandler;
    // The timestamp when this timer is initially started, in milliseconds
    private long startTimestamp;
    // The total time that the timer is paused. For example clock starts at 0,
    // pauses between 0 and 10, then then 15 and 20. So delay time is 15
    // at timestamp 40, the passed time is current - start - delay = 40 - 0 - 15 = 25
    // See the getTime() method
    private long delayTime;
    private long lastPauseTimestamp;
    // Whether it is running or not
    private boolean isRunning;
    Runnable tickSelector = new Runnable() {
        @Override
        public void run() {
            if (tickListener != null && isRunning) {
                tickListener.onTick(getTime());
                startTicking();
            }
        }
    };
    // An array to store lap timestamps
    private final ArrayList<Integer> lapTimestamps;


    /**
     * Create a new counting up timer. Will start immediately
     */
    public CountUpTimer(boolean startWhenCreated) {
        delayTime = 0;
        isRunning = startWhenCreated;
        lapTimestamps = new ArrayList<>();
        reset();
    }

    /**
     * Reset the timer, also clears all laps information. Running status will not affected
     */
    public void reset() {
        lastPauseTimestamp = 0;
        startTimestamp = SystemClock.elapsedRealtime();
        delayTime = 0;
        lastPauseTimestamp = startTimestamp;
        lapTimestamps.clear();
    }

    /**
     * Helper to parse the milliseconds to human-readable time
     *
     * @return the time in format h:mm:ss.SSS, for example 0:28:14.019
     */
    public static String getHumanFriendlyTime(int millis) {
        int currentTimeSeconds = millis / 1000;

        int hours = currentTimeSeconds / 3600;
        int minutes = (currentTimeSeconds % 3600) / 60;
        int seconds = currentTimeSeconds % 60;
        int milliseconds = millis % 1000;
        return String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
    }

    /**
     * Create a new lap
     */
    public void lap() {
        lapTimestamps.add(getTime());
    }

    /**
     * Create a new lap
     */
    public void lap(long lastPauseTimestamp) {
        this.lastPauseTimestamp = lastPauseTimestamp;
        lapTimestamps.add(getTime());
    }

    /**
     * Get the current time of this timer
     *
     * @return current time of this timer in milliseconds
     */
    public int getTime() {
        if (isRunning) return (int) (SystemClock.elapsedRealtime() - startTimestamp - delayTime);
        else return (int) (lastPauseTimestamp - startTimestamp - delayTime);
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Pause the timer
     */
    public void pause() {
        if (isRunning) {
            lastPauseTimestamp = SystemClock.elapsedRealtime();
            isRunning = false;
            stopTicking();
        }
    }

    /**
     * Resume the timer
     */
    public void resume() {
        if (!isRunning) {
            long currentTime = SystemClock.elapsedRealtime();
            delayTime += currentTime - lastPauseTimestamp;
            isRunning = true;
            startTicking();
        }
    }

    /**
     * Toggle the running state of this timer
     *
     * @return is the timer running after toggling?
     */
    public boolean toggleRunning() {
        if (isRunning) pause();
        else resume();

        return isRunning;
    }

    void stopTicking() {
        if (tickHandler != null) tickHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Get the duration of a specified lap
     *
     * @param position lap number, starts at 0
     * @return the duration of the lap
     */
    public int getLapDuration(int position) {
        if (position == 0) return lapTimestamps.get(0);
        else return lapTimestamps.get(position) - lapTimestamps.get(position - 1);
    }

    /**
     * Get the begin timestamp of a specified lap, when the lap was started
     *
     * @param position lap number, starts at 0
     * @return the begin timestamp of the lap
     */
    public int getLapTimestamp(int position) {
        return lapTimestamps.get(position);
    }

    /**
     * @return The number of laps
     */
    public int getLapCount() {
        return lapTimestamps.size();
    }

    /**
     * Parse the current time of this timer to human-readable time
     *
     * @return human-readable time in format h:mm:ss.SSS
     */
    public String getHumanFriendlyTime() {
        return getHumanFriendlyTime(getTime());
    }

    void startTicking() {
        if (tickHandler == null) tickHandler = new Handler();
        tickHandler.removeCallbacksAndMessages(null);

        int time = getTime();
        if (interval == 0) {
            interval = 1;
        }
        int remainingTimeInInterval = 0;

        try {
            remainingTimeInInterval = interval - time % interval;
        } catch (Exception ex) {

        }

        tickHandler.postDelayed(tickSelector, remainingTimeInInterval);
    }

    public CountUpTimer setTickListener(int interval, final TickListener tickListener) {
        this.interval = interval;
        this.tickListener = tickListener;

        if (tickListener == null) {
            stopTicking();
        } else {
            startTicking();
        }

        return this;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(TAG, "finalize");
    }


    public interface TickListener {
        void onTick(int milliseconds);
    }

}
