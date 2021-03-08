package com.joshtalks.joshskills.ui.gif

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.USER_PROFILE_FLOW_FROM
import com.joshtalks.joshskills.databinding.ActivityGifBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GIFActivity : CoreJoshActivity() {
    private lateinit var binding: ActivityGifBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_gif)
        binding.lifecycleOwner = this
        binding.handler = this
        Glide.with(this)
            .load(R.raw.boom_new)
            .into(binding.imageGif)
        CoroutineScope(Dispatchers.Main).launch {
            binding.image.animationLeftToRight()
            delay(10)
            binding.text.animationLeftToRight()
            delay(10)
            binding.appCompatTextView2.animationLeftToRight()
            delay(10)
            binding.btnProfile.animationBottomToTop()
        }
    }

    fun View.animationLeftToRight(){
        val animSet = AnimationSet(false)
        animSet.fillAfter = false
        animSet.duration = 700
        animSet.interpolator = LinearInterpolator()
        val translate = TranslateAnimation(
            Animation.ABSOLUTE,  //from xType
            +500f,
            Animation.ABSOLUTE,  //to xType
            0f,
            Animation.ABSOLUTE,  //from yType
            0f,
            Animation.ABSOLUTE,  //to yType
           0f
        )
        animSet.addAnimation(translate)
        this.startAnimation(animSet)
    }

    fun View.animationBottomToTop(){
        val animSet = AnimationSet(false)
        animSet.fillAfter = false
        animSet.duration = 700
        animSet.interpolator = LinearInterpolator()
        val translate = TranslateAnimation(
            Animation.ABSOLUTE,  //from xType
            0f,
            Animation.ABSOLUTE,  //to xType
            0f,
            Animation.ABSOLUTE,  //from yType
            +200f,
            Animation.ABSOLUTE,  //to yType
            0f
        )
        animSet.addAnimation(translate)
        this.startAnimation(animSet)
    }

    fun exit() {
        finish()
    }

    fun goToProfile() {
        UserProfileActivity.startUserProfileActivity(
            this, Mentor.getInstance().getId(),
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            null, USER_PROFILE_FLOW_FROM.AWARD.value
        )
    }
}
