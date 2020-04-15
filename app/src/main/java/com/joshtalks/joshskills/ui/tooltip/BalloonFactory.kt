package com.joshtalks.joshskills.ui.tooltip

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.COURSE_OFFER
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.skydoves.balloon.*
import io.github.inflationx.calligraphy3.TypefaceUtils

object BalloonFactory {

    fun getFirstTimeUserBalloon(
        baseContext: Context,
        lifecycleOwner: LifecycleOwner,
        onBalloonClickListener: OnBalloonClickListener,
        onBalloonDismissListener: OnBalloonDismissListener
    ): Balloon {
        val text = baseContext.getString(R.string.first_time_user_hint)
        val typefaceSpan = TypefaceUtils.load(baseContext.assets, "fonts/Roboto-Medium.ttf")
        val textForm: TextForm = TextForm.Builder(baseContext)
            .setText(text)
            .setTextColorResource(R.color.gray_53)
            .setTextSize(12f)
            .setTextTypeface(typefaceSpan)
            .build()

        return Balloon.Builder(baseContext)
            .setTextForm(textForm)
            .setArrowSize(10)
            .setWidthRatio(0.85f)
            .setArrowOrientation(ArrowOrientation.TOP)
            .setArrowVisible(true)
            .setHeight(72)
            .setTextSize(12f)
            .setArrowPosition(0.8f)
            .setCornerRadius(8f)
            //.setAlpha(0.9f)
            .setSpace(8)
            .setBackgroundColorResource(R.color.white)
            .setOnBalloonClickListener(onBalloonClickListener)
            .setOnBalloonDismissListener(onBalloonDismissListener)
            .setDismissWhenShowAgain(true)
            .setBalloonAnimation(BalloonAnimation.FADE)
            .setLifecycleOwner(lifecycleOwner)
            .setDismissWhenClicked(true)
            .setDismissWhenTouchOutside(true)
            .build()
    }


    fun hintOfferFirstTime(
        baseContext: Context,
        lifecycleOwner: LifecycleOwner, onBalloonDismissListener: OnBalloonDismissListener
    ): Balloon {
        var userName = "User"
        try {
            if (User.getInstance().firstName.isNotEmpty()) {
                userName = User.getInstance().firstName.capitalize()
            }
        } catch (ex: NullPointerException) {

        }
        val text =
            baseContext.getString(R.string.find_more_course_hint_ftime, userName, COURSE_OFFER)
        val typefaceSpan = TypefaceUtils.load(baseContext.assets, "fonts/Roboto-Medium.ttf")
        val textForm: TextForm = TextForm.Builder(baseContext)
            .setText(text)
            .setTextColorResource(R.color.white)
            .setTextSize(12f)
            .setTextTypeface(typefaceSpan)
            .build()

        return Balloon.Builder(baseContext)
            .setTextForm(textForm)
            .setArrowSize(10)
            .setWidthRatio(0.85f)
            .setArrowOrientation(ArrowOrientation.TOP)
            .setArrowVisible(true)
            .setHeight(80)
            .setTextSize(12f)
            .setArrowPosition(0.5f)
            .setCornerRadius(8f)
            // .setAlpha(0.9f)
            .setSpace(8)
            .setDismissWhenShowAgain(false)
            .setOnBalloonDismissListener(onBalloonDismissListener)
            .setBackgroundColorResource(R.color.gray_53)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(lifecycleOwner)
            .setDismissWhenClicked(false)
            .setDismissWhenTouchOutside(false)
            .build()
    }


    @SuppressLint("DefaultLocale")
    fun offerIn7Days(
        baseContext: Context,
        lifecycleOwner: LifecycleOwner
    ): Balloon {
        var userName = "User"
        try {
            if (User.getInstance().firstName.isNotEmpty()) {
                userName = User.getInstance().firstName.capitalize()
            }
        } catch (ex: NullPointerException) {

        }
        val text =
            baseContext.getString(R.string.find_more_course_hint_ftime, userName, COURSE_OFFER)
        val typefaceSpan = TypefaceUtils.load(baseContext.assets, "fonts/Roboto-Medium.ttf")
        val textForm: TextForm = TextForm.Builder(baseContext)
            .setText(text)
            .setTextColorResource(R.color.white)
            .setTextSize(14f)
            .setTextTypeface(typefaceSpan)
            .build()

        return Balloon.Builder(baseContext)
            .setTextForm(textForm)
            .setArrowSize(10)
            .setWidthRatio(0.85f)
            .setArrowOrientation(ArrowOrientation.TOP)
            .setArrowVisible(true)
            .setHeight(102)
            .setArrowPosition(0.5f)
            .setCornerRadius(8f)
            //.setAlpha(0.9f)
            .setSpace(8)
            .setBackgroundColorResource(R.color.gray_53)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(lifecycleOwner)
            .setDismissWhenClicked(false)
            .setDismissWhenTouchOutside(false)
            .build()
    }


    fun getCourseOfferBalloon(
        baseContext: Context,
        remainDay: String,
        lifecycleOwner: LifecycleOwner,
        onBalloonClickListener: OnBalloonClickListener
    ): Balloon {

        val text = baseContext.getString(
            R.string.buy_course_offer_tooltip,
            COURSE_OFFER,
            remainDay
        )
        val typefaceSpan = TypefaceUtils.load(baseContext.assets, "fonts/Roboto-Medium.ttf")
        val textForm: TextForm = TextForm.Builder(baseContext)
            .setText(text)
            .setTextColorResource(R.color.gray_53)
            .setTextSize(13f)
            .setTextTypeface(typefaceSpan)
            .build()

        return Balloon.Builder(baseContext)
            .setTextForm(textForm)
            .setArrowSize(10)
            .setArrowVisible(true)
            .setWidthRatio(0.75f)
            .setArrowOrientation(ArrowOrientation.BOTTOM)
            .setArrowVisible(true)
            .setHeight(70)
            .setArrowPosition(0.75f)
            .setCornerRadius(4f)
            .setSpace(8)
            .setBackgroundColorResource(R.color.controls_panel_stroke)
            .setOnBalloonClickListener(onBalloonClickListener)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(lifecycleOwner)
            .setDismissWhenClicked(true)
            .build()
    }

    fun getTooltipForCertificate(
        baseContext: Context, lifecycleOwner: LifecycleOwner, value: OnBalloonDismissListener
    ): Balloon {

        val per =
            AppObjectController.getFirebaseRemoteConfig().getString("COURSE_COMPLETED_PERCENTAGE")
        val text = baseContext.getString(R.string.certificate_tooltip_hint, per)
        val typefaceSpan = TypefaceUtils.load(baseContext.assets, "fonts/Roboto-Regular.ttf")
        val textForm: TextForm = TextForm.Builder(baseContext)
            .setText(text)
            .setTextColorResource(R.color.gray_53)
            .setTextSize(11f)
            .setTextTypeface(typefaceSpan)
            .build()

        return Balloon.Builder(baseContext)
            .setTextForm(textForm)
            .setArrowSize(10)
            .setArrowVisible(true)
            .setWidthRatio(0.85f)
            .setArrowOrientation(ArrowOrientation.BOTTOM)
            .setArrowVisible(true)
            .setHeight(64)
            .setDismissWhenTouchOutside(true)
            .setCornerRadius(4f)
            .setBackgroundColorResource(R.color.controls_panel_stroke)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(lifecycleOwner)
            .setDismissWhenClicked(true)
            .setOnBalloonDismissListener(value)
            .build()
    }


}