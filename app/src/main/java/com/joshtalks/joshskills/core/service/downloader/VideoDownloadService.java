package com.joshtalks.joshskills.core.service.downloader;

import android.app.Notification;

import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.scheduler.PlatformScheduler;
import com.google.android.exoplayer2.util.NotificationUtil;
import com.google.android.exoplayer2.util.Util;
import com.joshtalks.joshskills.R;

import static com.joshtalks.joshskills.core.service.downloader.DownloadNotificationUtil.CHANNEL_ID;

public class VideoDownloadService extends DownloadService {

    private static final int JOB_ID = 1;
    private static final int FOREGROUND_NOTIFICATION_ID = 1;
    private static final String TAG = VideoDownloadService.class.getCanonicalName();

    public VideoDownloadService() {
        super(FOREGROUND_NOTIFICATION_ID,
                DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
                CHANNEL_ID,
                R.string.exo_download_notification_channel_name);
    }

    @Override
    protected DownloadManager getDownloadManager() {
        return MediaPlayerManager.getInstance().getDownloadManager();
    }

    @Override
    protected PlatformScheduler getScheduler() {
        return Util.SDK_INT >= 21 ? new PlatformScheduler(this, JOB_ID) : null;
    }

    @Override
    protected Notification getForegroundNotification(DownloadManager.TaskState[] taskStates) {

        for (DownloadManager.TaskState taskState : taskStates) {
           // PostCard card = PostCard.create(Util.fromUtf8Bytes(taskState.action.data));
            //EventBus.getDefault().post(new VideoDownloadProgressUpdate(card.getId(), taskState.downloadPercentage));
        }

        return DownloadNotificationUtil.buildProgressNotification(
                this,
                taskStates);

    }

    @Override
    protected void onTaskStateChanged(DownloadManager.TaskState taskState) {

        if (taskState.action.isRemoveAction) {
            return;
        }

        Notification notification = null;

        //PostCard card = PostCard.create(Util.fromUtf8Bytes(taskState.action.data));

        if (taskState.state == DownloadManager.TaskState.STATE_COMPLETED) {

            notification = DownloadNotificationUtil.buildDownloadCompletedNotification(this);

           // DownloadedPostsDao.getInstance().markAsCompleted(card);

        } else if (taskState.state == DownloadManager.TaskState.STATE_FAILED) {

            notification = DownloadNotificationUtil.buildDownloadFailedNotification(this);

        }

        int notificationId = FOREGROUND_NOTIFICATION_ID + 1 + taskState.taskId;
        NotificationUtil.setNotification(this, notificationId, notification);

    }

}
