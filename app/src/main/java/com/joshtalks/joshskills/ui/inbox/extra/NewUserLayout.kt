package com.joshtalks.joshskills.ui.inbox.extra

import android.content.Context
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import kotlinx.android.synthetic.main.new_user_layout.view.*

class NewUserLayout : FrameLayout {
    private var callback: Callback? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    init {
        View.inflate(context, R.layout.new_user_layout, this)
        val boolean = AppObjectController.getFirebaseRemoteConfig()
            .getBoolean(FirebaseRemoteConfigKey.SHOW_BB_TOOL_TIP_FIRST_TIME)
        if (boolean) {
            new_user_layout.visibility = View.VISIBLE
            hint_text.text = AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.BB_TOOL_TIP_FIRST_TIME_TEXT)
            val content = SpannableString(
                AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.BB_TOOL_TIP_FIRST_TIME_BTN_TEXT)
            )
            content.setSpan(UnderlineSpan(), 0, content.length, 0)
            text_btn.text = content
            new_user_layout.setOnClickListener {
                callback?.callback(AnalyticsEvent.BLANK_INBOX_SCREEN_CLICKED.NAME)
            }
            text_btn.setOnClickListener {
                callback?.callback(AnalyticsEvent.OK_GOT_IT_CLICKED.NAME)
            }
        }
    }

    fun addCallback(callback: Callback) {
        this.callback = callback
    }

    interface Callback {
        fun callback(name: String)
    }
}
