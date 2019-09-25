package com.joshtalks.joshskills.core.service.downloader;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.core.AppObjectController;
import com.joshtalks.joshskills.ui.chat.ConversationActivity;


public class DownloadNotificationUtil {

    private static final @StringRes
    int NULL_STRING_ID = 0;
    public static final String CHANNEL_ID = "download_channel";
    private static final int smallIcon = R.mipmap.ic_launcher;

    private DownloadNotificationUtil() {}

    public static Notification buildProgressNotification(Context context, DownloadManager.TaskState[] taskStates) {
        float totalPercentage = 0;
        int downloadTaskCount = 0;
        boolean allDownloadPercentagesUnknown = true;
        boolean haveDownloadedBytes = false;
        boolean haveDownloadTasks = false;
        boolean haveRemoveTasks = false;
        for (DownloadManager.TaskState taskState : taskStates) {
            if (taskState.state != DownloadManager.TaskState.STATE_STARTED
                    && taskState.state != DownloadManager.TaskState.STATE_COMPLETED) {
                continue;
            }
            if (taskState.action.isRemoveAction) {
                haveRemoveTasks = true;
                continue;
            }
            haveDownloadTasks = true;
            if (taskState.downloadPercentage != C.PERCENTAGE_UNSET) {
                allDownloadPercentagesUnknown = false;
                totalPercentage += taskState.downloadPercentage;
            }
            haveDownloadedBytes |= taskState.downloadedBytes > 0;
            downloadTaskCount++;
        }

        int titleStringId =
                haveDownloadTasks
                        ? R.string.exo_download_downloading
                        : (haveRemoveTasks ? R.string.exo_download_removing : R.string.incomplete_downloads);

        String title = AppObjectController.getJoshApplication().getString(titleStringId);


        int progress = 0;
        boolean indeterminate = true;
        if (haveDownloadTasks) {
            progress = (int) (totalPercentage / downloadTaskCount);
            indeterminate = allDownloadPercentagesUnknown && haveDownloadedBytes;
        }


        String message = taskStates.length + " Video" + ((taskStates.length) > 0 ? "s" : "");

        NotificationCompat.Builder notificationBuilder =
                newNotificationBuilder(
                        context, smallIcon, CHANNEL_ID, getIntent(), message, title + " " + Math.round(progress) + " %");

        notificationBuilder.setProgress(/* max= */ 100, progress, indeterminate);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setShowWhen(false);
        return notificationBuilder.build();
    }

    public static Notification buildDownloadCompletedNotification(Context context) {
        return newNotificationBuilder(
                context, smallIcon, CHANNEL_ID, getIntent(), "Download Completed", "")
                .build();
    }

    public static Notification buildDownloadFailedNotification(Context context) {

        @StringRes int titleStringId = R.string.exo_download_failed;
        return newNotificationBuilder(
                context, smallIcon, CHANNEL_ID, getIntent(), "Download Failed","")
                .build();

    }

    private static NotificationCompat.Builder newNotificationBuilder(
            Context context,
            @DrawableRes int smallIcon,
            String channelId,
            @Nullable PendingIntent contentIntent,
            @Nullable String message,
            String title) {
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, channelId).setSmallIcon(smallIcon);

        notificationBuilder.setContentTitle(title);

        if (contentIntent != null) {
            notificationBuilder.setContentIntent(contentIntent);
        }
        if (message != null) {
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        }
        return notificationBuilder;
    }

    private static PendingIntent getIntent() {
        Intent intent = new Intent(AppObjectController.getJoshApplication(), ConversationActivity.class);
        PendingIntent pi = PendingIntent.getActivity(AppObjectController.getJoshApplication(), 0, intent, 0);
        return pi;
    }

}
