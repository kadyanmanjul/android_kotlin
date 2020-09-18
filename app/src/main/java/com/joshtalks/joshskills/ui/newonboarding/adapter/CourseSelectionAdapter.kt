package com.joshtalks.joshskills.ui.newonboarding.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.CourseSelectionViewHolderBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.CourseSelectedEventBus
import com.joshtalks.joshskills.repository.server.CourseExploreModel

class CourseSelectionAdapter(
    private var courseList: List<CourseExploreModel>,
    private var isSecondFlow: Boolean
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val context = AppObjectController.joshApplication

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CourseSelectionViewHolderBinding.inflate(inflater, parent, false)
        return CourseSelectViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return courseList.size
    }

    override fun getItemViewType(position: Int): Int {
        return courseList[position].cardType.ordinal
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CourseSelectViewHolder) {
            (holder).also {
                it.bind(courseList[position], position)
            }
        }
    }

    inner class CourseSelectViewHolder(val binding: CourseSelectionViewHolderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(courseExploreModel: CourseExploreModel, position: Int) {
            if (isSecondFlow) {
                binding.selectCourse.visibility = View.GONE
                binding.alfa.visibility = View.GONE
                binding.rootView.setOnClickListener {
                    RxBus2.publish(
                        CourseSelectedEventBus(
                            flag = courseExploreModel.isSelected,
                            id = courseExploreModel.id,
                            whatsappLink = courseExploreModel.whatsappUrl,
                            isAlreadyEnrolled = true
                        )
                    )
                }
            } else {
                binding.selectCourse.setOnClickListener {
                    setText(courseExploreModel, position)
                }

                binding.rootView.setOnClickListener {
                    setText(courseExploreModel, position)
                }

                setSelectionState(courseExploreModel)
            }
            Glide.with(context)
                .load(courseExploreModel.imageUrl)
                .override(binding.imageView.width, binding.imageView.height)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .into(binding.imageView)
        }

        private fun setSelectionState(courseExploreModel: CourseExploreModel) {
            if (courseExploreModel.isSelected) {
                binding.selectCourse.text = context.getString(R.string.selected)
                binding.alfa.visibility = View.VISIBLE
                setDrawableStart()
                setBackgroundColor(R.color.green)
            } else {
                binding.selectCourse.text = context.getString(R.string.select_course)
                binding.alfa.visibility = View.GONE
                setDrawableStart(null)
                setBackgroundColor(R.color.button_color)
            }
        }

        fun setText(courseExploreModel: CourseExploreModel, position: Int) {
            if (courseExploreModel.isSelected) {
                binding.selectCourse.text = context.getString(R.string.select_course)
                binding.alfa.visibility = View.GONE
                setDrawableStart(null)
                setBackgroundColor(R.color.button_color)
                courseExploreModel.isSelected = false

            } else {
                binding.selectCourse.text = context.getString(R.string.selected)
                binding.alfa.visibility = View.VISIBLE
                setDrawableStart()
                setBackgroundColor(R.color.green)
                courseExploreModel.isSelected = true
            }
            notifyDataSetChanged()
            RxBus2.publish(
                CourseSelectedEventBus(
                    flag = courseExploreModel.isSelected,
                    id = courseExploreModel.id,
                    whatsappLink = courseExploreModel.whatsappUrl
                )
            )
        }

        private fun setDrawableStart(
            drawableId: Int? = R.drawable.ic_tick_extra_smallest,
            tintColorId: Int = R.color.white
        ) {
            val drawable = drawableId?.let {
                ContextCompat.getDrawable(
                    context,
                    it
                )
            }
            drawable?.setTint(
                ContextCompat.getColor(
                    context,
                    tintColorId
                )
            )
            drawable?.setBounds(4, 0, 4, 0)

            binding.selectCourse.setCompoundDrawablesWithIntrinsicBounds(
                drawable, null, null, null
            )
        }

        private fun setBackgroundColor(colorId: Int) = binding.selectCourse.setBackgroundColor(
            ContextCompat.getColor(
                context,
                colorId
            )
        )
    }

}
