package com.joshtalks.joshskills.core.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.joshtalks.joshskills.R;
import com.joshtalks.joshskills.ui.practise.PractiseSubmitActivity;

import static com.joshtalks.joshskills.ui.chat.ConversationActivityKt.CHAT_ROOM_OBJECT;


public class MusicNotificationManager {

    public static final int NOTIFICATION_ID = 101;
    static final String PLAY_PAUSE_ACTION = "com.joshtalks.joshskills.PLAYPAUSE";
    static final String NEXT_ACTION = "com.joshtalks.joshskills.NEXT";
    static final String PREV_ACTION = "com.joshtalks.joshskills.PREV";
    static final String STOP_ACTION = "com.joshtalks.joshskills.STOP";

    private final String CHANNEL_ID = "com.joshtalks.joshskills.CHANNEL_ID";
    private final int REQUEST_CODE = 100;
    private final NotificationManager mNotificationManager;
    private final MusicService mMusicService;
    private NotificationCompat.Builder mNotificationBuilder;
    private int mAccent;

    MusicNotificationManager(@NonNull final MusicService musicService) {
        mMusicService = musicService;
        mNotificationManager = (NotificationManager) mMusicService.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void setAccentColor(final int color) {
        mAccent = R.color.colorAccent;
    }

    public final NotificationManager getNotificationManager() {
        return mNotificationManager;
    }

    public final NotificationCompat.Builder getNotificationBuilder() {
        return mNotificationBuilder;
    }

    private PendingIntent playerAction(@NonNull final String action) {

        final Intent pauseIntent = new Intent();
        pauseIntent.setAction(action);

        return PendingIntent.getBroadcast(mMusicService, REQUEST_CODE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public Notification createNotification() {

        mNotificationBuilder = new NotificationCompat.Builder(mMusicService, CHANNEL_ID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        final Intent openPlayerIntent = new Intent(mMusicService, PractiseSubmitActivity.class);
      //  openPlayerIntent.putExtra(CHAT_ROOM_OBJECT, mMusicService.getMediaPlayerHolder().getConversation());
        openPlayerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent contentIntent = PendingIntent.getActivity(mMusicService, REQUEST_CODE, openPlayerIntent, 0);

        final String spanned = "Audio";
        final String contentText = "Audio playing";

        mNotificationBuilder
                .setShowWhen(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(getLargeIcon())
                .setColor(mAccent)
                .setContentTitle(spanned)
                .setContentText(contentText)
                //.setContentIntent(contentIntent)
                // .addAction(notificationAction(PLAY_PAUSE_ACTION))
                //.addAction(notificationAction(STOP_ACTION))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        // mNotificationBuilder.setStyle(new MediaStyle().setShowActionsInCompactView(0, 1));

        Notification notification = mNotificationBuilder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;


        return notification;
    }

    @NonNull
    private NotificationCompat.Action notificationAction(@NonNull final String action) {

        int icon;

        switch (action) {
            default:
          /*  case PREV_ACTION:
                icon = R.drawable.ic_skip_previous_notification;
                break;
          */
            case PLAY_PAUSE_ACTION:
                icon = mMusicService.getMediaPlayerHolder().getState() != PlaybackInfoListener.State.PAUSED ? R.drawable.ic_pause_notification : R.drawable.ic_play_notification;
                break;
            case STOP_ACTION:
                icon = R.drawable.ic_stop_notification;
                break;
        }
        return new NotificationCompat.Action.Builder(icon, action, playerAction(action)).build();
    }

    @RequiresApi(26)
    private void createNotificationChannel() {

        if (mNotificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            final NotificationChannel notificationChannel =
                    new NotificationChannel(CHANNEL_ID,
                            mMusicService.getString(R.string.app_name),
                            NotificationManager.IMPORTANCE_LOW);

            notificationChannel.setDescription(
                    mMusicService.getString(R.string.app_name));

            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setShowBadge(false);

            mNotificationManager.createNotificationChannel(notificationChannel);
        }
    }

    private Bitmap getLargeIcon() {
        final VectorDrawable vectorDrawable = (VectorDrawable) mMusicService.getDrawable(R.drawable.ic_josh_course);
        final int largeIconSize = mMusicService.getResources().getDimensionPixelSize(R.dimen.notification_large_dim);
        final Bitmap bitmap = Bitmap.createBitmap(largeIconSize, largeIconSize, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);

        if (vectorDrawable != null) {
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.setTint(mAccent);
            vectorDrawable.setAlpha(100);
            vectorDrawable.draw(canvas);
        }

        return bitmap;
    }
}
