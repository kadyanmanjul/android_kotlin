package com.joshtalks.joshskills.ui.group.views

import android.content.Context
import android.util.AttributeSet
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

    val toolBarTitleTv : TextView by lazy {
        this.findViewById(R.id.toolbar_title)
    }

    val toolBarContainer : LinearLayoutCompat by lazy {
        this.findViewById(R.id.title_container)
    }

    val toolbarImageView : CircleImageView by lazy {
        this.findViewById(R.id.image_view_logo)
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
        attrs?.let {
            val attrsValue = context.obtainStyledAttributes(it, R.styleable.GroupsAppBar)
            try {
                val title = (attrsValue.getString(R.styleable.GroupsAppBar_title) ?: "Groups")
                val hasSubTitle = attrsValue.getBoolean(R.styleable.GroupsAppBar_has_sub_title, false)
                val imageUrl = attrsValue.getString(R.styleable.GroupsAppBar_img_url)
                val firstIcon = attrsValue.getString(R.styleable.GroupsAppBar_first_right_icon_src)
                val secondIcon = attrsValue.getString(R.styleable.GroupsAppBar_second_right_icon_src)
                if(hasSubTitle) {
                    toolBarTitleTv.visibility = View.GONE
                    toolBarContainer.visibility = View.VISIBLE
                    titleTv.text = title
                } else {
                    toolBarContainer.visibility = View.GONE
                    toolBarTitleTv.visibility = View.VISIBLE
                    toolBarTitleTv.text = title
                }

                when(imageUrl) {
                    null -> toolbarImageView.visibility = View.GONE
                    "" -> setDrawableImage(R.drawable.josh_skill_logo, toolbarImageView)
                    else -> setGroupImage(imageUrl)
                }

                if(firstIcon == null)
                    firstIconImageView.visibility = GONE
                else
                    setDrawableImage(R.drawable.josh_skill_logo, firstIconImageView)

                if(secondIcon == null)
                    secondIconImageView.visibility = GONE
                else
                    setDrawableImage(R.drawable.josh_skill_logo, secondIconImageView)

            } finally {
                attrsValue.recycle()
            }
        }
    }

    //TODO: Explicitly Handle low end device issue
    private fun setGroupImage(url : String) {
        Glide.with(toolbarImageView)
            .load(url)
            .into(toolbarImageView)
    }

    private fun setDrawableImage(drawableRes : Int, imageView : ImageView) {
        imageView.setImageResource(drawableRes)
    }

}