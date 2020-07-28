package com.joshtalks.joshskills.ui.explore

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
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

class CourseExploreAdapter(var context: Context, var courseList: ArrayList<CourseExploreModel>) :
    RecyclerView.Adapter<CourseExploreAdapter.CourseExploreViewHolder>() {

    class CourseExploreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: AppCompatImageView
        var buyNow: MaterialButton

        init {
            imageView = itemView.findViewById(R.id.image_view)
            buyNow = itemView.findViewById(R.id.buy_now_button)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseExploreViewHolder {
        return CourseExploreViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.course_explorer_view_holder, parent, false)
        )
    }


    override fun getItemCount(): Int {
        return courseList.size
    }


    override fun onBindViewHolder(holder: CourseExploreViewHolder, position: Int) {
        val courseExploreModel = courseList.get(position)
        Glide.with(context)
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
                    holder.buyNow.visibility = View.VISIBLE

                    return false
                }

            }).into(holder.imageView)

        holder.buyNow.text =
            AppObjectController.getFirebaseRemoteConfig().getString("show_details_label")

        holder.buyNow.setOnClickListener {
            WorkMangerAdmin.buyNowEventWorker(courseExploreModel.testName)
            RxBus2.publish(courseExploreModel)
        }

        holder.imageView.setOnClickListener {
            WorkMangerAdmin.buyNowImageEventWorker(courseExploreModel.testName)
            RxBus2.publish(courseExploreModel)
        }
    }

}