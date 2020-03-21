package com.joshtalks.filelogger;


public interface Loggable {

    void v(String tag, String log);
    void d(String tag, String log);
    void i(String tag, String log);
    void w(String tag, String log);
    void e(String tag, String log);
    void e(String tag, String log, Throwable tr);
}
