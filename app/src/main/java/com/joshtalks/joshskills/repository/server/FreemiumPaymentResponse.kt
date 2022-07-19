package com.joshtalks.joshskills.repository.server


import android.content.Intent
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.databinding.BindingAdapter
import com.google.android.material.textview.MaterialTextView
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity

data class FreemiumPaymentResponse(
    @SerializedName("test_id")
    val testId: String?,

    @SerializedName("actual_amount")
    val actualAmount: String?,

    @SerializedName("discounted_amount")
    val discountedAmount: String?,

    @SerializedName("heading")
    val heading: String?,

    @SerializedName("button_text")
    val buttonText: String?,

    @SerializedName("encrypted_text")
    val encryptedText: String?,

    @SerializedName("sub_heading")
    val featureList: List<FreemiumPaymentFeature>,

    @SerializedName("coupon_details")
    val couponDetails: CouponDetails?,

    @SerializedName("course_name")
    val courseName: String?,

    @SerializedName("teacher_name")
    val teacherName: String?,

    @SerializedName("image_url")
    val imageUrl: String?,
)

data class FreemiumPaymentFeature(
    @SerializedName("feature")
    val feature: String?,

    @SerializedName("feature_desc")
    var featureDesc: String?,

    @SerializedName("feature_link")
    var featureLink: FreemiumFeatureLink? = null,

    @SerializedName("freemium")
    val freemium: String,

    @SerializedName("premium")
    val premium: String
) {

    fun showFreemiumIcon(): Boolean? = freemium.toBooleanStrictOrNull()
    fun showPremiumIcon(): Boolean? = premium.toBooleanStrictOrNull()

}

enum class FreemiumFeatureLink(val text: String) {
    @SerializedName("Explore Courses")
    EXPLORE_COURSES("Explore Courses"), ;
}

@BindingAdapter("onFeatureLinkClick")
fun setOnFeatureLinkClick(textView: MaterialTextView, featureLink: FreemiumFeatureLink? = null) {
    if (featureLink == null) return
    val spannableString = SpannableString(featureLink.text)
    spannableString.setSpan(object : ClickableSpan() {
        override fun onClick(widget: View) {
            when (featureLink) {
                FreemiumFeatureLink.EXPLORE_COURSES -> {
                    Intent(textView.context, CourseExploreActivity::class.java).also {
                        textView.context.startActivity(it)
                    }
                }
            }
        }
    }, 0, featureLink.text.length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
    textView.text = spannableString
    textView.movementMethod = LinkMovementMethod.getInstance();
}