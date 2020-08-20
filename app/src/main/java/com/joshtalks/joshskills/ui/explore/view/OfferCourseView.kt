package com.joshtalks.joshskills.ui.explore.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.server.CourseExploreModel

class OfferCourseView : FrameLayout {
    private lateinit var imageView: AppCompatImageView
    private lateinit var buyNowButton: MaterialButton

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()

    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()

    }

    private fun init() {
        View.inflate(context, R.layout.course_explorer_header_view, this)
        imageView = findViewById(R.id.image_view)
        buyNowButton = findViewById(R.id.buy_now_button)
    }


    fun bind(courseExploreModel: CourseExploreModel) {
        imageView.setImage(courseExploreModel.imageUrl)
        buyNowButton.visibility = View.VISIBLE
        buyNowButton.text =
            AppObjectController.getFirebaseRemoteConfig().getString("show_details_label")

        buyNowButton.setOnClickListener {
            RxBus2.publish(courseExploreModel)
        }
        findViewById<View>(R.id.root_view).setOnClickListener {
            RxBus2.publish(courseExploreModel)
        }
    }

}