package com.joshtalks.recordview;

public interface OnRecordListener {
    void onStart();
    void onCancel();
    void onFinish(long recordTime);
    void onLessThanSecond();
}
