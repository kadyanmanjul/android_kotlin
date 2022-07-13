package com.joshtalks.joshskills.dynamic

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Message
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.FFmpegExecution
import com.arthenica.mobileffmpeg.LogMessage
import com.greentoad.turtlebody.mediapicker.util.UtilsFile
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.constants.INCREASE_AUDIO_VOLUME
import com.joshtalks.joshskills.constants.VIDEO_AUDIO_MERGED_PATH
import com.joshtalks.joshskills.core.showToast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer


const val DURATION_FIRST: String = "first"
class MuxerUtils {
@RequiresApi(api = Build.VERSION_CODES.Q)
public fun saveVideoQ(ctx: Context, videoPath: String): String? {
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

            val inputStream = ctx.contentResolver.openInputStream(Uri.parse(videoPath))
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
    } catch (ex: Exception) {
        showToast(ex.message.toString())
    }
    return getVideoFilePath()
}

fun extractAudioFromVideo(videoPath: String): String {
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

fun mergeAudioWithAudio(audio1: String, audio2: String ,videoPath: String ,output: String) {
    val outputPath = getAudioFilePathMP3()
    val pathsList = ArrayList<String>()

    pathsList.add(audio1)
    pathsList.add(audio2)

    val query = mergeAudios(pathsList, DURATION_FIRST, outputPath)

    CallBackOfQuery().callQuery(query, object : FFmpegCallBack {
        override fun process(logMessage: LogMessage) {
            Log.e("AudioMuxer", logMessage.text)
        }

        override fun success() {
            mergeAudioWithVideo(videoPath, outputPath ,output)
        }

        override fun cancel() {
            showToast("An error has occurred")
        }

        override fun failed() {
            showToast("An error has occurred")
        }
    })
}

 public fun increaseAudioVolume(inputAudio: String) {
    val outputPath = getAudioFilePathMP3()
    val query = audioVolumeUpdate(inputAudio, volume = 5.0f, output = outputPath)
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

fun audioVolumeUpdate(inputFile: String, volume: Float, output: String): Array<String> {
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

fun mergeAudios(inputAudioList: ArrayList<String>, duration: String, output: String): Array<String> {
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
        add(output)
    }
    return inputs.toArray(arrayOfNulls<String>(inputs.size))
}

fun convertAudioToAAC(audio: String): String {
    return if (audio.endsWith(".mp3"))
        audio.removeSuffix(".mp3").plus(".aac")
    else
        audio
}

fun deleteFile(path: String) {
    File(path).delete()
}

fun getVideoFilePath(): String {
    return getAndroidDownloadFolder()?.absolutePath + "/" + "JoshSkill-" + System.currentTimeMillis() + ".mp4"
}

fun getAudioFilePathAAC(): String {
    return getAndroidDownloadFolder()?.absolutePath + "/" + "JoshSkill-" + System.currentTimeMillis() + ".aac"
}

fun getAudioFilePathMP3(): String {
    return getAndroidDownloadFolder()?.absolutePath + "/" + "JoshSkill-" + System.currentTimeMillis() + ".mp3"
}

fun getAndroidDownloadFolder(): File? {
    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
}

fun audioVideoMuxer(filePath: String?, videoDownPath: String?, outputFile: String): String {
    try {
        val videoExtractor: MediaExtractor = MediaExtractor()
        val audioExtractor: MediaExtractor = MediaExtractor()

        val audioPath = filePath?.let { convertAudioToAAC(it) }

        audioExtractor.setDataSource(filePath!!)
        audioExtractor.selectTrack(0)
        val audioFormat: MediaFormat = audioExtractor.getTrackFormat(0)
        videoDownPath?.let { videoExtractor.setDataSource(it) }
        videoExtractor.selectTrack(0)
        val videoFormat: MediaFormat = videoExtractor.getTrackFormat(0)

        val muxer: MediaMuxer = MediaMuxer(
            outputFile,
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
                videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime()
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
                audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime()
                audioBufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo)
                audioExtractor.advance()
            }
        }

        muxer.stop()
        muxer.release()

        return outputFile

    } catch (e: IOException) {
        e.printStackTrace()
        return outputFile
    } catch (e: Exception) {
        e.printStackTrace()
        return outputFile
    }
}

fun mergeAudioWithVideo(video:String ,audio:String ,output:String) {
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
        "$output"
    )
    Log.e("Ayaaz 1","$output")
    mergeVideo(c)
}

private fun mergeVideo(co: Array<String>) {
    FFmpeg.executeAsync(co) { executionId, returnCode ->
        Log.d("hello", "return  $returnCode")
        Log.d("hello", "executionID  $executionId")
        Log.d("hello", "FFMPEG  " + FFmpegExecution(executionId, co))

        EventLiveData.value = Message().apply {
            what = VIDEO_AUDIO_MERGED_PATH
        }
    }
}
}

/*
WORKING
Audio:/storage/emulated/0/Android/data/com.joshtalks.joshskills/files/JoshSkill/Media/JoshAppAudio/Sent/Record_1656071639687.aac        //only our audio
Video:/storage/emulated/0/Android/data/com.joshtalks.joshskills/files/JoshSkill/Media/JoshAppDocuments/5.mp4

NOT WORKING
Audio:/storage/emulated/0/Download/JoshSkill-1656072297413.aac                                                                          //merged audio
Video:/storage/emulated/0/Android/data/com.joshtalks.joshskills/files/JoshSkill/Media/JoshAppDocuments/5.mp4
 */