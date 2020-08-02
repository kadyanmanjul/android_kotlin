package com.joshtalks.joshskills.core.custom_ui.recorder

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaRecorder
import android.os.AsyncTask
import android.util.Log
import java.io.File
import java.io.IOException


class AudioRecorder internal constructor(
    private val mContext: Context,
    /**
     * Returns the current record filename.
     *
     * @return The current filename (the full path).
     */
    val recordFileName: String,
    private val mMediaRecorderConfig: MediaRecorderConfig,
    private val mIsLoggable: Boolean
) {
    enum class Status {
        STATUS_UNKNOWN, STATUS_READY_TO_RECORD, STATUS_RECORDING, STATUS_RECORD_PAUSED
    }

    /**
     * Returns the current recording status.
     *
     * @return The current recorder status.
     *
     * @see Status
     */
    var status: Status? = null

    interface OnException {
        fun onException(e: Exception?)
    }

    interface OnStartListener :
        OnException {
        fun onStarted()
    }

    interface OnPauseListener :
        OnException {
        fun onPaused(activeRecordFileName: String?)
    }

    /**
     * @author lassana
     * @since 10/06/2013
     */
    class MediaRecorderConfig
    /**
     * Constructor.
     *
     * @param audioEncodingBitRate Used for [MediaRecorder.setAudioEncodingBitRate]
     * @param audioChannels        Used for [MediaRecorder.setAudioChannels]
     * @param audioSource          Used for [MediaRecorder.setAudioSource]
     * @param audioEncoder         Used for [MediaRecorder.setAudioEncoder]
     */(
        var mAudioEncodingBitRate: Int,
        var mAudioChannels: Int,
        var mAudioSource: Int,
        var mAudioEncoder: Int
    ) {

        companion object {
            val DEFAULT =
                MediaRecorderConfig( /* 64 Kib per second            */
                    64 * 1024,  /* Stereo                       */
                    2,  /* Default audio source (usually, device microphone)  */
                    MediaRecorder.AudioSource.DEFAULT,  /* Default encoder for the target Android version   */
                    MediaRecorder.AudioEncoder.HE_AAC
                )
        }

    }

    internal inner class StartRecordTask :
        AsyncTask<OnStartListener?, Void?, Exception?>() {
        private var mOnStartListener: OnStartListener? = null
        override fun doInBackground(vararg params: OnStartListener?): Exception? {
            mOnStartListener = params[0]
            var exception: Exception? = null
            try {
                mediaRecorder!!.prepare()
                mediaRecorder!!.start()
            } catch (e: IOException) {
                exception = e
            }
            return exception
        }

        override fun onPostExecute(result: Exception?) {
            super.onPostExecute(result)
            if (result == null) {
                this@AudioRecorder.status =
                    AudioRecorder.Status.STATUS_RECORDING
                mOnStartListener?.onStarted()
            } else {
                this@AudioRecorder.status =
                    AudioRecorder.Status.STATUS_READY_TO_RECORD
                mOnStartListener?.onException(result)
            }
        }
    }

    internal inner class PauseRecordTask : AsyncTask<OnPauseListener?, Void?, Exception?>() {
        private var mOnPauseListener: OnPauseListener? = null
        override fun doInBackground(vararg params: OnPauseListener?): Exception? {
            mOnPauseListener = params[0]
            var exception: Exception? = null
            try {
                mediaRecorder!!.stop()
                mediaRecorder!!.release()
            } catch (e: Exception) {
                exception = e
            }
            if (exception == null) {
                appendToFile(recordFileName, temporaryFileName)
            }
            return exception
        }

        override fun onPostExecute(result: Exception?) {
            super.onPostExecute(result)
            if (result == null) {
                this@AudioRecorder.status =
                    AudioRecorder.Status.STATUS_RECORD_PAUSED
                mOnPauseListener?.onPaused(recordFileName)
            } else {
                this@AudioRecorder.status =
                    AudioRecorder.Status.STATUS_READY_TO_RECORD
                mOnPauseListener?.onException(result)
            }
        }
    }


    /**
     * Returns the MediaRecorder object
     * @return The current MediaRecorder object
     */
    var mediaRecorder: MediaRecorder? = null
        private set

    /**
     * Continues an existing record or starts a new one.
     *
     * @param listener The listener instance.
     */
    @SuppressLint("NewApi")
    fun start(listener: OnStartListener?) {

        val task = StartRecordTask()
        mediaRecorder = MediaRecorder()
        mediaRecorder!!.setAudioSamplingRate(16000)
        mediaRecorder!!.setAudioEncodingBitRate(32000)
        //mMediaRecorder.setAudioEncodingBitRate(mMediaRecorderConfig.mAudioEncodingBitRate);
        mediaRecorder!!.setAudioChannels(mMediaRecorderConfig.mAudioChannels)
        mediaRecorder!!.setAudioSource(mMediaRecorderConfig.mAudioSource)
        mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder!!.setOutputFile(temporaryFileName)
        mediaRecorder!!.setAudioEncoder(mMediaRecorderConfig.mAudioEncoder)
        task.execute(listener)
    }

    /**
     * Pauses an active recording.
     *
     * @param listener The listener instance.
     */
    @SuppressLint("NewApi")
    fun pause(listener: OnPauseListener?) {
        val task = PauseRecordTask()
        task.execute(listener)
    }

    /**
     * Returns true if record is started, false if not.
     *
     * @return true if record is started.
     */
    val isRecording: Boolean
        get() = status == Status.STATUS_RECORDING

    /**
     * Returns true if record can be started, false if not.
     *
     * @return true if record can be started.
     */
    val isReady: Boolean
        get() = status == Status.STATUS_READY_TO_RECORD

    /**
     * Returns true if record is paused, false if not.
     *
     * @return true if record is paused.
     */
    val isPaused: Boolean
        get() = status == Status.STATUS_RECORD_PAUSED

    private val temporaryFileName: String
        get() = mContext.cacheDir.absolutePath + File.separator + "tmprecord"

    private fun appendToFile(
        targetFileName: String,
        newFileName: String
    ) {
        Mp4ParserWrapper.append(targetFileName, newFileName)
    }

    /**
     * Drops the current recording.
     */
    fun cancel() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder!!.stop()
                mediaRecorder!!.release()
            }
        } catch (e: Exception) {
            error("Exception during record cancelling", e)
        }
        status =
            Status.STATUS_UNKNOWN
    }

    private fun debug(msg: String, e: Exception?) {
        if (mIsLoggable) Log.d(TAG, msg, e)
    }

    private fun error(msg: String, e: Exception?) {
        if (mIsLoggable) Log.e(TAG, msg, e)
    }

    companion object {
        private const val TAG = "AudioRecorder"

        /**
         * Returns a ready-to-use AudioRecorder.
         * Uses [.MediaRecorderConfig.DEFAULT] as
         * [MediaRecorder] config.
         *
         * @param context An Android context instance.
         * @param targetFileName A filename (full path) of a record.
         * @return A configured [AudioRecorder] instance.
         *
         */
        @Deprecated("Use AudioRecorderBuilder instead.")
        fun build(
            context: Context,
            targetFileName: String
        ): AudioRecorder {
            return build(
                context,
                targetFileName,
                MediaRecorderConfig.DEFAULT
            )
        }

        /**
         * Returns a ready-to-use AudioRecorder.
         *
         * @param context An Android context instance.
         * @param targetFileName A filename (full path) of a record.
         * @param mediaRecorderConfig A record config.
         * @return A configured [AudioRecorder] instance.
         *
         */
        @Deprecated("Use AudioRecorderBuilder instead.")
        fun build(
            context: Context,
            targetFileName: String,
            mediaRecorderConfig: MediaRecorderConfig
        ): AudioRecorder {
            val rvalue =
                AudioRecorder(
                    context,
                    targetFileName,
                    mediaRecorderConfig,
                    false
                )
            rvalue.status =
                Status.STATUS_READY_TO_RECORD
            return rvalue
        }
    }

    /* package-local */
    init {
        status =
            Status.STATUS_READY_TO_RECORD
    }
}
