package com.joshtalks.joshskills.ui.view_holders

import android.graphics.drawable.Drawable
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.model.CourseExploreModel
import com.mindorks.placeholderview.annotations.*

@Layout(R.layout.course_explorer_view_holder)
class CourseExplorerViewHolder( private val courseExploreModel: CourseExploreModel) :BaseCell()  {

    @View(R.id.image_view)
    lateinit var imageView: AppCompatImageView

    @View(R.id.buy_now_button)
    lateinit var buyNow: MaterialButton



    @Resolve
    fun onResolved() {

        Glide.with(getAppContext())
            .load(courseExploreModel.imageUrl)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false

                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    buyNow.visibility=android.view.View.VISIBLE

                    return false
                }

            }).into(imageView)




    }

    @Click(R.id.buy_now_button)
    fun onClick() {
        RxBus2.publish(courseExploreModel)
    }


}



