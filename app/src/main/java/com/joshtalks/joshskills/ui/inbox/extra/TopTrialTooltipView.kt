package com.joshtalks.joshskills.ui.inbox.extra

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey

class TopTrialTooltipView : FrameLayout {

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
        View.inflate(context, R.layout.top_trial_tooltip_view, this)
        ballonTV = findViewById(R.id.balloon_text)
        ballonTV.text =
            String.format(
                AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.BB_TOOL_TIP_BELOW_FIND_COURSE_TEXT),
                7
            )
    }

    fun setText(remainingTrialDays: Int) {
        if(remainingTrialDays in 0..1){
            ballonTV.text =
                AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.BB_TOOL_TIP_BELOW_FIND_COURSE_TEXT_FREE)
        }
        else {
            ballonTV.text =
                String.format(
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.BB_TOOL_TIP_BELOW_FIND_COURSE_TEXT),
                    remainingTrialDays
                )
        }
    }
}
