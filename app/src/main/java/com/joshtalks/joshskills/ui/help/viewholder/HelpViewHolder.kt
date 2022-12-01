package com.joshtalks.joshskills.ui.help.viewholder

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.util.Log
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.DrawableTransformation
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.SINGLE_SPACE
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.HelpRequestEventBus
import com.joshtalks.joshskills.repository.server.help.Option
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import jp.wasabeef.glide.transformations.ColorFilterTransformation
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest


@Layout(R.layout.help_view_layout)
class HelpViewHolder(var option: Option, var unreadMessages: Int) {

    @View(R.id.iv_category_icon)
    lateinit var categoryIconIV: AppCompatImageView

    @View(R.id.tv_category_name)
    lateinit var categoryNameTV: AppCompatTextView

    @Resolve
    fun onViewInflated() {
        if (unreadMessages <= 1)
            categoryNameTV.text = option.name
        else categoryNameTV.text = option.name.plus(SINGLE_SPACE).plus("(${unreadMessages} Msg)")

        GlideToVectorYou
            .init()
            .with(AppObjectController.joshApplication)
            .requestBuilder
            .load(option.url)
            .into(categoryIconIV)
    }

    @Click(R.id.root_view)
    fun onClick() {
        RxBus2.publish(HelpRequestEventBus(option))
    }

}



