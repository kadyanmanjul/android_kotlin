package com.joshtalks.joshskills.ui.voip.new_arch.ui.utils

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.widget.Chronometer
import android.widget.GridLayout
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.BindingAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.core.textColorSet
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.adapter.TopicImageAdapter
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
            ContextCompat.getColorStateList(context, R.color.pure_black)
        }
        false -> {
            ContextCompat.getColorStateList(context, R.color.primary_800)
        }
    }
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

@BindingAdapter("setChips",)
fun GridLayout.setChips(interests : List<String>) {
    var index = 0
    this.removeAllViews()
    Log.d("ChipGroup.setChips", "setChips: $interests")
    for (interest in interests) {
        val chip =
            LayoutInflater.from(context).inflate(R.layout.common_interest_chip_item, null, false) as Chip
        chip.text = interest
        chip.id = index
        this.addView(chip)
        // this has to be done after adding the view
        chip.updateLayoutParams<GridLayout.LayoutParams> {
            setMargins(10,0,0,0)
        }
        if(index > 4)
            break
        index += 1
    }
}