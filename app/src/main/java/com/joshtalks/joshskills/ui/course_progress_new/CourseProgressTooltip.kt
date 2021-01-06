package com.joshtalks.joshskills.ui.course_progress_new

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey

class CourseProgressTooltip : FrameLayout {

    private var callback: OnDismissClick? = null
    private lateinit var ballonTV: AppCompatTextView

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
        View.inflate(context, R.layout.course_progress_tooltip_layout, this)
        ballonTV = findViewById(R.id.balloon_text)
        ballonTV.text =
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.COURSE_PROGRESS_TOOLTIP_TEXT)

        findViewById<ImageView>(R.id.cross_iv).setOnClickListener { callback?.onCourseProgressTooltipDismiss() }
    }

    fun setText(text: String) {
        ballonTV.text = text
    }

    fun setDismissListener(callback: OnDismissClick) {
        this.callback = callback
    }

    interface OnDismissClick {
        fun onCourseProgressTooltipDismiss()
    }
}