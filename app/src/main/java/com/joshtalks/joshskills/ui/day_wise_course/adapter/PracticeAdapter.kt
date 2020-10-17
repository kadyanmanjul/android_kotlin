package com.joshtalks.joshskills.ui.day_wise_course.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.PracticeItemLayoutBinding
import com.joshtalks.joshskills.repository.local.entity.ChatModel
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
        fun bind(chatModel: ChatModel) {

            setPracticeInfoView(chatModel)

            binding.practiceTitleTv.setOnClickListener {
                if (binding.practiceContentLl.visibility == View.GONE)
                    binding.practiceContentLl.visibility = View.VISIBLE
                else
                    binding.practiceContentLl.visibility = View.GONE

            }
            binding.btnPlayInfo.setOnClickListener {
                clickListener.playPracticeAudio(chatModel, layoutPosition)
            }

            binding.submitBtnPlayInfo.setOnClickListener {
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
                clickListener.removeAudioPractise(chatModel)
                removeAudioPractice()
            }

            binding.submitAnswerBtn.setOnClickListener {
                clickListener.submitPractise(chatModel)
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
            }
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
        fun submitPractise(chatModel: ChatModel)
        fun onSeekChange(seekTo: Long)
    }
}