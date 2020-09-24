package com.joshtalks.joshskills.ui.newonboarding.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.FragmentCourseEnrolledDetailItemBinding
import com.joshtalks.joshskills.repository.server.onboarding.CourseContent

class CourseEnrolledDetailAdapter(
) :
    RecyclerView.Adapter<CourseEnrolledDetailAdapter.ViewHolder>() {
    private var contentList= ArrayList<CourseContent>()

    fun setContent(categoryModel: ArrayList<CourseContent>) {
        contentList.addAll(categoryModel)
        notifyDataSetChanged()
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CourseEnrolledDetailAdapter.ViewHolder {
        val binding = FragmentCourseEnrolledDetailItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding).apply {
            setIsRecyclable(false)
        }
    }

    override fun getItemCount(): Int = contentList.size

    override fun onBindViewHolder(holder: CourseEnrolledDetailAdapter.ViewHolder, position: Int) {
        holder.bind(contentList[position])
    }

    inner class ViewHolder(val binding: FragmentCourseEnrolledDetailItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(content: CourseContent) {
            Glide.with(AppObjectController.joshApplication)
                .load(content.thumbnail)
                .override(binding.image.width, binding.image.height)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .into(binding.image)
            binding.detail.text = content.text
            binding.category.text = content.heading
        }
    }
}
