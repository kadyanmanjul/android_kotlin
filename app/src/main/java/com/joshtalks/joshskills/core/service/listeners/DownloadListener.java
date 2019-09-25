package com.joshtalks.joshskills.core.service.listeners;

public interface DownloadListener {
    void onDownloadsChanged();
    void onDownloadStarted();
    void onDownloadFailed(String reason);
}
