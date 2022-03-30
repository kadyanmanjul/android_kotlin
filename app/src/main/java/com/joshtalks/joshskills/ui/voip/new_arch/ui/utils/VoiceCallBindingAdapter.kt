package com.joshtalks.joshskills.ui.voip.new_arch.ui.utils

import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableBoolean
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallType
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
fun ConstraintLayout.setCallBackground(callType: CallType?) {
    if (callType != null) {
        when (callType) {
            is CallType.FavoritePracticePartner -> {
                this.setBackgroundResource(R.drawable.voip_bg)
            }
            is CallType.NormalPracticePartner -> {
                this.setBackgroundResource(R.drawable.voip_bg)

            }
            is CallType.GroupCall -> {
                this.setBackgroundResource(R.drawable.voip_bg)

            }
        }
    }
}

@BindingAdapter("setSpeakerImage")
fun AppCompatImageButton.setSpeakerImage(isSpeakerOn: ObservableBoolean) {
    if (isSpeakerOn.get()) {
        this.setImageResource(R.drawable.ic_baseline_speaker_on)
    } else {
        this.setImageResource(R.drawable.ic_baseline_speaker_mute)
    }
}

@BindingAdapter("setMicImage")
fun AppCompatImageButton.setMicImage(isMute: ObservableBoolean) {
    if (isMute.get()) {
        this.setImageResource(R.drawable.ic_baseline_mic_off)
    } else {
        this.setImageResource(R.drawable.ic_new_mic)
    }
}
