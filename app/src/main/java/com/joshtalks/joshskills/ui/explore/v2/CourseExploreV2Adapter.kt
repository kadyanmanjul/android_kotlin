package com.joshtalks.joshskills.ui.explore.v2

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.CourseExplorerViewHolderV2Binding
import com.joshtalks.joshskills.repository.server.course_detail.RecyclerViewCarouselItemDecorator
import com.joshtalks.joshskills.repository.server.course_recommend.Segment
import jp.wasabeef.recyclerview.animators.FadeInLeftAnimator

class CourseExploreV2Adapter(private var segmentList: ArrayList<Segment>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CourseExplorerViewHolderV2Binding.inflate(inflater, parent, false)
        return CourseExploreViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return segmentList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CourseExploreViewHolder) {
            (holder).also {
                it.bind(segmentList[position])
            }
        }
    }

    inner class CourseExploreViewHolder(val binding: CourseExplorerViewHolderV2Binding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.recyclerView.setHasFixedSize(true)
            binding.recyclerView.itemAnimator?.apply {
                addDuration = 2000
                changeDuration = 2000
            }
            binding.recyclerView.itemAnimator = FadeInLeftAnimator(OvershootInterpolator(2f))
            binding.recyclerView.layoutManager = LinearLayoutManager(
                AppObjectController.joshApplication,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            val cardWidthPixels =
                (AppObjectController.joshApplication.resources.displayMetrics.widthPixels * 0.90f).toInt()
            val cardHintPercent = 0.01f
            binding.recyclerView.addItemDecoration(
                RecyclerViewCarouselItemDecorator(
                    AppObjectController.joshApplication,
                    cardWidthPixels,
                    cardHintPercent
                )
            )
        }

        fun bind(segment: Segment) {
            with(binding) {
                if (recyclerView.adapter == null) {
                    headerTv.text = segment.name
                    binding.recyclerView.adapter = RecommendedItemAdapter(segment.courseList)
                }
            }
        }
    }
}