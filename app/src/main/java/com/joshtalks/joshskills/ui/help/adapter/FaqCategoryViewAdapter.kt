package com.joshtalks.joshskills.ui.help.adapter

import android.graphics.drawable.PictureDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.FaqCategoryItemLayoutBinding
import com.joshtalks.joshskills.repository.server.FAQCategory
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener

class FaqCategoryViewAdapter(private val listFAQCategory: ArrayList<FAQCategory> = arrayListOf(), private val cardType: Int) :
    RecyclerView.Adapter<FaqCategoryViewAdapter.FaqCategoryHolder>() {
    private var onClickListener: ((hashmap:HashMap<String,Any>) -> Unit) = { _ -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqCategoryHolder {
        val binding = FaqCategoryItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FaqCategoryHolder(binding)
    }

    override fun getItemCount() = listFAQCategory.size

    override fun onBindViewHolder(holder: FaqCategoryHolder, position: Int) {
        holder.setData(listFAQCategory[position], cardType)
    }

    fun addListOfFAQ(faqCategory: List<FAQCategory>){
        if (listFAQCategory.isEmpty()) {
            listFAQCategory.addAll(faqCategory)
            notifyDataSetChanged()
        }
    }

    fun setOnClickListener(onClickListener: (hashmap:HashMap<String,Any>) -> Unit) {
        this.onClickListener = onClickListener
    }

    inner class FaqCategoryHolder(val binding: FaqCategoryItemLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setData(faqCategory: FAQCategory, cardType: Int) {
            binding.tvCategoryName.text = faqCategory.categoryName

            val map:HashMap<String,Any> = HashMap()
            map["category_data"] = faqCategory
            map["category_list"] = listFAQCategory
            binding.rootView.setOnSingleClickListener { onClickListener.invoke(map) }

            if (faqCategory.iconUrl.endsWith(".svg")) {

                val requestBuilder = GlideToVectorYou
                    .init()
                    .with(AppObjectController.joshApplication)
                    .requestBuilder
                requestBuilder.load(faqCategory.iconUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(RequestOptions().centerCrop())
                    .listener(object : RequestListener<PictureDrawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<PictureDrawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: PictureDrawable?,
                            model: Any?,
                            target: Target<PictureDrawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            resource?.let {
                                AppObjectController.uiHandler.post {
                                    binding.ivCategoryIcon.setImageDrawable(it)
                                }
                            }
                            return false
                        }

                    }).submit()

            } else {
                binding.ivCategoryIcon.setImage(faqCategory.iconUrl)
            }
            if (cardType != -1)
                setCardDefaultTint(cardType)
        }

        private fun setCardDefaultTint(cardType: Int) {
            if (cardType != 1) {
                TextViewCompat.setTextAppearance(
                    binding.tvCategoryName,
                    R.style.TextAppearance_JoshTypography_BodyRegular20
                )
                binding.rootView.strokeColor = ResourcesCompat.getColor(
                    AppObjectController.joshApplication.resources,
                    R.color.pure_white,
                    null
                )
            } else {
                TextViewCompat.setTextAppearance(
                    binding.tvCategoryName,
                    R.style.TextAppearance_JoshTypography_Body_Text_Small_Bold
                )
                binding.rootView.strokeColor = ResourcesCompat.getColor(
                    AppObjectController.joshApplication.resources,
                    R.color.primary_500,
                    null
                )
            }
            binding.rootView.setCardBackgroundColor(
                ResourcesCompat.getColor(
                    AppObjectController.joshApplication.resources,
                    R.color.pure_white,
                    null
                )
            )
        }
    }
}