package com.joshtalks.joshskills.dynamic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.Message
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.LogMessage
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.base.getVideoFilePath
import com.joshtalks.joshskills.base.saveVideoQ
import com.joshtalks.joshskills.constants.INCREASE_AUDIO_VOLUME
import com.joshtalks.joshskills.constants.VIDEO_AUDIO_MERGED_PATH
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.NotificationChannelNames
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val VIDEO_PATH = "VIDEO_PATH"
const val AUDIO_PATH = "AUDIO_PATH"
const val DURATION_FIRST: String = "first"

const val CHANNEL_ID = "VIDEO_AUDIO_PROCESSING"
const val NOTIFICATION_ID = 1681

class VideoMergeService : Service() {
    private var mNotificationManager: NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoPath = intent?.getStringExtra(VIDEO_PATH)
        val audioPath = intent?.getStringExtra(AUDIO_PATH)
        if (videoPath != null && audioPath != null) {
            startVideoMuxing(videoPath, audioPath)
            showNotification()
        }
        return START_STICKY
    }

    private fun showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = NotificationChannelNames.OTHERS.type
            val importance: Int = NotificationManager.IMPORTANCE_LOW
            val mChannel =
                NotificationChannel(CHANNEL_ID, name, importance)
            mNotificationManager?.createNotificationChannel(mChannel)
        }

        val lNotificationBuilder = NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setChannelId(CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Creating your Video...")
                .setSmallIcon(R.drawable.ic_status_bar_notification)
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)


        startForeground(NOTIFICATION_ID, lNotificationBuilder.build())
    }

    private fun hideNotification() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        stopForeground(true)
    }

    private fun startVideoMuxing(
        videoDownPath: String,
        audioPath: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isActive) {
                //TODO : shownotification for the crash fix
                var outputFile: String? = ""
                if (Build.VERSION.SDK_INT >= 29) {
                    outputFile = saveVideoQ(this@VideoMergeService, videoDownPath)
                } else {
                    outputFile = getVideoFilePath()
                }
                increaseAudioVolume(audioPath)
                if (outputFile != null) {
                    val extractedPath = extractAudioFromVideo(videoDownPath)
                    mergeAudioWithAudio(
                        audioPath,
                        extractedPath, videoDownPath, outputFile
                    )
                } else{
                    hideNotification()
                }
            } else{
                hideNotification()
            }
        }
    }

    private fun increaseAudioVolume(inputAudio: String) {
        val outputPath = getAudioFilePathMP3()
        val query = audioVolumeUpdate(inputAudio, volume = 10.0f, output = outputPath)
        CallBackOfQuery().callQuery(query, object : FFmpegCallBack {
            override fun process(logMessage: LogMessage) {
            }

            override fun success() {
                EventLiveData.value = Message().apply {
                    what = INCREASE_AUDIO_VOLUME
                    obj = outputPath
                }
            }

            override fun cancel() {
            }

            override fun failed() {
            }
        })
    }


    private fun getAudioFilePathMP3(): String {
        return getAndroidDownloadFolder()?.absolutePath + "/" + "JoshSkill-" + System.currentTimeMillis() + ".mp3"
    }

    fun getAndroidDownloadFolder(): File? {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }

    private fun getAudioFilePathAAC(): String {
        return getAndroidDownloadFolder()?.absolutePath + "/" + "JoshSkill-" + System.currentTimeMillis() + ".aac"
    }

    private fun audioVolumeUpdate(inputFile: String, volume: Float, output: String): Array<String> {
        val inputs: ArrayList<String> = ArrayList()
        inputs.apply {
            add("-i")
            add(inputFile)
            add("-af")
            add("volume=$volume")
            add("-preset")
            add("ultrafast")
            add(output)
        }
        return inputs.toArray(arrayOfNulls<String>(inputs.size))
    }


    private fun extractAudioFromVideo(videoPath: String): String {
        val outputAudioPath = getAudioFilePathAAC()
        val videoAudioExtractor: MediaExtractor = MediaExtractor()
        var videoAudioFormat: MediaFormat = MediaFormat()
        val sampleSize = 256 * 1024
        var sawEOS = false
        var frameCount = 0
        val offset = 100

        videoAudioExtractor.setDataSource(videoPath)
        for (i in 0 until videoAudioExtractor.trackCount) {
            videoAudioFormat = videoAudioExtractor.getTrackFormat(i)
            val mime = videoAudioFormat.getString(MediaFormat.KEY_MIME);
            if (mime!!.startsWith("audio/")) {
                videoAudioExtractor.selectTrack(i)
            }
        }

        val muxer: MediaMuxer = MediaMuxer(
            outputAudioPath,
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )

        val videoAudioTrack = muxer.addTrack(videoAudioFormat)
        val videoAudioBuffer: ByteBuffer = ByteBuffer.allocate(sampleSize)
        val videoAudioBufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
        videoAudioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        muxer.start()

        while (!sawEOS) {
            frameCount++

            videoAudioBufferInfo.offset = offset
            videoAudioBufferInfo.size = videoAudioExtractor.readSampleData(videoAudioBuffer, offset)

            if (videoAudioBufferInfo.size < 0) {
                sawEOS = true
                videoAudioBufferInfo.size = 0
            } else {
                videoAudioBufferInfo.presentationTimeUs = videoAudioExtractor.getSampleTime()
                videoAudioBufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                muxer.writeSampleData(videoAudioTrack, videoAudioBuffer, videoAudioBufferInfo)
                videoAudioExtractor.advance()
            }
        }

        muxer.stop()
        muxer.release()
        videoAudioExtractor.release()

        return outputAudioPath
    }


    private fun mergeAudioWithAudio(audio1: String, audio2: String, videoPath: String, output: String) {
        val outputPath = getAudioFilePathMP3()
        val pathsList = ArrayList<String>()

        pathsList.add(audio1)
        pathsList.add(audio2)

        val query = mergeAudios(pathsList, DURATION_FIRST, outputPath)

        CallBackOfQuery().callQuery(query, object : FFmpegCallBack {
            override fun process(logMessage: LogMessage) {
            }

            override fun success() {
                mergeAudioWithVideo(videoPath, outputPath, output)
            }

            override fun cancel() {
                hideNotification()
                showToast("An error has occurred")
            }

            override fun failed() {
                hideNotification()
                showToast("An error has occurred")
            }
        })
    }

    private fun mergeAudios(
        inputAudioList: ArrayList<String>,
        duration: String,
        output: String
    ): Array<String> {
        val inputs: ArrayList<String> = ArrayList()
        inputs.apply {
            for (i in 0 until inputAudioList.size) {
                add("-i")
                add(inputAudioList[i])
            }
            add("-filter_complex")
            add("amix=inputs=${inputAudioList.size}:duration=$duration:dropout_transition=${inputAudioList.size}")
            add("-codec:a")
            add("libmp3lame")
            add("-q:a")
            add("0")
            add("-preset")
            add("ultrafast")
            add("-y")
            add(output)
        }
        return inputs.toArray(arrayOfNulls<String>(inputs.size))
    }


    fun mergeAudioWithVideo(video: String, audio: String, output: String) {

        val c = arrayOf(
            "-i",
            "$video",
            "-i",
            "$audio",
            "-c:v",
            "copy",
            "-c:a",
            "aac",
            "-map",
            "0:v:0",
            "-map",
            "1:a:0",
            "-shortest",
            "$output",
            "-y"
        )
        mergeVideo(c,output)
    }

    private fun mergeVideo(co: Array<String>, output: String) {
        FFmpeg.executeAsync(co) { executionId, returnCode ->
            EventLiveData.value = Message().apply {
                what = VIDEO_AUDIO_MERGED_PATH
                obj = output
                hideNotification()
            }
        }
    }


}