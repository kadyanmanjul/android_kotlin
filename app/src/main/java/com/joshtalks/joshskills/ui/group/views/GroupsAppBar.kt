package com.joshtalks.joshskills.ui.group.views

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

import androidx.constraintlayout.widget.ConstraintLayout

import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setUserImageOrInitialsWithWhiteBackground
import com.joshtalks.joshskills.ui.group.constants.CLOSED_GROUP
import com.joshtalks.joshskills.ui.group.constants.DM_CHAT
import com.joshtalks.joshskills.ui.group.model.DefaultImage

import java.lang.Exception

class GroupsAppBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    val titleTv: TextView by lazy {
        this.findViewById(R.id.title)
    }

    val subTitleTv: TextView by lazy {
        this.findViewById(R.id.sub_title)
    }

    val toolBarTitleTv: TextView by lazy {
        this.findViewById(R.id.toolbar_title)
    }

    val toolBarContainer: ConstraintLayout by lazy {
        this.findViewById(R.id.title_container)
    }

    val toolbarImageView: CircleImageView by lazy {
        this.findViewById(R.id.image_view_logo)
    }

    val backImageView: ImageView by lazy {
        this.findViewById(R.id.iv_back)
    }

    val firstIconImageView: ImageView by lazy {
        this.findViewById(R.id.first_right_icon)
    }

    val secondIconImageView: ImageView by lazy {
        this.findViewById(R.id.second_right_icon)
    }

    val closedGroupIcon: ImageView by lazy {
        this.findViewById(R.id.closed_grp_img)
    }

    init {
        try {
            View.inflate(getContext(), R.layout.groups_toolbar, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //TODO: Explicitly Handle low end device issue
    fun setImage(url: String, groupHeader: String?, groupType: String?) {
        toolbarImageView.visibility = View.VISIBLE
        if (groupType == DM_CHAT && url.isEmpty())
            toolbarImageView.setUserImageOrInitialsWithWhiteBackground(url, groupHeader!!, isRound = true,bgColor = R.color.pure_white,txtColor = R.color.primary_500)
        else if (url.isEmpty())
            toolbarImageView.setImageResource(DefaultImage.DEFAULT_GROUP_IMAGE.drwRes)
        else
            Glide.with(toolbarImageView)
                .load(url)
                .into(toolbarImageView)

    }

    fun setGroupSubTitle(subTitle: String, title: String, groupType: String) {
        if (subTitle.isNotBlank()) {
            toolBarTitleTv.visibility = View.GONE
            toolBarContainer.visibility = View.VISIBLE
            titleTv.text = title
            subTitleTv.text = subTitle
        } else {
            toolBarContainer.visibility = View.INVISIBLE
            toolBarTitleTv.visibility = View.VISIBLE
            toolBarTitleTv.text = title
        }
        when (groupType) {
            CLOSED_GROUP -> closedGroupIcon.visibility = View.VISIBLE
            else -> closedGroupIcon.visibility = View.GONE
        }
    }

    fun firstIcon(drawableRes: Int) {
        when(drawableRes){
            0 -> firstIconImageView.visibility = View.GONE
            else ->{
                firstIconImageView.visibility = View.VISIBLE
                setDrawableImage(drawableRes, firstIconImageView)
            }
        }
    }

    fun secondIcon(drawableRes: Int) {
        when (drawableRes) {
            0 -> secondIconImageView.visibility = View.GONE
            1 ->{
                secondIconImageView.visibility = View.VISIBLE
                setDrawableImage(drawableRes, secondIconImageView)
            }
            else -> {
                secondIconImageView.visibility = View.VISIBLE
                setDrawableImage(drawableRes, secondIconImageView)
            }
        }
    }

    private fun setDrawableImage(drawableRes: Int, imageView: ImageView) {
        imageView.setImageResource(drawableRes)
    }

    // Listeners
    fun onBackPressed(function: () -> Unit) {
        backImageView.setOnClickListener {
            function.invoke()
        }
    }

    fun onToolbarPressed(function: () -> Unit) {
        toolBarContainer.setOnClickListener {
            function.invoke()
        }
    }

    fun onTitlePressed(function: () -> Unit) {
        toolBarTitleTv.setOnClickListener {
            function.invoke()
        }
    }

    fun onFirstIconPressed(function: () -> Unit) {
        firstIconImageView.setOnClickListener {
            function.invoke()
        }
    }

    fun onSecondIconPressed(function: () -> Unit) {
        secondIconImageView.setOnClickListener {
            function.invoke()
        }
    }
}