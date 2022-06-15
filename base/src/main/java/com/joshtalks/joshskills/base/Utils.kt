package com.joshtalks.joshskills.base

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

const val APP_DIRECTORY = "JoshSkill"
const val MEDIA_DIRECTORY = "Media"
const val AUDIO_EXTENSION = ".aac"

fun getAudioSentMigratedPath(context: Context): String {
    return context.getExternalFilesDir(null)
        .toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshAppAudio/Sent"
}

fun getAudioSentFile(context: Context, path: String?, audioExtension: String = AUDIO_EXTENSION): File {

    val rootPath = getAudioSentMigratedPath(context)
    val f = File(rootPath)
    if (f.exists().not()) {
        f.mkdirs()
    }
    var fileName = ""

    if (path.isNullOrEmpty().not()) {
        fileName = File(path!!).name
    }

    if (fileName.isEmpty()) {
        fileName = "Record_" + System.currentTimeMillis().toString() + audioExtension
    }
    val file = File(rootPath + File.separator + fileName)
    file.createNewFile()
    return file
}

fun getVideoSentMigratedPath(context: Context): String {
    return context.getExternalFilesDir(null)
        .toString() + File.separator + APP_DIRECTORY + File.separator + MEDIA_DIRECTORY + "/JoshAppVideos/Sent"
}

fun videoSentFile(context: Context): File {
    val rootPath = getVideoSentMigratedPath(context)

    val f = File(rootPath)
    if (f.exists().not()) {
        f.mkdirs()
    }
    val file = File(rootPath + File.separator + "VID".plus("-") + System.currentTimeMillis().toString() +".mp4")
    file.createNewFile()
    return file
}

fun copy(fromPath: String, toPath: String): Boolean {

    val file = File(fromPath)
    if (!file.isFile) {
        return false
    }
    var inStream: FileInputStream? = null
    var outStream: FileOutputStream? = null
    try {
        inStream = FileInputStream(file)
        outStream = FileOutputStream(File(toPath))
        val inChannel = inStream.channel
        val outChannel = outStream.channel
        inChannel.transferTo(0, inChannel.size(), outChannel)
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    } finally {
        closeSilently(inStream)
        closeSilently(outStream)
    }
    return true
}

private fun closeSilently(closeable: Closeable?) {
    try {
        closeable?.close()
    } catch (e: IOException) {
    }
}

suspend fun audioVideoMuxer(recordAudioFile: File, recordVideoFile: File?,context: Context) {
    try {
        Log.e("sagar", "audioVideoMuxer() called with: recordAudioFile = $recordAudioFile, recordVideoFile = $recordVideoFile")
        val outputFile = if (Build.VERSION.SDK_INT >= 29) {
            saveVideoQ(context, recordVideoFile?.absolutePath?:"")
        } else {
            getVideoFilePath()
        }

        Log.e("sagar", "outputFile: $outputFile")
        val videoExtractor = MediaExtractor()
        videoExtractor.setDataSource(recordVideoFile?.absolutePath?:"")
        videoExtractor.selectTrack(0)
        val videoFormat: MediaFormat = videoExtractor.getTrackFormat(0)
        val audioExtractor = MediaExtractor()
        audioExtractor.setDataSource(recordAudioFile.absolutePath?:"")
        audioExtractor.selectTrack(0)

        val audioFormat: MediaFormat = audioExtractor.getTrackFormat(0)

        val muxer = MediaMuxer(
            outputFile?:"",
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )

        val videoTrack = muxer.addTrack(videoFormat)
        val audioTrack = muxer.addTrack(audioFormat)

        var sawEOS = false
        var frameCount = 0
        val offset = 100
        val sampleSize = 256 * 1024
        val videoBuf: ByteBuffer = ByteBuffer.allocate(sampleSize)
        val audioBuf: ByteBuffer = ByteBuffer.allocate(sampleSize)
        val videoBufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
        val audioBufferInfo: MediaCodec.BufferInfo = MediaCodec.BufferInfo()

        videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

        muxer.start()
        while (!sawEOS) {
            videoBufferInfo.offset = offset
            videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset)

            if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
                sawEOS = true
                videoBufferInfo.size = 0

            } else {
                videoBufferInfo.presentationTimeUs = videoExtractor.sampleTime
                videoBufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME
                muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo)
                videoExtractor.advance()

                frameCount++
            }
        }

        var sawEOS2 = false
        var frameCount2 = 0
        while (!sawEOS2) {
            frameCount2++

            audioBufferInfo.offset = offset
            audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, offset)

            if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
                sawEOS2 = true
                audioBufferInfo.size = 0
            } else {
                audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
                audioBufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo)
                audioExtractor.advance()
            }
        }

        muxer.stop()
        muxer.release()

    } catch (e: IOException) {
        Log.e("sagar", "audioVideoMuxerError:${e.message}")
    }catch (ex:java.lang.Exception){
        Log.e("sagar", "audioVideoMuxerErro11r:${ex.message}")
    }
}

@RequiresApi(api = Build.VERSION_CODES.Q)
fun saveVideoQ(ctx: Context, videoPath: String): String? {
    try {
        val valuesVideos = ContentValues()
        val videoFileName = "MergedVideo_" + System.currentTimeMillis() + ".mp4"
        valuesVideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/")
        valuesVideos.put(MediaStore.Video.Media.TITLE, videoFileName)
        valuesVideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
        valuesVideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        val uriSavedVideo = ctx.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            valuesVideos
        )
        val pfd: ParcelFileDescriptor?
        try {
            pfd = uriSavedVideo?.let { ctx.contentResolver.openFileDescriptor(it, "w") }
            assert(pfd != null)
            val out = FileOutputStream(pfd!!.fileDescriptor)
            val uri = Uri.fromFile(File(videoPath))
            val inputStream = ctx.contentResolver.openInputStream(uri)
            val buf = ByteArray(8192)
            var len: Int
            var progress = 0
            while (inputStream!!.read(buf).also { len = it } > 0) {
                progress += len
                out.write(buf, 0, len)
            }
            out.close()
            inputStream.close()
            pfd.close()
            valuesVideos.clear()
            valuesVideos.put(MediaStore.Video.Media.IS_PENDING, 0)
            valuesVideos.put(
                MediaStore.Video.Media.IS_PENDING,
                0
            )
            uriSavedVideo?.let { ctx.contentResolver.update(it, valuesVideos, null, null) }
            return uriSavedVideo?.let { UtilsFile.getFilePath(ctx, it) }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }catch (ex:Exception){

    }
    return getVideoFilePath()
}


fun getVideoFilePath(): String {
    return getAndroidDownloadFolder()?.absolutePath + "/" + "JoshSkill-" + System.currentTimeMillis() + ".mp4"
}

fun getAndroidDownloadFolder(): File? {
    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
}