package com.joshtalks.joshskills.ui.voip.new_arch.ui.utils

import android.graphics.Color
import android.os.SystemClock
import android.view.View
import android.widget.Chronometer
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableBoolean
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.FPP
import com.joshtalks.joshskills.base.constants.GROUP
import com.joshtalks.joshskills.base.constants.PEER_TO_PEER
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
fun ConstraintLayout.setCallBackground(callType: Int) {
    if (callType>0) {
        when (callType) {
             PEER_TO_PEER -> {
//                 Normal Call
                 this.setBackgroundColor(resources.getColor(R.color.colorPrimary))
             }
             FPP -> {
//                 FPP
                 this.setBackgroundResource(R.drawable.voip_bg)
             }
             GROUP -> {
//                 Group Call
                 this.setBackgroundColor(resources.getColor(R.color.colorPrimary))
            }
        }
    }
}

@BindingAdapter("setSpeakerImage")
fun AppCompatImageButton.setSpeakerImage(isSpeakerOn: ObservableBoolean) {
    if (!isSpeakerOn.get()) {
        this.backgroundTintList =
            ContextCompat.getColorStateList(context, R.color.dis_color_10f)
        this.imageTintList = ContextCompat.getColorStateList(context, R.color.white)
    } else {
        this.backgroundTintList =
            ContextCompat.getColorStateList(context, R.color.white)
        this.imageTintList =
            ContextCompat.getColorStateList(context, R.color.grey_61)
    }
}

@BindingAdapter("setMicImage")
fun AppCompatImageButton.setMicImage(isMute: ObservableBoolean) {
    if (!isMute.get()) {
        this.backgroundTintList =
            ContextCompat.getColorStateList(context, R.color.dis_color_10f)
        this.imageTintList = ContextCompat.getColorStateList(context, R.color.white)
    } else {
        this.backgroundTintList =
            ContextCompat.getColorStateList(context, R.color.white)
        this.imageTintList =
            ContextCompat.getColorStateList(context, R.color.grey_61)    }
}

@BindingAdapter("startTimer")
fun Chronometer.startTimer(baseTime: Long) {
    base = SystemClock.elapsedRealtime() - baseTime
    start()
}