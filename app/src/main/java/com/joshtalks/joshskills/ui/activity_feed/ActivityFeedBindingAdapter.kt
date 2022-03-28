package com.joshtalks.joshskills.ui.activity_feed

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponseFirebase
import de.hdodenhof.circleimageview.CircleImageView

@BindingAdapter("partialTextColor","fullText")
fun TextView.setColorize(subStringToColorize: String, fullText:String) {
    val spannable: Spannable = SpannableString(fullText)
    spannable.setSpan(
        ForegroundColorSpan(getColorHexCode()),
        0,
        subStringToColorize.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    spannable.setSpan(
        StyleSpan(Typeface.BOLD),
        0,
        subStringToColorize.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    setText(spannable, TextView.BufferType.SPANNABLE)
}

@BindingAdapter("imageResource")
fun CircleImageView.setImage(url: String?) {
    if (url.isNullOrEmpty()) {
        this.setImageResource(R.drawable.ic_call_placeholder)
    } else {
        val requestOptions = RequestOptions().placeholder(R.drawable.ic_call_placeholder)
            .error(R.drawable.ic_call_placeholder)
            .format(DecodeFormat.PREFER_RGB_565)
            .disallowHardwareConfig().dontAnimate().encodeQuality(75)
        Glide.with(AppObjectController.joshApplication)
            .load(url)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(requestOptions)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(this)
    }
}

@BindingAdapter("updatedImageResource")
fun ImageView.setImage(url: String?) {
    val requestOptions = RequestOptions().placeholder(R.drawable.ic_call_placeholder)
        .error(R.drawable.ic_call_placeholder)
        .format(DecodeFormat.PREFER_RGB_565)
        .disallowHardwareConfig().dontAnimate().encodeQuality(75)
    Glide.with(AppObjectController.joshApplication)
        .load(url)
        .optionalTransform(
            WebpDrawable::class.java,
            WebpDrawableTransformation(CircleCrop())
        )
        .apply(requestOptions)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .into(this)
}

fun getColorHexCode(): Int {
    val colorArray = arrayOf(
        "#f83a7e", "#2213fa", "#d5857a",
        "#706d45", "#63805a", "#b812bc",
        "#ee431b", "#f56fbe", "#721fde",
        "#953f30", "#ed9207", "#8d8eb4",
        "#78bcb2", "#3c6c9b", "#6ce172",
        "#4dc7b6", "#fe5b00", "#846fd2",
        "#755812", "#3b9c42", "#c2d542",
        "#a22b2f", "#cc794a", "#c20748",
        "#7a4ff8", "#163d52"
    )
    var colorInt = Color.parseColor(colorArray[++FirstTimeUser.idx % colorArray.size])
    return colorInt
}

@BindingAdapter("feedListAdapter", "onFeedItemClick")
fun setSeeAllRequestMemberAdapter(
    view: RecyclerView,
    adapter: ActivityFeedListAdapter,
    function: ((ActivityFeedResponseFirebase, Int) -> Unit)?
) {
    val layoutManager1 = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
    view.layoutManager = layoutManager1


    view.setHasFixedSize(false)
    view.adapter = adapter

    adapter.setListener(function)
}
