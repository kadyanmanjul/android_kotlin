package com.joshtalks.joshskills

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.viewpager.widget.PagerAdapter
import kotlinx.coroutines.*
import java.net.MalformedURLException
import java.net.URL


class SliderImagesViewPager(private var listOfImages: List<SliderImage> = listOf()) : PagerAdapter() {


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
        val view = LayoutInflater.from(container.context).inflate(R.layout.item_buy_page_slider, container, false)
        val imageView = view.findViewById<AppCompatImageView>(R.id.image_slider)

        CoroutineScope(Dispatchers.IO).launch {
            downloadImage(listOfImages[position].imageUrl, imageView)
        }

        container.addView(view)
        return view
    }

    private suspend fun downloadImage(url: String?, imageview: ImageView) {
        var newurl: URL? = null
        try {
            newurl = URL(url)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        var bitmap: Bitmap? = null
        try {
            bitmap = BitmapFactory.decodeStream(newurl?.openConnection()?.getInputStream())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        withContext(Dispatchers.Main) {
            imageview.setImageBitmap(bitmap)
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }
}