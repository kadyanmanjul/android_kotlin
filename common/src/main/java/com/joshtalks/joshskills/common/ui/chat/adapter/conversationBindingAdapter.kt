package com.joshtalks.joshskills.common.ui.chat.adapter

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.joshtalks.joshskills.common.R

@BindingAdapter(value = ["chatScreenBackground"], requireAll = false)
fun chatScreenBackground(imageView: ImageView, image: Int) {
    try {
        if (image==0) {
            imageView.setBackgroundResource(R.color.disabled)
        } else {
            imageView.setImageResource(image)
        }
    } catch (e: Exception) {
        imageView.setBackgroundResource(R.color.disabled)
        e.printStackTrace()
    } catch (e: OutOfMemoryError) {
        imageView.setBackgroundResource(R.color.disabled)
        e.printStackTrace()
    }
}