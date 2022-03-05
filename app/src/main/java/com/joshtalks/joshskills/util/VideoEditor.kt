package com.joshtalks.joshskills.util

import android.content.Context
import android.util.Log
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException
import java.io.File
import java.io.IOException

class VideoEditor private constructor(private val context: Context) {

    private var videoPath: String? = null
    private var callback: VideoFFMpegCallback? = null
    private var outputFilePath = ""
    private var position: String? = null
    private var imagePath: String? = null

    companion object {
        fun with(context: Context): VideoEditor {
            return VideoEditor(context)
        }
    }

    fun setVideoPath(videoPath: String): VideoEditor {
        this.videoPath = videoPath
        return this
    }


    fun setCallback(callback: VideoFFMpegCallback): VideoEditor {
        this.callback = callback
        return this
    }

    fun setImagePath(imagePath: String): VideoEditor {
        this.imagePath = imagePath
        return this
    }

    fun setOutputPath(outputPath: String): VideoEditor {
        this.outputFilePath = outputPath
        return this
    }

    fun setPosition(overlayPosition: OverlayPosition): VideoEditor {
        this.position = overlayPosition.value
        return this
    }

    fun execute() {
        val outputFile = File(outputFilePath)
        Log.v("Yash", "outputFilePath: $outputFilePath")
        val cmd: Array<String> = arrayOf(
            "-y",
            "-i",
            videoPath!!,
            "-i",
            imagePath!!,
            "-filter_complex",
            position!!,
            "-codec:a",
            "copy",
            outputFile.path
        )
        try {
            FFmpeg.getInstance(context).execute(cmd, object : ExecuteBinaryResponseHandler() {
                override fun onStart() {

                }

                override fun onProgress(message: String?) {
                    callback!!.onProgress(message!!)
                }

                override fun onSuccess(message: String?) {
                    callback!!.onSuccess(outputFile, "video")
                }

                override fun onFailure(message: String?) {
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                    callback!!.onFailure(IOException(message))
                }

                override fun onFinish() {
                    callback!!.onFinish()
                }
            })
        } catch (e: Exception) {
            callback!!.onFailure(e)
        } catch (e2: FFmpegCommandAlreadyRunningException) {
            callback!!.onNotAvailable(e2)
        }
    }

    enum class OverlayPosition(val value: String) {
        CENTER_ALIGN("overlay=(W/w)/2:(H/h)/2"),
        BOTTOM_CENTER_ALIGN ("[0]scale=320:-1[img]; [1]scale=320:-1[vid]; [img][vid] overlay=0:(H-h)/1.3")
    }

    interface VideoFFMpegCallback {
        fun onProgress(progress: String)
        fun onSuccess(convertedFile: File, type: String)
        fun onFailure(error: Exception)
        fun onNotAvailable(error: Exception)
        fun onFinish()
    }
}