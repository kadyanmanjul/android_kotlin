package com.joshtalks.joshskills.ui.groupchat.listeners;

public interface RecordListener {
    void onStart();

    void onCancel();

    void onFinish(long time);

    void onLessTime();
}