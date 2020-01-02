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
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.course_explorer_view_holder)
class CourseExplorerViewHolder(private val courseExploreModel: CourseExploreModel) : BaseCell() {

    @View(R.id.image_view)
    lateinit var imageView: AppCompatImageView

    @View(R.id.buy_now_button)
    lateinit var buyNow: MaterialButton


    @Resolve
    fun onResolved() {

        Glide.with(getAppContext())
            .load(courseExploreModel.imageUrl)
            .override(Target.SIZE_ORIGINAL)
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
                    buyNow.visibility = android.view.View.VISIBLE

                    return false
                }

            }).into(imageView)

        buyNow.text = AppObjectController.getFirebaseRemoteConfig().getString("buy_course_label")

    }

    @Click(R.id.buy_now_button)
    fun onClick() {
        WorkMangerAdmin.buyNowEventWorker(courseExploreModel.testName)
        RxBus2.publish(courseExploreModel)
    }

    @Click(R.id.image_view)
    fun onClickImageView() {
        WorkMangerAdmin.buyNowImageEventWorker(courseExploreModel.testName)
        RxBus2.publish(courseExploreModel)
    }
}



