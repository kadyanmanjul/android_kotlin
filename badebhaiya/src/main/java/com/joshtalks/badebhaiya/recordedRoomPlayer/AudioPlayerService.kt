package com.joshtalks.badebhaiya.recordedRoomPlayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AudioPlayerService : MediaBrowserServiceCompat() {

    companion object {
        private const val SERVICE_TAG = "MusicService"
        const val MEDIA_ROOT_ID = "root_id"
        const val NETWORK_ERROR = "NETWORK_ERROR"
        const val UPDATE_PLAYER_POSITION_INTERVAL = 100L


        var curSongDuration = 0L
            private set

        var playingRoomId: Int? = 0

        var actualSong = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "test")
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "12345")
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "test")
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "test")
            .putString(
                MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                "https://media.istockphoto.com/vectors/music-note-icon-vector-illustration-vector-id1175435360?k=20&m=1175435360&s=612x612&w=0&h=1yoTgUwobvdFlNxUQtB7_NnWOUD83XOMZHvxUzkOJJs="
            )
            .putString(
                MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                "https://s3.ap-south-1.amazonaws.com/www.static.skills.com/bb-app/Abhijit_Chavda-_Aliens_ISRO_Aur_C_(getmp3.pro).mp3"
            )
            .putString(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                "https://www.smartpassiveincome.com/wp-content/uploads/2021/12/Learn-How-to-Podcast.png"
            )
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "test")
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, "test")
            .build()!!

        fun setAudio(recordedRoom: RoomListResponseItem) {
            playingRoomId = recordedRoom.roomId
            actualSong = MediaMetadataCompat.Builder()
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    recordedRoom.speakersData?.fullName
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                    recordedRoom.roomId.toString()
                )
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, recordedRoom.topic)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, recordedRoom.topic)
                .putString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                    recordedRoom.speakersData?.photoUrl
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                    recordedRoom.recordings!![0].url
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                    recordedRoom.speakersData?.photoUrl
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                    recordedRoom.speakersData?.fullName
                )
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, recordedRoom.topic)
                .build()!!
        }

    }


    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer: ExoPlayer

    @Inject
    lateinit var musicServiceConnection: MusicServiceConnection

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var musicNotificationManager: MusicNotificationManager

    var isForegroundService = false

    private var curPlayingSong: MediaMetadataCompat? = null
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var musicPlayerEventListener: MusicPlayerEventListener
    private var isPlayerInitialized = false


    override fun onCreate() {
        super.onCreate()
        Timber.tag("audioservice").d("AUDIO SERVICE IS ON CREATE")


        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, 0)
        }

        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ) {
            curSongDuration = exoPlayer.duration
        }

        val musicPlaybackPreparer = MusicPlaybackPreparer() {
            curPlayingSong = it
            preparePlayer(
                true
            )
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)

        musicPlayerEventListener = MusicPlayerEventListener(this, serviceScope)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
        collectData()

    }

    private fun collectData() {
        serviceScope.launch {
            musicServiceConnection.playBackSpeed.collectLatest {
                changePlaybackSpeed(it)
            }
        }
    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return actualSong.description
        }
    }

    fun changePlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
    }


    private fun preparePlayer(
        playNow: Boolean
    ) {
//        val curSongIndex = if(curPlayingSong == null) 0 else songs.indexOf(itemToPlay)
//        exoPlayer.prepare(asMediaSource(dataSourceFactory))
        exoPlayer.setMediaSource(asMediaSource(dataSourceFactory))
        exoPlayer.prepare()
        exoPlayer.seekTo(0L)
        exoPlayer.playWhenReady = playNow
    }

    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory): ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(
                MediaItem.fromUri(
                    actualSong.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI).toUri()
                )
            )
        concatenatingMediaSource.addMediaSource(mediaSource)

        return concatenatingMediaSource
    }

    fun asMediaItems(): MutableList<MediaBrowserCompat.MediaItem> {
        val desc = MediaDescriptionCompat.Builder()
            .setMediaUri(actualSong.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI).toUri())
            .setTitle(actualSong.description.title)
            .setSubtitle(actualSong.description.subtitle)
            .setMediaId(actualSong.description.mediaId)
            .setIconUri(actualSong.description.iconUri)
            .build()
        return mutableListOf(
            MediaBrowserCompat.MediaItem(
                desc,
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        )
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Timber.tag("audioservice").d("AUDIO SERVICE IS DESTROYED")
        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when (parentId) {
            MEDIA_ROOT_ID -> {
//                val resultsSent = firebaseMusicSource.whenReady { isInitialized ->
//                    if(isInitialized) {
                result.sendResult(asMediaItems())
                if (!isPlayerInitialized
//                            && firebaseMusicSource.songs.isNotEmpty()
                ) {
                    preparePlayer(true)
                    isPlayerInitialized = true
                }
            }
//            else {
//                        mediaSession.sendSessionEvent("NETWORK_ERROR", null)
//                        result.sendResult(null)
//                    }
        }
//                if(!resultsSent) {
//                    result.detach()
//                }
    }
//        }
//    }
}