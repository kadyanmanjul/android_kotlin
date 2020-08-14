package com.joshtalks.joshskills.ui.explore

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.custom_ui.decorator.StickHeaderItemDecoration
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.CourseExplorerViewHolderBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.server.CourseExploreModel


class CourseExploreAdapter(var courseList: ArrayList<CourseExploreModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    StickHeaderItemDecoration.StickyHeaderInterface {
    private val context = AppObjectController.joshApplication

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == ExploreCardType.NORMAL.ordinal) {
            val binding = CourseExplorerViewHolderBinding.inflate(inflater, parent, false)
            CourseExploreViewHolder(binding)
        } else {
            val binding = CourseExplorerViewHolderBinding.inflate(inflater, parent, false)
            CourseExploreViewHolder(binding)
        }
    }

    override fun getItemCount(): Int {
        return courseList.size
    }

    override fun getItemViewType(position: Int): Int {
        return courseList[position].cardType.ordinal
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CourseExploreViewHolder) {
            (holder).also {
                it.bind(courseList[position])
            }
        }
    }

    override fun isHeader(itemPosition: Int): Boolean {
        return courseList[itemPosition].cardType.ordinal != ExploreCardType.NORMAL.ordinal
    }

    override fun getHeaderLayout(headerPosition: Int): Int {
        return R.layout.course_explorer_view_holder
    }

    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        var headerPosition = -1
        var position = itemPosition
        do {
            if (isHeader(position)) {
                headerPosition = itemPosition
                break
            }
            position -= 1
        } while (position >= 0)
        return headerPosition
    }

    override fun bindHeaderData(header: View, headerPosition: Int) {
    }


    inner class CourseExploreViewHolder(val binding: CourseExplorerViewHolderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(courseExploreModel: CourseExploreModel) {
            with(binding) {
                if (courseExploreModel.isClickable) {
                    buyNowButton.visibility = VISIBLE
                    buyNowButton.text =
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString("show_details_label")

                    buyNowButton.setOnClickListener {
                        RxBus2.publish(courseExploreModel)
                    }
                    rootView.setOnClickListener {
                        RxBus2.publish(courseExploreModel)
                    }
                } else {
                    buyNowButton.visibility = GONE
                    buyNowButton.setOnClickListener {
                        showToast(
                            AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.FFCOURSE_CARD_CLICK_MSG)
                        )
                    }

                    rootView.setOnClickListener {
                        showToast(
                            AppObjectController.getFirebaseRemoteConfig()
                                .getString(FirebaseRemoteConfigKey.FFCOURSE_CARD_CLICK_MSG)
                        )
                    }
                }

                Glide.with(context)
                    .load(courseExploreModel.imageUrl)
                    .override(imageView.width, imageView.height)
                    .optionalTransform(
                        WebpDrawable::class.java,
                        WebpDrawableTransformation(CircleCrop())
                    )
                    .into(imageView)
            }
        }
    }

}
