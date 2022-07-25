package com.joshtalks.joshskills.dynamic

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.Message
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
//import com.arthenica.mobileffmpeg.FFmpeg
//import com.arthenica.mobileffmpeg.LogMessage
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.constants.INCREASE_AUDIO_VOLUME
import com.joshtalks.joshskills.constants.VIDEO_AUDIO_MERGED_PATH
import com.joshtalks.joshskills.constants.VIDEO_AUDIO_MUX_FAILED
import com.joshtalks.joshskills.repository.local.model.NotificationChannelNames
import java.io.File
import java.io.FileOutputStream
import java.net.URISyntaxException
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
const val TAG = "VideoMergeService"

//class VideoMergeService : Service() {
//    private var mNotificationManager: NotificationManager? = null
//
//    override fun onCreate() {
//        super.onCreate()
//        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
//    }
//
//    override fun onBind(p0: Intent?): IBinder? {
//        return null
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val videoPath = intent?.getStringExtra(VIDEO_PATH)
//        val audioPath = intent?.getStringExtra(AUDIO_PATH)
//        if (videoPath != null && audioPath != null) {
//            Log.d(
//                TAG,
//                "onStartCommand() called with: videoPath = $videoPath, audioPath = $audioPath"
//            )
//            startVideoMuxing(videoPath, audioPath)
//            showNotification()
//        }
//        return START_STICKY
//    }
//
//    private fun showNotification() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name: CharSequence = NotificationChannelNames.OTHERS.type
//            val importance: Int = NotificationManager.IMPORTANCE_LOW
//            val mChannel =
//                NotificationChannel(CHANNEL_ID, name, importance)
//            mNotificationManager?.createNotificationChannel(mChannel)
//        }
//
//        val lNotificationBuilder = NotificationCompat.Builder(
//                this,
//                CHANNEL_ID
//            )
//                .setChannelId(CHANNEL_ID)
//                .setContentTitle(getString(R.string.app_name))
//                .setContentText("Creating your Video...")
//                .setSmallIcon(R.drawable.ic_status_bar_notification)
//                .setOngoing(false)
//                .setAutoCancel(true)
//                .setPriority(NotificationCompat.PRIORITY_MIN)
//
//
//        startForeground(NOTIFICATION_ID, lNotificationBuilder.build())
//    }
//
//    private fun hideNotification(isSuccess :Boolean = false) {
//        Log.d(TAG, "hideNotification() called $isSuccess")
//        if (isSuccess.not()){
//            EventLiveData.value = Message().apply {
//                what = VIDEO_AUDIO_MUX_FAILED
//            }
//        }
//        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
//        stopForeground(true)
//    }
//
//    private fun startVideoMuxing(
//        videoDownPath: String,
//        audioPath: String
//    ) {
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                if (isActive) {
//                    var outputFile: String? = ""
//                    if (Build.VERSION.SDK_INT >= 29) {
//                        outputFile = saveVideoQ(this@VideoMergeService, videoDownPath)
//                    } else {
//                        outputFile = getVideoFilePath()
//                    }
//                    increaseAudioVolume(audioPath)
//                    if (outputFile != null) {
//                        val extractedPath = extractAudioFromVideo(videoDownPath)
//                        mergeAudioWithAudio(
//                            audioPath,
//                            extractedPath, videoDownPath, outputFile
//                        )
//                    } else{
//                        hideNotification()
//                    }
//                } else{
//                    hideNotification()
//                }
//            }catch (ex:Exception){
//                hideNotification()
//            }
//        }
//    }
//
//    private fun increaseAudioVolume(inputAudio: String) {
//        val outputPath = getAudioFilePathMP3()
//        val query = audioVolumeUpdate(inputAudio, volume = 10.0f, output = outputPath)
//        CallBackOfQuery().callQuery(query, object : FFmpegCallBack {
//            override fun process(logMessage: LogMessage) {
//            }
//
//            override fun success() {
//                EventLiveData.value = Message().apply {
//                    what = INCREASE_AUDIO_VOLUME
//                    obj = outputPath
//                }
//            }
//
//            override fun cancel() {
//            }
//
//            override fun failed() {
//            }
//        })
//    }
//
//
//    private fun getAudioFilePathMP3(): String {
//        return getAndroidDownloadFolder()?.absolutePath + "/" + "JoshSkill-" + System.currentTimeMillis() + ".mp3"
//    }
//
//    private fun getAudioFilePathAAC(): String {
//        return getAndroidDownloadFolder()?.absolutePath + "/" + "JoshSkill-" + System.currentTimeMillis() + ".aac"
//    }
//
//    private fun audioVolumeUpdate(inputFile: String, volume: Float, output: String): Array<String> {
//        val inputs: ArrayList<String> = ArrayList()
//        inputs.apply {
//            add("-i")
//            add(inputFile)
//            add("-af")
//            add("volume=$volume")
//            add("-preset")
//            add("ultrafast")
//            add(output)
//        }
//        return inputs.toArray(arrayOfNulls<String>(inputs.size))
//    }
//
//
//    private fun extractAudioFromVideo(videoPath: String): String {
//        val outputAudioPath = getAudioFilePathAAC()
//        val videoAudioExtractor: MediaExtractor = MediaExtractor()
//        var videoAudioFormat: MediaFormat = MediaFormat()
//        val sampleSize = 256 * 1024
//        var sawEOS = false
//        var frameCount = 0
//        val offset = 100
//
//        videoAudioExtractor.setDataSource(videoPath)
//        for (i in 0 until videoAudioExtractor.trackCount) {
//            videoAudioFormat = videoAudioExtractor.getTrackFormat(i)
//            val mime = videoAudioFormat.getString(MediaFormat.KEY_MIME);
//            if (mime!!.startsWith("audio/")) {
//                videoAudioExtractor.selectTrack(i)
//            }
//        }
//
//        val muxer: MediaMuxer = MediaMuxer(
//            outputAudioPath,
//            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
//        )
//
//        val videoAudioTrack = muxer.addTrack(videoAudioFormat)
//        val videoAudioBuffer: ByteBuffer = ByteBuffer.allocate(sampleSize)
//        val videoAudioBufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
//        videoAudioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
//
//        muxer.start()
//
//        while (!sawEOS) {
//            frameCount++
//
//            videoAudioBufferInfo.offset = offset
//            videoAudioBufferInfo.size = videoAudioExtractor.readSampleData(videoAudioBuffer, offset)
//
//            if (videoAudioBufferInfo.size < 0) {
//                sawEOS = true
//                videoAudioBufferInfo.size = 0
//            } else {
//                videoAudioBufferInfo.presentationTimeUs = videoAudioExtractor.getSampleTime()
//                videoAudioBufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
//                muxer.writeSampleData(videoAudioTrack, videoAudioBuffer, videoAudioBufferInfo)
//                videoAudioExtractor.advance()
//            }
//        }
//
//        muxer.stop()
//        muxer.release()
//        videoAudioExtractor.release()
//
//        return outputAudioPath
//    }
//
//
//    private fun mergeAudioWithAudio(audio1: String, audio2: String, videoPath: String, output: String) {
//        val outputPath = getAudioFilePathMP3()
//        val pathsList = ArrayList<String>()
//
//        pathsList.add(audio1)
//        pathsList.add(audio2)
//
//        val query = mergeAudios(pathsList, DURATION_FIRST, outputPath)
//
//        CallBackOfQuery().callQuery(query, object : FFmpegCallBack {
//            override fun process(logMessage: LogMessage) {
//                Log.d(TAG, "mergeAudioWithAudio process() called with: logMessage = $logMessage")
//            }
//
//            override fun success() {
//                Log.d(TAG, "mergeAudioWithAudio success() called")
//                mergeAudioWithVideo(videoPath, outputPath, output)
//            }
//
//            override fun cancel() {
//                Log.d(TAG, "mergeAudioWithAudio cancel() called")
//                hideNotification()
//            }
//
//            override fun failed() {
//                Log.d(TAG, "mergeAudioWithAudio failed() called")
//                hideNotification()
//            }
//        })
//    }
//
//    private fun mergeAudios(
//        inputAudioList: ArrayList<String>,
//        duration: String,
//        output: String
//    ): Array<String> {
//        val inputs: ArrayList<String> = ArrayList()
//        inputs.apply {
//            for (i in 0 until inputAudioList.size) {
//                add("-i")
//                add(inputAudioList[i])
//            }
//            add("-filter_complex")
//            add("amix=inputs=${inputAudioList.size}:duration=$duration:dropout_transition=${inputAudioList.size}")
//            add("-codec:a")
//            add("libmp3lame")
//            add("-q:a")
//            add("0")
//            add("-preset")
//            add("ultrafast")
//            add("-y")
//            add(output)
//        }
//        return inputs.toArray(arrayOfNulls<String>(inputs.size))
//    }
//
//
//    fun mergeAudioWithVideo(video: String, audio: String, output: String) {
//
//        val c = arrayOf(
//            "-i",
//            "$video",
//            "-i",
//            "$audio",
//            "-c:v",
//            "copy",
//            "-c:a",
//            "aac",
//            "-map",
//            "0:v:0",
//            "-map",
//            "1:a:0",
//            "-shortest",
//            "$output",
//            "-y"
//        )
//        mergeVideo(c,output)
//    }
//
//    private fun mergeVideo(co: Array<String>, output: String) {
//        FFmpeg.executeAsync(co) { executionId, returnCode ->
//            Log.d(
//                TAG,
//                "mergeVideo() called with: executionId = $executionId, returnCode = $returnCode"
//            )
//            EventLiveData.value = Message().apply {
//                what = VIDEO_AUDIO_MERGED_PATH
//                obj = output
//                hideNotification(true)
//            }
//        }
//    }
//
//
//    @RequiresApi(api = Build.VERSION_CODES.Q)
//    fun saveVideoQ(ctx: Context, videoPath: String): String? {
//        try {
//            val valuesVideos = ContentValues()
//            val videoFileName = "MergedVideo_" + System.currentTimeMillis() + ".mp4"
//            valuesVideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/")
//            valuesVideos.put(MediaStore.Video.Media.TITLE, videoFileName)
//            valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
//            valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
//            val uriSavedVideo = ctx.contentResolver.insert(
//                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
//                valuesVideos
//            )
//            val pfd: ParcelFileDescriptor?
//            try {
//                pfd = uriSavedVideo?.let { ctx.contentResolver.openFileDescriptor(it, "w") }
//                assert(pfd != null)
//                val out = FileOutputStream(pfd!!.fileDescriptor)
//                val uri = Uri.fromFile(File(videoPath))
//                val inputStream = ctx.contentResolver.openInputStream(uri)
//                val buf = ByteArray(8192)
//                var len: Int
//                var progress = 0
//                while (inputStream!!.read(buf).also { len = it } > 0) {
//                    progress += len
//                    out.write(buf, 0, len)
//                }
//                out.close()
//                inputStream.close()
//                pfd.close()
//                valuesVideos.clear()
//                valuesVideos.put(MediaStore.Video.Media.IS_PENDING, 0)
//                valuesVideos.put(
//                    MediaStore.Video.Media.IS_PENDING,
//                    0
//                )
//                uriSavedVideo?.let { ctx.contentResolver.update(it, valuesVideos, null, null) }
//                return uriSavedVideo?.let { getFilePath(ctx, it) }
//            } catch (e: java.lang.Exception) {
//                e.printStackTrace()
//            }
//        }catch (ex:Exception){
//
//        }
//        return getVideoFilePath()
//    }
//
//
//    fun getVideoFilePath(): String {
//        return getAndroidDownloadFolder()?.absolutePath + "/" + "JoshSkill-" + System.currentTimeMillis() + ".mp4"
//    }
//
//    fun getAndroidDownloadFolder(): File? {
//        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//    }
//
//
//    @SuppressLint("NewApi")
//    @Throws(URISyntaxException::class)
//    fun getFilePath(context: Context, uri_: Uri): String? {
//        var uri = uri_
//        var selection: String? = null
//        var selectionArgs: Array<String>? = null
//        // Uri is different in versions after KITKAT (Android 4.4), we need to
//        if (Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri(context.applicationContext, uri)) {
//            if (isExternalStorageDocument(uri)) {
//                val docId = DocumentsContract.getDocumentId(uri)
//                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
//            } else if (isDownloadsDocument(uri)) {
//                val id = DocumentsContract.getDocumentId(uri)
//                uri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))
//            } else if (isMediaDocument(uri)) {
//                val docId = DocumentsContract.getDocumentId(uri)
//                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//                val type = split[0]
//                if ("image" == type) {
//                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//                } else if ("video" == type) {
//                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
//                } else if ("audio" == type) {
//                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
//                }
//                selection = "_id=?"
//                selectionArgs = arrayOf(split[1])
//            }
//        }
//        if ("content".equals(uri.scheme!!, ignoreCase = true)) {
//            val projection = arrayOf(MediaStore.Images.Media.DATA)
//            val cursor: Cursor?
//            try {
//                cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
//                val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
//                if (cursor.moveToFirst()) {
//                    return cursor.getString(column_index)
//                }
//            } catch (e: Exception) {
//            }
//
//        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
//            return uri.path
//        }
//        return null
//    }
//
//    private fun isExternalStorageDocument(uri: Uri): Boolean {
//        return "com.android.externalstorage.documents" == uri.authority
//    }
//
//    private fun isDownloadsDocument(uri: Uri): Boolean {
//        return "com.android.providers.downloads.documents" == uri.authority
//    }
//
//    private fun isMediaDocument(uri: Uri): Boolean {
//        return "com.android.providers.media.documents" == uri.authority
//    }

//}