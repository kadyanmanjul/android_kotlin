package com.joshtalks.joshskills.ui.group.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import com.bumptech.glide.Glide
import com.joshtalks.joshskills.R
import de.hdodenhof.circleimageview.CircleImageView
import java.lang.Exception

class GroupsAppBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    val titleTv : TextView by lazy {
        this.findViewById(R.id.title)
    }

    val subTitleTv : TextView by lazy {
        this.findViewById(R.id.sub_title)
    }

    val toolBarTitleTv : TextView by lazy {
        this.findViewById(R.id.toolbar_title)
    }

    val toolBarContainer : LinearLayoutCompat by lazy {
        this.findViewById(R.id.title_container)
    }

    val toolbarImageView : CircleImageView by lazy {
        this.findViewById(R.id.image_view_logo)
    }

    val backImageView : ImageView by lazy {
        this.findViewById(R.id.iv_back)
    }

    val firstIconImageView : ImageView by lazy {
        this.findViewById(R.id.first_right_icon)
    }

    val secondIconImageView : ImageView by lazy {
        this.findViewById(R.id.second_right_icon)
    }


    init {
            try {
                View.inflate(getContext(), R.layout.groups_toolbar, this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
    }

    //TODO: Explicitly Handle low end device issue
    fun setImage(url : String) {
        toolbarImageView.visibility = View.VISIBLE
        if(url.isEmpty())
            toolbarImageView.setImageResource(R.drawable.josh_skill_logo)
        else
            Glide.with(toolbarImageView)
                .load(url)
                .into(toolbarImageView)

    }

    fun setGroupSubTitle(subTitle : String, title : String) {
        if(subTitle.isNotBlank()) {
            toolBarTitleTv.visibility = View.GONE
            toolBarContainer.visibility = View.VISIBLE
            titleTv.text = title
            subTitleTv.text = subTitle
        } else {
            toolBarContainer.visibility = View.GONE
            toolBarTitleTv.visibility = View.VISIBLE
            toolBarTitleTv.text = title
        }
    }

    fun firstIcon(drawableRes: Int) {
        firstIconImageView.visibility = View.VISIBLE
        setDrawableImage(drawableRes, firstIconImageView)
    }

    fun secondIcon(drawableRes: Int) {
        secondIconImageView.visibility = View.VISIBLE
        setDrawableImage(drawableRes, secondIconImageView)
    }

    private fun setDrawableImage(drawableRes : Int, imageView : ImageView) {
        imageView.setImageResource(drawableRes)
    }

    // Listeners
    fun onBackPressed(function : () -> Unit) {
        backImageView.setOnClickListener {
            function.invoke()
        }
    }

    fun onFirstIconPressed(function : () -> Unit) {
        firstIconImageView.setOnClickListener {
            function.invoke()
        }
    }

    fun onSecondIconPressed(function : () -> Unit) {
        secondIconImageView.setOnClickListener {
            function.invoke()
        }
    }

}