package com.joshtalks.joshskills.ui.explore.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.server.CourseExploreModel

class OfferCourseView : FrameLayout {
    private lateinit var imageView: AppCompatImageView
    private lateinit var buyNowButton: MaterialButton
    private lateinit var cardView: View

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
        cardView = findViewById(R.id.root_view)

    }


    fun bind(courseExploreModel: CourseExploreModel) {
        imageView.setImage(courseExploreModel.imageUrl)
        buyNowButton.visibility = View.VISIBLE
        buyNowButton.text =
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.SHOW_DETAILS_LABEL)

        buyNowButton.setOnClickListener {
            RxBus2.publish(courseExploreModel)
        }
        findViewById<View>(R.id.root_view).setOnClickListener {
            RxBus2.publish(courseExploreModel)
        }

        if (courseExploreModel.isClickable) {
            buyNowButton.visibility = View.VISIBLE
            buyNowButton.text =
                AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.SHOW_DETAILS_LABEL)

            buyNowButton.setOnClickListener {
                RxBus2.publish(courseExploreModel)
            }

            cardView.setOnClickListener {
                RxBus2.publish(courseExploreModel)
            }
        } else {
            buyNowButton.visibility = View.GONE
            cardView.isClickable = false
            cardView.isFocusable = false

            buyNowButton.setOnClickListener {
                showToast(
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.FFCOURSE_CARD_CLICK_MSG)
                )
            }

            buyNowButton.setOnClickListener {
                showToast(
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.FFCOURSE_CARD_CLICK_MSG)
                )
            }

        }

    }

}
