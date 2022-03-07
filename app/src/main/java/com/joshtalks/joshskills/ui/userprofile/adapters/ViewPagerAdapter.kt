package com.joshtalks.joshskills.ui.userprofile.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.ZoomageView
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import java.util.*
import kotlin.reflect.KFunction0

class ViewPagerAdapter(
    context: Context,
    private val images: Array<String> = arrayOf(),
    private val dismissAllowingStateLoss: KFunction0<Unit>,
    private var callback: AdapterCallback
) :
    PagerAdapter() {
    var mLayoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    override fun getCount(): Int {
        return images.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as LinearLayout
    }
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        callback.onSwipeCallback(position)
        val itemView: View = mLayoutInflater.inflate(R.layout.previous_pic_resource_layout, container, false)

        val imageView = itemView.findViewById<View>(R.id.imageViewMain) as ZoomageView
        val width = AppObjectController.screenWidth * .8
        val height = AppObjectController.screenHeight * .7

        val multi = MultiTransformation(
            RoundedCornersTransformation(
                Utils.dpToPx(ROUND_CORNER),
                8,
                RoundedCornersTransformation.CornerType.ALL
            )
        )
        imageView.doubleTapToZoom = true
        Glide.with(AppObjectController.joshApplication)
            .load(images[position])
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .override(width.toInt(), height.toInt())
            .apply(RequestOptions.bitmapTransform(multi))
            .into(imageView)
        imageView.setGestureDetectorInterface {
            dismissAllowingStateLoss()
        }



        Objects.requireNonNull(container).addView(itemView)
        return itemView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as LinearLayout)
    }
}
interface AdapterCallback {
    fun onSwipeCallback(position: Int)
}