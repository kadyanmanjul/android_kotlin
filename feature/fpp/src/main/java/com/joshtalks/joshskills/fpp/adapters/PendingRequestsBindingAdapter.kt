package com.joshtalks.joshskills.fpp.adapters

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.setUserImageOrInitials
import com.joshtalks.joshskills.common.ui.fpp.PendingRequestDetail

object PendingRequestsBindingAdapter {
    @BindingAdapter(value = ["recentCallImage"], requireAll = false)
    @JvmStatic
    fun recentCallImage(imageView: ImageView, caller: PendingRequestDetail?) {
        caller?.let {
            imageView.setUserImageOrInitials(it.photoUrl, it.fullName?:"", isRound = true)
        } ?: imageView.setImageResource(R.drawable.ic_call_placeholder)
    }
}