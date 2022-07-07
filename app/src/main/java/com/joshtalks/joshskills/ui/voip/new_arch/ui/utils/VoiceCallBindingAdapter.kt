package com.joshtalks.joshskills.ui.voip.new_arch.ui.utils

import android.content.Intent
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import android.widget.Chronometer
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableBoolean
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.FPP
import com.joshtalks.joshskills.base.constants.FROM_INCOMING_CALL
import com.joshtalks.joshskills.base.constants.GROUP
import com.joshtalks.joshskills.base.constants.PEER_TO_PEER
import com.joshtalks.joshskills.base.constants.STARTING_POINT
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.adapter.TopicImageAdapter
import de.hdodenhof.circleimageview.CircleImageView
import jp.wasabeef.glide.transformations.BlurTransformation


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

@BindingAdapter("setColorLocalUser")
fun AppCompatTextView.setColorLocalUser(isSpeaking : Boolean = false) {
    if (isSpeaking){
        this.setTextColor(resources.getColor(R.color.green))
    }
    else
        this.setTextColor(resources.getColor(R.color.white))
}

@BindingAdapter("setColorRemoteUser")
fun AppCompatTextView.setColorRemoteUser(isSpeaking : Boolean = false) {
    if (isSpeaking){
        this.setTextColor(resources.getColor(R.color.p2p_circle_yellow))
    }
    else
        this.setTextColor(resources.getColor(R.color.white))
}

@BindingAdapter("setCallBackground")
fun ConstraintLayout.setCallBackground(callType: Int) {
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
        else -> {
            this.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        }
    }
}

@BindingAdapter("setSpeakerImage")
fun AppCompatImageButton.setSpeakerImage(isSpeakerOn: Boolean) {
    if (!isSpeakerOn) {
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
fun AppCompatImageButton.setMicImage(isMute: Boolean) {
    if (!isMute) {
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

@BindingAdapter("recordTimeStarts")
fun Chronometer.recordTimeStarts(b: Boolean) {
    if (b) {
        this.base = SystemClock.elapsedRealtime()
        this.start()
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

@BindingAdapter("setViewPagerAdapter")
fun ViewPager2.setViewPagerAdapter(image: String?) {
    val imageList = ArrayList<String>()
    if (image != null) {
        imageList.add(image)
    }
    if (!imageList.isNullOrEmpty()) {
        val adapter = TopicImageAdapter(imageList, context)
        this.adapter = adapter
    }
}

@BindingAdapter("setBottomCardBackground")
fun MaterialCardView.setBottomCardBackground(callType: Boolean) {
    backgroundTintList = when (callType) {
        true -> {
            ContextCompat.getColorStateList(context, R.color.black_quiz)
        }
        false -> {
            ContextCompat.getColorStateList(context, R.color.bottom_sheet_color)
        }
    }
}

@BindingAdapter("setBackgroundForLocalUser")
fun ImageView.setBackgroundForLocalUser(imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty())
        Glide.with(this).load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .apply(bitmapTransform(BlurTransformation(24)))
            .into(this)
    else
        Glide.with(this)
            .load(R.drawable.local_user_default_image)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(this)
}

@BindingAdapter("setBackgroundForRemoteUser")
fun ImageView.setBackgroundForRemoteUser(imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty())
        Glide.with(this).load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .apply(bitmapTransform(BlurTransformation(24)))
            .into(this)
    else
        Glide.with(this)
            .load(R.drawable.remote_user_image)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(this)
}


@BindingAdapter("loadGif")
fun ImageView.loadGif(imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty())
        Glide.with(this).load(imageUrl).into(DrawableImageViewTarget(this))
    else
        Glide.with(this)
            .load(R.drawable.remote_user_image)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(this)
}

@BindingAdapter("setWord","setColor")
fun AppCompatTextView.setWord(word: String?,color:String?) {
    if (!word.isNullOrEmpty()){
        this.text = word
    }
    if (!color.isNullOrEmpty() && color != ""){
        Log.d("naman", "setWord: $color ")
        this.setTextColor(Color.parseColor(color))
    }
}

@BindingAdapter("nextWordBtn")
fun MaterialButton.nextWordBtn(isEnabled : Boolean?) {
    if (isEnabled != null) {
        when (isEnabled) {
            true -> {
                this.backgroundTintList = ContextCompat.getColorStateList(context, R.color.grey)
                this.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
            false -> {
                this.backgroundTintList = ContextCompat.getColorStateList(context, R.color.white)
                this.setTextColor(ContextCompat.getColor(context, R.color.p2p_game_dark_purple))
            }
        }
    }
}

@BindingAdapter("setPlayBtnBackground")
fun AppCompatTextView.setPlayBtnBackground(isEnabled: Boolean?) {
    if (isEnabled != null) {
        when (isEnabled) {
            false -> {
                this.backgroundTintList = null
            }
            true -> {
                this.backgroundTintList = ContextCompat.getColorStateList(context, R.color.grey)
            }
        }
    }
}