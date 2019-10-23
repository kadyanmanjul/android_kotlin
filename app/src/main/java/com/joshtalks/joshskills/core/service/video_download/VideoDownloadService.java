package com.joshtalks.joshskills.core.service.video_download;

import android.app.Notification;
import android.util.Log;


import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ext.workmanager.WorkManagerScheduler;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.scheduler.PlatformScheduler;
import com.google.android.exoplayer2.scheduler.Scheduler;
import com.google.android.exoplayer2.ui.DownloadNotificationHelper;
import com.google.android.exoplayer2.util.NotificationUtil;
import com.google.android.exoplayer2.util.Util;
import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.messaging.RxBus2;
import com.joshtalks.joshskills.repository.local.DatabaseUtils;
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS;
import com.joshtalks.joshskills.repository.local.eventbus.DownloadMediaEventBus;
import com.joshtalks.joshskills.repository.local.eventbus.MediaProgressEventBus;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class VideoDownloadService extends DownloadService {

    private static final String CHANNEL_ID = "download_channel";
    private static final int JOB_ID = 1;
    private static final int FOREGROUND_NOTIFICATION_ID = 1;

    private static int nextNotificationId = FOREGROUND_NOTIFICATION_ID + 1;

    private DownloadNotificationHelper notificationHelper;

    public VideoDownloadService() {
        super(
                FOREGROUND_NOTIFICATION_ID,
                DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
                CHANNEL_ID,
                R.string.exo_download_notification_channel_name);
        nextNotificationId = FOREGROUND_NOTIFICATION_ID + 1;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationHelper = new DownloadNotificationHelper(this, CHANNEL_ID);
    }

    @Override
    protected DownloadManager getDownloadManager() {
        return VideoDownloadController.getInstance().getDownloadManager();
    }

    @Override
    protected Scheduler getScheduler() {
        return Util.SDK_INT >= 21 ? new PlatformScheduler(this, JOB_ID) : null;
    }

    @Override
    protected Notification getForegroundNotification(List<Download> downloads) {
        showDownloadProgress(downloads);
        return notificationHelper.buildProgressNotification(
                R.drawable.ic_download, null, "", downloads);
    }

    @Override
    protected void onDownloadChanged(Download download) {
        Log.i("onDownloadChanged", "onDownloadChanged " + download.state+"  "+download.getPercentDownloaded()+"  "+download.getBytesDownloaded());
        DOWNLOAD_STATUS downloadStatus = DOWNLOAD_STATUS.NOT_START;

        if (download.state == Download.STATE_DOWNLOADING || download.state == Download.STATE_QUEUED) {
            videoNotUploadFlagUpdate();
            downloadStatus = DOWNLOAD_STATUS.DOWNLOADING;
        } else if (download.state == Download.STATE_COMPLETED) {
            downloadStatus = DOWNLOAD_STATUS.DOWNLOADED;
        } else if (download.state == Download.STATE_REMOVING) {
            downloadStatus = DOWNLOAD_STATUS.FAILED;
        } else if (download.state == Download.STATE_FAILED) {
            downloadStatus = DOWNLOAD_STATUS.FAILED;
        } else if (download.state == Download.STATE_STOPPED) {
            downloadStatus = DOWNLOAD_STATUS.FAILED;
        }
        DatabaseUtils.updateVideoDownload(Util.fromUtf8Bytes(download.request.data), downloadStatus);
        Notification notification;
        if (download.state == Download.STATE_COMPLETED) {
            notification = notificationHelper.buildDownloadCompletedNotification(R.mipmap.ic_launcher, null, "Download completed");
        } else if (download.state == Download.STATE_FAILED) {
            notification = notificationHelper.buildDownloadFailedNotification(R.mipmap.ic_launcher, null, "Download failed");
            RxBus2.publish(new MediaProgressEventBus(Download.STATE_FAILED,Util.fromUtf8Bytes(download.request.data), 0));

        } else {
            return;
        }
        NotificationUtil.setNotification(this, nextNotificationId++, notification);

    }

    void videoNotUploadFlagUpdate() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                DatabaseUtils.updateAllVideoStatusWhichIsDownloading();
            }
        }, 0, 30 * 60 * 1000);

    }

    void showDownloadProgress(List<Download> downloads){
        try {
            float totalPercentage = 0;
            int downloadTaskCount = 0;
            boolean allDownloadPercentagesUnknown = true;
            boolean haveDownloadedBytes = false;
            boolean haveDownloadTasks = false;
            boolean haveRemoveTasks = false;
            Download download = null;
            for (int i = 0; i < downloads.size(); i++) {
                 download = downloads.get(i);
                if (download.state == Download.STATE_REMOVING) {
                    haveRemoveTasks = true;
                    continue;
                }
                if (download.state != Download.STATE_RESTARTING
                        && download.state != Download.STATE_DOWNLOADING) {
                    continue;
                }
                haveDownloadTasks = true;
                float downloadPercentage = download.getPercentDownloaded();
                if (downloadPercentage != C.PERCENTAGE_UNSET) {
                    allDownloadPercentagesUnknown = false;
                    totalPercentage += downloadPercentage;
                }
                haveDownloadedBytes |= download.getBytesDownloaded() > 0;
                downloadTaskCount++;
            }


            if (haveDownloadTasks) {
                int progress = (int) (totalPercentage / downloadTaskCount);
                RxBus2.publish(new MediaProgressEventBus(Download.STATE_DOWNLOADING,Util.fromUtf8Bytes(download.request.data), progress));
            }
        }catch (Exception e){}
    }
}
