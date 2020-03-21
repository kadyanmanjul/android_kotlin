package com.joshtalks.filelogger;


public interface FileFormatter {
    String formatLine(long timeInMillis, String level, String tag, String log);
    String formatFileName(long timeInMillis);
}
