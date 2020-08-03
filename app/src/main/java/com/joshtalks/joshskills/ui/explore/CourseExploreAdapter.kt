package com.joshtalks.joshskills.ui.explore

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.server.CourseExploreModel

class CourseExploreAdapter(
    var context: Context,
    var courseList: ArrayList<CourseExploreModel>
) :
    RecyclerView.Adapter<CourseExploreAdapter.CourseExploreViewHolder>() {

    //Will be true if user has applied filter. We will show language tags between course list on its basis.
    var isFilterEnabled: Boolean = false

    class CourseExploreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: AppCompatImageView = itemView.findViewById(R.id.image_view)
        var buyNow: MaterialButton = itemView.findViewById(R.id.buy_now_button)
        var languageTag: TextView = itemView.findViewById(R.id.language_tag)
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
        if (isFilterEnabled && position != 0 && !courseExploreModel.language.equals(
                courseList.get(position - 1).language, true
            )
        ) {
            holder.languageTag.visibility = VISIBLE
            holder.languageTag.text = courseExploreModel.language?.capitalize()
        } else
            holder.languageTag.visibility = GONE

        holder.buyNow.text =
            AppObjectController.getFirebaseRemoteConfig().getString("show_details_label")

        holder.buyNow.setOnClickListener {
            RxBus2.publish(courseExploreModel)
        }

        holder.imageView.setOnClickListener {
            RxBus2.publish(courseExploreModel)
        }
        try {
            Glide.with(context)
                .load(courseExploreModel.imageUrl)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .into(holder.imageView)

        } catch (ex: Exception) {
        }
    }

}