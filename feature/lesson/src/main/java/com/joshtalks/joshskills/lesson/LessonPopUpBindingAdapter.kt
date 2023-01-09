package com.joshtalks.joshskills.lesson

import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter

@BindingAdapter("bindSrcCompat")
fun bindSrcCompat(imageView: ImageView?, drawable: Int) {
    imageView?.setImageDrawable(ContextCompat.getDrawable(imageView.context,drawable))
}

@BindingAdapter("setCardBackgroundColor")
fun setCardBackgroundColor(cardView: CardView, color:Int){
    cardView.setCardBackgroundColor(ContextCompat.getColor(cardView.context,color))
}

@BindingAdapter("lessonTextColor")
fun lessonTextColor(textView: TextView, color: Int){
    textView.setTextColor(ContextCompat.getColor(textView.context,color))
}