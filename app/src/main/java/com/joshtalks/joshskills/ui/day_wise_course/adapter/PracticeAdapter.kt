package com.joshtalks.joshskills.ui.day_wise_course.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.PracticeItemLayoutBinding
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import java.util.concurrent.TimeUnit
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable

class PracticeAdapter(
    val context: Context,
    val itemList: ArrayList<ChatModel>,
    val clickListener: PracticeClickListeners
) :
    RecyclerView.Adapter<PracticeAdapter.PracticeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PracticeViewHolder {
        val binding = PracticeItemLayoutBinding.inflate(LayoutInflater.from(context), parent, false)

        return PracticeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PracticeViewHolder, position: Int) {
        holder.bind(itemList.get(position))
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class PracticeViewHolder(val binding: PracticeItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var startTime: Long = 0L
        var filePath: String? = null
        var appAnalytics: AppAnalytics? = null

        fun bind(chatModel: ChatModel) {
            appAnalytics = AppAnalytics.create(AnalyticsEvent.PRACTICE_SCREEN.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam("chatId", chatModel.chatId)
            setPracticeInfoView(chatModel)

            binding.practiceTitleTv.setOnClickListener {
                if (binding.practiceContentLl.visibility == View.GONE)
                    binding.practiceContentLl.visibility = View.VISIBLE
                else
                    binding.practiceContentLl.visibility = View.GONE

            }
            binding.btnPlayInfo.setOnClickListener {
                appAnalytics?.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio Played")
                clickListener.playPracticeAudio(chatModel, layoutPosition)
            }

            binding.submitBtnPlayInfo.setOnClickListener {
                appAnalytics?.addParam(
                    AnalyticsEvent.PRACTICE_EXTRA.NAME,
                    "Already Submitted audio Played"
                )
                clickListener.playSubmitPracticeAudio(chatModel, layoutPosition)
                val state =
                    if (chatModel.isPlaying) {
                        MaterialPlayPauseDrawable.State.Play
                    } else {
                        MaterialPlayPauseDrawable.State.Pause
                    }
                binding.submitBtnPlayInfo.state = state
            }

            binding.ivCancel.setOnClickListener {
                chatModel.filePath = null
                clickListener.removeAudioPractise(chatModel)
                removeAudioPractice()
            }

            binding.submitAnswerBtn.setOnClickListener {

                if (chatModel.filePath == null) {
                    showToast(context.getString(R.string.submit_practise_msz))
                    return@setOnClickListener
                }

                if (clickListener.submitPractise(chatModel)) {
                    appAnalytics?.addParam(AnalyticsEvent.PRACTICE_SOLVED.NAME, true)
                    appAnalytics?.addParam(AnalyticsEvent.PRACTICE_STATUS.NAME, "Submitted")
                    appAnalytics?.addParam(
                        AnalyticsEvent.PRACTICE_TYPE_SUBMITTED.NAME,
                        "$it Practice Submitted"
                    )
                    appAnalytics?.addParam(
                        AnalyticsEvent.PRACTICE_SUBMITTED.NAME,
                        "Submit Practice $"
                    )
                }

            }
        }

        fun setPracticeInfoView(chatModel: ChatModel) {
            chatModel.question?.run {
                binding.audioViewContainer.visibility = View.VISIBLE
                this.audioList?.getOrNull(0)?.audio_url?.let {
                    binding.btnPlayInfo.tag = it
                    binding.practiseSeekbar.max = this.audioList?.getOrNull(0)?.duration!!
                    if (binding.practiseSeekbar.max == 0) {
                        binding.practiseSeekbar.max = 2_00_000
                    }
                }
                initializePractiseSeekBar(chatModel)

                if (this.practiceEngagement.isNullOrEmpty()) {
                    binding.submitAnswerBtn.visibility = View.VISIBLE
//                    appAnalytics.addParam(AnalyticsEvent.PRACTICE_SOLVED.NAME, false)
//                    appAnalytics.addParam(AnalyticsEvent.PRACTICE_STATUS.NAME, "Not Submitted")
                    setViewAccordingExpectedAnswer(chatModel)
                } else {
                    hidePracticeInputLayout()
                    binding.submitAnswerBtn.visibility = View.GONE
//                    appAnalytics.addParam(AnalyticsEvent.PRACTICE_SOLVED.NAME, true)
//                    appAnalytics.addParam(AnalyticsEvent.PRACTICE_STATUS.NAME, "Already Submitted")
                    setViewUserSubmitAnswer(chatModel)
                }
            }
        }

        private fun setViewAccordingExpectedAnswer(chatModel: ChatModel) {
            chatModel.question?.run {
                showPracticeInputLayout()
                this.expectedEngageType?.let {
                    binding.uploadPractiseView.visibility = VISIBLE
                    binding.uploadFileView.visibility = VISIBLE

                    binding.practiseInputHeader.text =
                        context.getString(R.string.record_answer_label)
                    binding.uploadPractiseView.setImageResource(R.drawable.recv_ic_mic_white)
                    audioRecordTouchListener(chatModel)
                    binding.audioPractiseHint.visibility = VISIBLE

                }
            }
        }

        private fun setViewUserSubmitAnswer(chatModel: ChatModel) {
            chatModel.question?.run {
                this.expectedEngageType?.let {
                    hidePracticeInputLayout()
                    showPracticeSubmitLayout()
                    binding.yourSubAnswerTv.visibility = VISIBLE
                    val params: ViewGroup.MarginLayoutParams =
                        binding.subPractiseSubmitLayout.layoutParams as ViewGroup.MarginLayoutParams
                    params.topMargin = Utils.dpToPx(20)
                    binding.subPractiseSubmitLayout.layoutParams = params
                    binding.yourSubAnswerTv.text = context.getString(R.string.your_submitted_answer)
                    val practiseEngagement = this.practiceEngagement?.get(0)

                    binding.submitAudioViewContainer.visibility = VISIBLE
                    filePath = practiseEngagement?.answerUrl
                    if (PermissionUtils.isStoragePermissionEnabled(context) && AppDirectory.isFileExist(
                            practiseEngagement?.localPath
                        )
                    ) {
                        filePath = practiseEngagement?.localPath
                        binding.submitPractiseSeekbar.max =
                            Utils.getDurationOfMedia(context, filePath!!)
                                ?.toInt() ?: 0
                    } else {
                        if (practiseEngagement?.duration != null) {
                            binding.submitPractiseSeekbar.max = practiseEngagement.duration
                        } else {
                            binding.submitPractiseSeekbar.max = 1_00_000
                        }
                    }


                    initializePractiseSeekBar(chatModel)
                    binding.ivCancel.visibility = View.GONE
                }
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun audioRecordTouchListener(chatModel: ChatModel) {
            binding.uploadPractiseView.setOnTouchListener { _, event ->
                if (PermissionUtils.isAudioAndStoragePermissionEnable(context).not()) {
                    clickListener.askRecordPermission()
                    return@setOnTouchListener true
                }
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        binding.videoPlayer.onPause()
                        binding.rootView.requestDisallowInterceptTouchEvent(true)
                        binding.counterContainer.visibility = VISIBLE
                        val scaleAnimation = AnimationUtils.loadAnimation(context, R.anim.scale)
                        binding.uploadPractiseView.startAnimation(scaleAnimation)
                        binding.counterTv.base = SystemClock.elapsedRealtime()
                        startTime = System.currentTimeMillis()
                        binding.counterTv.start()
                        clickListener.startRecording(chatModel, layoutPosition, startTime)
                        binding.audioPractiseHint.visibility = View.GONE

                        appAnalytics?.addParam(AnalyticsEvent.AUDIO_RECORD.NAME, "Audio Recording")
                    }
                    MotionEvent.ACTION_MOVE -> {
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        binding.rootView.requestDisallowInterceptTouchEvent(false)
                        binding.counterTv.stop()
                        val stopTime = System.currentTimeMillis()
                        clickListener.stopRecording(chatModel, layoutPosition, stopTime)
                        binding.uploadPractiseView.clearAnimation()
                        binding.counterContainer.visibility = View.GONE
                        binding.audioPractiseHint.visibility = VISIBLE
//                        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                        val timeDifference =
                            TimeUnit.MILLISECONDS.toSeconds(stopTime) - TimeUnit.MILLISECONDS.toSeconds(
                                startTime
                            )
                        if (timeDifference > 1) {
                            audioAttachmentInit(chatModel)
                            /*practiceViewModel.recordFile?.let {
//                                isAudioRecordDone = true
                                filePath = AppDirectory.getAudioSentFile(null).absolutePath
                                AppDirectory.copy(it.absolutePath, filePath!!)
                                audioAttachmentInit(chatModel)
                            }*/

                        }
                    }
                }

                true
            }
        }

        private fun audioAttachmentInit(chatModel: ChatModel) {
            showPracticeSubmitLayout()
            binding.submitAudioViewContainer.visibility = VISIBLE
            initializePractiseSeekBar(chatModel)
            binding.submitPractiseSeekbar.max =
                Utils.getDurationOfMedia(context, filePath)?.toInt() ?: 0
            enableSubmitButton()
        }

        private fun initializePractiseSeekBar(chatModel: ChatModel) {
            binding.practiseSeekbar.progress = chatModel.playProgress
            binding.practiseSeekbar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    var userSelectedPosition = 0
                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                    }

                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
                            userSelectedPosition = progress
                        }
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        clickListener.onSeekChange(userSelectedPosition.toLong())
                    }
                })
            binding.submitPractiseSeekbar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    var userSelectedPosition = 0
                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                    }

                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
                            userSelectedPosition = progress
                        }
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        clickListener.onSeekChange(userSelectedPosition.toLong())
                    }
                })
        }

        fun removeAudioPractice() {
            hidePracticeSubmitLayout()
            binding.submitAudioViewContainer.visibility = View.GONE
            binding.submitPractiseSeekbar.progress = 0
            binding.submitPractiseSeekbar.max = 0
            binding.submitBtnPlayInfo.state = MaterialPlayPauseDrawable.State.Play
            disableSubmitButton()
            appAnalytics?.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio practise removed")
        }

        private fun disableSubmitButton() {
            binding.submitAnswerBtn.apply {
                isEnabled = false
                isClickable = false
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.seek_bar_background
                    )
                )
            }
        }

        private fun enableSubmitButton() {
            binding.submitAnswerBtn.apply {
                isEnabled = true
                isClickable = true
                backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        AppObjectController.joshApplication,
                        R.color.button_color
                    )
                )
            }
        }

        fun hidePracticeInputLayout() {
            binding.practiseInputHeader.visibility = View.GONE
            binding.practiceInputLl.visibility = View.GONE
        }

        fun showPracticeInputLayout() {
            binding.practiseInputHeader.visibility = View.VISIBLE
            binding.practiceInputLl.visibility = View.VISIBLE
        }

        fun showPracticeSubmitLayout() {
            binding.yourSubAnswerTv.visibility = View.VISIBLE
            binding.subPractiseSubmitLayout.visibility = View.VISIBLE
        }

        fun hidePracticeSubmitLayout() {
            binding.yourSubAnswerTv.visibility = View.GONE
            binding.subPractiseSubmitLayout.visibility = View.GONE
        }
    }

    interface PracticeClickListeners {
        fun playPracticeAudio(chatModel: ChatModel, position: Int)
        fun playSubmitPracticeAudio(chatModel: ChatModel, position: Int)
        fun removeAudioPractise(chatModel: ChatModel)
        fun submitPractise(chatModel: ChatModel): Boolean
        fun onSeekChange(seekTo: Long)
        fun startRecording(chatModel: ChatModel, position: Int, startTimeUnit: Long)
        fun stopRecording(chatModel: ChatModel, position: Int, stopTime: Long)
        fun askRecordPermission()
    }
}