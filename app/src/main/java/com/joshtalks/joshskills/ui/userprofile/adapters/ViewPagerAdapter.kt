package com.joshtalks.joshskills.ui.userprofile.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.PreviousPicResourceLayoutBinding
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import java.util.*
import kotlin.reflect.KFunction0

class ViewPagerAdapter(
    context: Context,
    private val images: Array<String> = arrayOf(),
    private val dismissAllowingStateLoss: KFunction0<Unit>) :
    RecyclerView.Adapter<ViewPagerAdapter.ViewHolder>() {
    var mLayoutInflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getItemCount() = images.size

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewPagerAdapter.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        val binding = PreviousPicResourceLayoutBinding.inflate(inflater, parent, false)
        return ViewHolder(binding, parent.context)
    }

    override fun onBindViewHolder(holder: ViewPagerAdapter.ViewHolder, position: Int) {
        return holder.bind(images[position])
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class ViewHolder(val binding: PreviousPicResourceLayoutBinding, val context: Context) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(image: String) {
            val imageView = binding.imageViewMain
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
                .load(image)
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
        }
    }
}