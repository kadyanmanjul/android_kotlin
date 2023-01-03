package com.joshtalks.joshskills.common.repository.server.feedback

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class FeedbackTypes : Parcelable {
    VIDEO, PRACTISE
}