package com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.Utils
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.model.SliderImage

class BuyPageViewPager(private var listOfImages: List<SliderImage> = listOf()) : PagerAdapter() {


    fun addListOfImages(images: List<SliderImage>) {
        Log.e("sagar", "addListOfImages: $images")
        if (listOfImages.isEmpty()) {
            listOfImages = images
        }
    }

    override fun getCount(): Int {
        Log.e("sagar", "getCount: ${listOfImages.size}" )
        return listOfImages.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(container.context).inflate(com.joshtalks.joshskills.R.layout.item_buy_page_slider, container, false)
        val imageView = view.findViewById<AppCompatImageView>(com.joshtalks.joshskills.R.id.image_slider)
        Glide.with(AppObjectController.joshApplication)
            .load(listOfImages[position].imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .fitCenter()
            .into(imageView)
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}