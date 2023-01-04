package com.joshtalks.joshskills.common.ui.voip.new_arch.ui.utils

import android.content.Intent
import android.widget.Chronometer
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.voip.base.constants.*
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.views.VoiceCallActivity
import de.hdodenhof.circleimageview.CircleImageView

@BindingAdapter("setProfileImage")
fun CircleImageView.setProfileImage(imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty())
        Glide.with(this)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(this)
    else
        Glide.with(this)
            .load(R.drawable.ic_call_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(this)
}

@BindingAdapter("setCallBackground")
fun ConstraintLayout.setCallBackground(isGameOn: Boolean) {
    when (isGameOn) {
        true -> {
            this.setBackgroundColor(resources.getColor(R.color.pure_black))
        }
        false -> {
            this.setBackgroundColor(resources.getColor(R.color.primary_500))
        }
    }
}

@BindingAdapter("setSpeakerImage")
fun AppCompatImageButton.setSpeakerImage(isSpeakerOn: Boolean) {
    if (!isSpeakerOn) {
        this.backgroundTintList =
            ContextCompat.getColorStateList(context, R.color.pure_white_10)
        this.imageTintList = ContextCompat.getColorStateList(context, R.color.pure_white)
    } else {
        this.backgroundTintList =
            ContextCompat.getColorStateList(context, R.color.pure_white)
        this.imageTintList =
            ContextCompat.getColorStateList(context, R.color.icon_subdued)
    }
}

@BindingAdapter("setMicImage")
fun AppCompatImageButton.setMicImage(isMute: Boolean) {
    if (!isMute) {
        this.backgroundTintList =
            ContextCompat.getColorStateList(context, R.color.pure_white_10)
        this.imageTintList = ContextCompat.getColorStateList(context, R.color.pure_white)
    } else {
        this.backgroundTintList =
            ContextCompat.getColorStateList(context, R.color.pure_white)
        this.imageTintList =
            ContextCompat.getColorStateList(context, R.color.icon_subdued)
    }
}

@BindingAdapter("setRecordButtonImage")
fun AppCompatImageButton.setRecordButtonImage(isRecording: Boolean) {
    if (!isRecording) {
        this.setImageResource(R.drawable.ic_record_btn)
    } else {
        this.setImageResource(R.drawable.ic_stop_record)
    }
}

@BindingAdapter("startTimer")
fun Chronometer.startTimer(baseTime: Long) {
    if (baseTime > 0) {
        base = baseTime
        start()
    }
}

@BindingAdapter("acceptCall")
fun AppCompatImageButton.acceptCall(isAccept: Boolean?) {
    this.setOnClickListener {
        if (isAccept == true) {
            val intent = Intent(context, VoiceCallActivity::class.java).apply {
                putExtra(STARTING_POINT, FROM_INCOMING_CALL)
            }
            context.startActivity(intent)
        }
    }
}