package com.joshtalks.badebhaiya.recordedRoomPlayer

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.util.NotificationUtil
import com.joshtalks.badebhaiya.R


class MusicNotificationManager(
    private val context: Context,
    sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener,
    private val newSongCallback: () -> Unit
) {

    private var currentBitmap: Bitmap? = null
    private val notificationManager: PlayerNotificationManager

    companion object {
        const val NOTIFICATION_ID = 331
    }

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)
        notificationManager =
            PlayerNotificationManager.Builder(context, NOTIFICATION_ID, "recorded_room_streaming")
                .setChannelNameResourceId(R.string.recorded_room)
                .setChannelDescriptionResourceId(R.string.recorded_room_channel_description)
                .setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
                .setNotificationListener(notificationListener)
                .setPlayActionIconResourceId(R.drawable.ic_play_plain_white)
                .setPauseActionIconResourceId(R.drawable.ic_pause_plain_white)
                .setFastForwardActionIconResourceId(R.drawable.ic_fast_forward)
                .setRewindActionIconResourceId(R.drawable.ic_rewind)
                .setChannelImportance(NotificationUtil.IMPORTANCE_HIGH)
                .setSmallIconResourceId(R.drawable.ic_status_bar_notification)
                .build()

        notificationManager.setColorized(true)
        notificationManager.setColor(context.getColor(R.color.player_blue))

        notificationManager.apply {
            setMediaSessionToken(sessionToken)
            setUsePreviousAction(false)
//            setColorized(true)
//            setColor(Color.RED)
            setUseFastForwardActionInCompactView(true)
            setUseRewindActionInCompactView(true)
        }

    }

    fun showNotification(player: Player) {
        notificationManager.setPlayer(player)
    }

    private inner class DescriptionAdapter(
        private val mediaController: MediaControllerCompat
    ) : PlayerNotificationManager.MediaDescriptionAdapter {

        override fun getCurrentContentTitle(player: Player): CharSequence {
            newSongCallback()
            return mediaController.metadata.description.title.toString()
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            return mediaController.sessionActivity
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            return mediaController.metadata.description.subtitle.toString()
        }


        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
                Glide.with(context).asBitmap()
                    .load(mediaController.metadata.description.iconUri)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            currentBitmap = resource
                            callback.onBitmap(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) = Unit
                    })
                return currentBitmap
        }
    }
}
