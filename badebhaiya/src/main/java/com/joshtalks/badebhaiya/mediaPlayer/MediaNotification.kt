package com.joshtalks.badebhaiya.mediaPlayer

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.joshtalks.badebhaiya.R

class MediaNotification {
    val CHANNEL_ID="Media_Channel"
    val CHANNEL_PLAY="channel_play"
    val ACTION_PREVIOUS=" action_previous"
    val CHANNEL_NEXT="channel_next"

    val notification=Notification()

    fun mediaNotification(context: Context, speakerData: MediaData){

        val contentView = RemoteViews(context.packageName, R.layout.media_notification)
        contentView.setImageViewResource(R.id.notibackward,R.drawable.ic_backward_icon)
        contentView.setImageViewResource(R.id.notiforward, R.drawable.ic_forward_icon)
        contentView.setTextViewText(R.id.notiftitle, "Room Name")
        contentView.setTextViewText(R.id.notiftext, "Moderator Name")


//        setListeners(contentView) //look at step 3


        notification.contentView = contentView

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManagerCompat=NotificationManagerCompat.from(context)
            val mediaSessionCompat= MediaSessionCompat(context,"FOR MEDIA")

            val icon=BitmapFactory.decodeResource(context.resources,R.drawable.ic_status_bar_notification)

            lateinit var pendingIntentBackward:PendingIntent
            lateinit var pendingIntentForward: PendingIntent
            var drawBackward=R.drawable.ic_backward_icon
            var drawForward=R.drawable.ic_forward_icon
//            var intentPending=Intent(context,NotificationActionService.class)
//            pendingIntentBackward=PendingIntent.getBroadcast(context,0,intentBackward,PendingIntent.FLAG_UPDATE_CURRENT)



            val notification=NotificationCompat.Builder(context,CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_status_bar_notification)
                .setContentTitle(speakerData.roomName)
                .setContentText(speakerData.Speaker_data?.name)
                .setLargeIcon(icon)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notification.contentView=contentView

            notificationManagerCompat.notify(1,notification)



        }

    }
}